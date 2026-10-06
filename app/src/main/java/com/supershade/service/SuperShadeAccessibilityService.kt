package com.supershade.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Privileged accessibility service that intercepts the system status bar / notification
 * panel and seamlessly presents SuperShade.
 *
 * It combines:
 * 1. A top [WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY] touch capture strip.
 *    Because TYPE_ACCESSIBILITY_OVERLAY sits at window layer ~33 (above TYPE_STATUS_BAR
 *    layer ~28), swipes down on the status bar are captured before SystemUI sees them.
 * 2. Window state monitoring to immediately dismiss any system shade expansions that
 *    originate from launcher swipe-down gestures, home-screen widgets, or hardware keys.
 */
class SuperShadeAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: SuperShadeAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    private val governor: StatusBarGovernor by inject()
    private val shadeViewModel: ShadeViewModel by inject()
    private val settings: ShadeSettings by inject()
    private val shadeWindowManager: com.supershade.overlay.ShadeWindowManager by inject()
    private val headsUpOverlay: HeadsUpOverlay by inject()
    private val notificationRepo: com.supershade.domain.notification.NotificationRepository by inject()
    private val brightnessRepo: com.supershade.domain.brightness.BrightnessRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var touchCaptureView: View? = null
    @Volatile private var isSuperShadeActive = false
    @Volatile private var currentSplitGestureMode = com.supershade.settings.SplitGestureMode.SEPARATE_70_30
    /** Epoch-ms of the last time SuperShade was opened via TYPE_WINDOWS_CHANGED; used to debounce false triggers. */
    @Volatile private var lastShadeOpenTimeMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        android.util.Log.i("SuperShadeA11y", "onServiceConnected: initializing settings observer")

        // Observe isOpen state from ViewModel so opening shade from gesture or system intercept
        // immediately presents the overlay window.
        shadeViewModel.state
            .map { it.isOpen }
            .distinctUntilChanged()
            .onEach { isOpen ->
                if (isOpen) shadeWindowManager.show() else shadeWindowManager.hide()
            }
            .launchIn(scope)

        // Show peek cards for incoming notifications when shade is closed.
        notificationRepo.newNotifications
            .onEach { notification ->
                if (!shadeViewModel.state.value.isOpen) {
                    headsUpOverlay.show(notification)
                }
            }
            .launchIn(scope)

        combine(settings.isActive, settings.blockSystemShade) { active, block ->
            active to block
        }
            .distinctUntilChanged()
            .onEach { (active, block) ->
                isSuperShadeActive = active
                if (active) {
                    attachAccessibilityTouchCapture()
                    if (block) {
                        governor.disableExpansion()
                    } else {
                        governor.enableExpansion()
                    }
                } else {
                    detachAccessibilityTouchCapture()
                    governor.restoreSystemStatusBar()
                }
            }
            .launchIn(scope)

        settings.splitGestureMode
            .distinctUntilChanged()
            .onEach { mode ->
                currentSplitGestureMode = mode
            }
            .launchIn(scope)
    }


    private fun attachAccessibilityTouchCapture() {
        if (touchCaptureView != null) return

        val wm = getSystemService(WindowManager::class.java) ?: return
        windowManager = wm

        val statusBarHeightPx = run {
            val extra = (20 * resources.displayMetrics.density).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val insets = wm.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                        android.view.WindowInsets.Type.statusBars()
                    )
                    val top = insets?.top ?: 0
                    if (top > 0) return@run top + extra
                } catch (_: Throwable) {}
            }
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resId > 0) resources.getDimensionPixelSize(resId) else 0
            h.coerceAtLeast((28 * resources.displayMetrics.density).toInt()) + extra
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val display = display
                    val maxRate = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 120f
                    preferredRefreshRate = maxRate.coerceAtLeast(60f)
                } catch (_: Throwable) {
                    preferredRefreshRate = 120f
                }
            }
        }

        var startX = 0f
        var startY = 0f
        var triggered = false
        var isBrightnessScrubbing = false
        var lastBrightnessStep = -1
        var velocityTracker: VelocityTracker? = null
        val density = resources.displayMetrics.density
        // Responsive pull threshold: 14dp downward motion
        val dragThreshold = (14f * density).coerceAtLeast(20f)

        val view = View(this).apply {
            setOnTouchListener { v, event ->
                if (shadeViewModel.state.value.isOpen) return@setOnTouchListener false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        triggered = false
                        isBrightnessScrubbing = false
                        lastBrightnessStep = -1
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain().apply {
                            addMovement(event)
                        }
                        val edgeExclusionPx = 10f * density
                        val screenWidth = resources.displayMetrics.widthPixels
                        if (startX < edgeExclusionPx || startX > (screenWidth - edgeExclusionPx)) {
                            return@setOnTouchListener false
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        velocityTracker?.addMovement(event)
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        velocityTracker?.computeCurrentVelocity(1000)
                        val yVelocity = velocityTracker?.yVelocity ?: 0f

                        // LineageOS & Good Lock QuickStar horizontal status bar scrub for brightness
                        val isHorizontalScrub = deltaX > (22f * density) && deltaX > (kotlin.math.abs(deltaY) * 1.75f)
                        if (!triggered && (isBrightnessScrubbing || isHorizontalScrub)) {
                            isBrightnessScrubbing = true
                            val screenWidth = resources.displayMetrics.widthPixels.coerceAtLeast(1)
                            val brightnessRatio = (event.rawX / screenWidth.toFloat()).coerceIn(0.04f, 1.0f)
                            val brightnessLevel = (brightnessRatio * 255).toInt()
                            val step = brightnessLevel / 20
                            if (step != lastBrightnessStep) {
                                lastBrightnessStep = step
                                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            }
                            scope.launch(Dispatchers.IO) {
                                brightnessRepo.set(brightnessLevel)
                            }
                            return@setOnTouchListener true
                        }

                        val isFlingDown = !isBrightnessScrubbing && yVelocity > (750f * density) && deltaY > (8f * density) && deltaY > deltaX
                        val isStandardPull = !isBrightnessScrubbing && deltaY > dragThreshold && deltaY > deltaX * 1.05f

                        // Fluid pull trigger: downward fling or standard downward gesture (> 46 degrees)
                        if (!triggered && (isFlingDown || isStandardPull)) {
                            triggered = true
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            val screenWidth = resources.displayMetrics.widthPixels.coerceAtLeast(1)
                            val ratio = startX / screenWidth.toFloat()
                            val mode = currentSplitGestureMode

                            val isCutoutDeadband = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val cutout = v.rootWindowInsets?.displayCutout
                                val topRect = cutout?.boundingRectTop
                                if (topRect != null && !topRect.isEmpty) {
                                    startX >= (topRect.left - 12 * density) && startX <= (topRect.right + 12 * density)
                                } else false
                            } else false

                            val expandQs = when {
                                mode == com.supershade.settings.SplitGestureMode.ALWAYS_NOTIFICATIONS -> false
                                mode == com.supershade.settings.SplitGestureMode.ALWAYS_QUICK_SETTINGS -> true
                                mode.isTogether -> false
                                isCutoutDeadband -> false
                                mode == com.supershade.settings.SplitGestureMode.SEPARATE_30_70 -> ratio < 0.30f
                                mode == com.supershade.settings.SplitGestureMode.SEPARATE_50_50 -> ratio > 0.50f
                                mode == com.supershade.settings.SplitGestureMode.SEPARATE_70_30 -> ratio > 0.70f
                                else -> false
                            }
                            openSuperShade(expandQs)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        triggered = false
                        isBrightnessScrubbing = false
                        lastBrightnessStep = -1
                        velocityTracker?.recycle()
                        velocityTracker = null
                        true
                    }
                    else -> false
                }
            }
        }

        touchCaptureView = view
        try {
            wm.addView(view, params)
            android.util.Log.i("SuperShadeA11y", "TYPE_ACCESSIBILITY_OVERLAY touch window added successfully at height $statusBarHeightPx px")
        } catch (e: Exception) {
            android.util.Log.e("SuperShadeA11y", "Failed to add TYPE_ACCESSIBILITY_OVERLAY window", e)
        }
    }

    private fun detachAccessibilityTouchCapture() {
        touchCaptureView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager?.removeViewImmediate(view)
                }
            } catch (t: Throwable) {
                android.util.Log.w("SuperShadeA11y", "Error removing touch window", t)
            }
        }
        touchCaptureView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isSuperShadeActive || event == null) return

        when (event.eventType) {
            // Real system shade expansion updates the window list.
            // Guard: require isActive + layer >= 3 to avoid false positives from transient UI.
            // Debounce: skip re-triggers within 2 s of a previous open.
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (shadeViewModel.state.value.isOpen) return
                // Debounce: don't re-trigger if we just opened within 2 s
                val now = System.currentTimeMillis()
                if (now - lastShadeOpenTimeMs < 2000L) return
                try {
                    val windowList = try { windows } catch (t: Throwable) { null } ?: emptyList()
                    val isSystemShadeVisible = windowList.any { w ->
                        try {
                            w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM &&
                                w.title?.toString() == "NotificationShade" &&
                                w.layer >= 2
                        } catch (t: Throwable) { false }
                    }
                    if (isSystemShadeVisible) {
                        lastShadeOpenTimeMs = now
                        android.util.Log.d("SuperShadeA11y", "Intercepted native NotificationShade window expansion")
                        scope.launch { governor.runShell("cmd", "statusbar", "collapse") }
                        openSuperShade(expandQs = false)
                    }
                } catch (t: Throwable) {}
            }

            // Strictly filter TYPE_WINDOW_STATE_CHANGED to genuine shade controllers
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString().orEmpty()
                val cls = event.className?.toString().orEmpty()

                if (pkg != "com.android.systemui") return

                // Explicitly ignore volume panels, heads up, edge panels, keyguard, clipboard, toasts
                if (cls.contains("Volume", ignoreCase = true) ||
                    cls.contains("HeadsUp", ignoreCase = true) ||
                    cls.contains("Keyguard", ignoreCase = true) ||
                    cls.contains("Edge", ignoreCase = true) ||
                    cls.contains("Clipboard", ignoreCase = true) ||
                    cls.contains("NavigationBar", ignoreCase = true) ||
                    cls.contains("Biometric", ignoreCase = true) ||
                    cls.contains("Toast", ignoreCase = true)
                ) {
                    return
                }

                val isExactShadeController = cls == "com.android.systemui.shade.NotificationShadeWindowView" ||
                    cls == "com.android.systemui.shade.SecNotificationShadeWindowView" ||
                    cls == "com.android.systemui.shade.NotificationPanelViewController" ||
                    cls == "com.android.systemui.shade.SecNotificationPanelViewController"

                if (isExactShadeController && !shadeViewModel.state.value.isOpen) {
                    android.util.Log.d("SuperShadeA11y", "Intercepted exact shade controller: $cls")
                    openSuperShade(expandQs = false)
                }
            }

            // Fallback for Good Lock One Hand Operation+ (com.samsung.android.sidegesturepad).
            // OHO+ may dispatch a view-click or gesture-detection event in the sidegesturepad
            // process before the window list changes, giving us an earlier intercept point.
            AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> {
                val pkg = event.packageName?.toString().orEmpty()
                if (pkg == "com.samsung.android.sidegesturepad" && !shadeViewModel.state.value.isOpen) {
                    android.util.Log.d("SuperShadeA11y", "OHO+ gesture/click event intercepted from $pkg")
                    openSuperShade(expandQs = false)
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!isSuperShadeActive || event == null) return super.onKeyEvent(event)
        val keyCode = event.keyCode

        // Close SuperShade when Home or App Switch is pressed while shade is open
        if (shadeViewModel.state.value.isOpen) {
            if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
                if (event.action == KeyEvent.ACTION_UP) {
                    android.util.Log.d("SuperShadeA11y", "Home/Recents button tapped while shade open -> closing SuperShade")
                    shadeViewModel.close()
                }
                return true
            }
        }

        // Samsung Good Lock One Hand Operation+ and Knox inject keycodes:
        // - 1003: Samsung One UI SEM_KEYCODE_EXPAND_NOTI_PANEL
        // - 1004: Samsung One UI SEM_KEYCODE_EXPAND_QUICK_PANEL
        // - 83:   KeyEvent.KEYCODE_NOTIFICATION (Stock AOSP notification panel key)
        // - 217:  KEYCODE_MEDIA_CLOSE — used by some OHO+ variants on One UI 8
        // - 0 w/ scanCode 766: Samsung custom scan code for notification panel (some OHO+ builds)
        if (keyCode == 1003 || keyCode == 83 || keyCode == 217 ||
            (keyCode == 0 && event.scanCode == 766)) {
            if (event.action == KeyEvent.ACTION_UP) {
                android.util.Log.d("SuperShadeA11y", "Intercepted notification panel keyevent (keyCode=$keyCode scanCode=${event.scanCode}) from Good Lock / System")
                openSuperShade(expandQs = false)
            }
            return true
        } else if (keyCode == 1004) {
            if (event.action == KeyEvent.ACTION_UP) {
                android.util.Log.d("SuperShadeA11y", "Intercepted quick panel keyevent ($keyCode) from Good Lock / System")
                openSuperShade(expandQs = true)
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    @Suppress("DEPRECATION")
    override fun onGesture(gestureId: Int): Boolean {
        if (shadeViewModel.state.value.isOpen) {
            when (gestureId) {
                GESTURE_SWIPE_UP,
                GESTURE_SWIPE_UP_AND_LEFT,
                GESTURE_SWIPE_UP_AND_RIGHT -> {
                    android.util.Log.d("SuperShadeA11y", "Accessibility swipe-up detected while shade open -> closing SuperShade")
                    shadeViewModel.close()
                    return true
                }
            }
        }
        return super.onGesture(gestureId)
    }

    private fun openSuperShade(expandQs: Boolean = false) {
        // Collapse native system panel via Shizuku without injecting synthetic BACK keycodes
        scope.launch {
            governor.collapse()
        }

        val intent = Intent(this, ShadeService::class.java).apply {
            action = ShadeService.ACTION_OPEN_SHADE
            putExtra(ShadeService.EXTRA_EXPAND_QS, expandQs)
        }
        try {
            startForegroundService(intent)
        } catch (_: Exception) {
            try { startService(intent) } catch (_: Exception) {}
        }
        shadeViewModel.open(expandQs)
        shadeWindowManager.show(expandQs)
    }

    fun dismissSystemShade(): Boolean {
        // Never trigger system dismiss actions if SuperShade is already open or showing,
        // as Android global actions can route KEYCODE_BACK directly into our overlay window.
        if (shadeViewModel.state.value.isOpen || shadeWindowManager.isShowing()) {
            scope.launch { governor.collapse() }
            return true
        }
        val result = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
        scope.launch { governor.collapse() }
        return result
    }

    /**
     * Called by [ShadeOpenReceiver] when an external broadcast (Good Lock,
     * Tasker, Bixby, ADB, etc.) requests SuperShade to open.
     */
    fun openSuperShadeFromReceiver(expandQs: Boolean = false) {
        if (!isSuperShadeActive) return
        if (shadeViewModel.state.value.isOpen) return
        openSuperShade(expandQs)
    }

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (isSuperShadeActive) {
            detachAccessibilityTouchCapture()
            attachAccessibilityTouchCapture()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detachAccessibilityTouchCapture()
        shadeWindowManager.hide()
        governor.restoreSystemStatusBarBlocking()
        scope.cancel()
        instance = null
    }
}

