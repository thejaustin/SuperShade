package com.supershade.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var touchCaptureView: View? = null
    @Volatile private var isSuperShadeActive = false
    @Volatile private var currentSplitGestureMode = com.supershade.settings.SplitGestureMode.SEPARATE_70_30

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

        settings.isActive
            .distinctUntilChanged()
            .onEach { active ->
                isSuperShadeActive = active
                if (active) {
                    attachAccessibilityTouchCapture()
                    if (settings.blockSystemShade.first()) {
                        governor.disableExpansion()
                    }
                } else {
                    detachAccessibilityTouchCapture()
                    governor.enableExpansion()
                }
            }
            .launchIn(scope)

        settings.blockSystemShade
            .distinctUntilChanged()
            .onEach { block ->
                if (isSuperShadeActive) {
                    if (block) governor.disableExpansion() else governor.enableExpansion()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insets = wm.currentWindowMetrics.windowInsets.getInsetsIgnoringVisibility(
                    android.view.WindowInsets.Type.statusBars()
                )
                val top = insets?.top ?: 0
                if (top > 0) return@run top
            }
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resId > 0) resources.getDimensionPixelSize(resId) else 0
            h.coerceAtLeast((28 * resources.displayMetrics.density).toInt())
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            statusBarHeightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        var startX = 0f
        var startY = 0f
        var triggered = false
        val density = resources.displayMetrics.density
        // Robust threshold: at least 28dp (never hair-trigger 10-16dp)
        val dragThreshold = (28f * density).coerceAtLeast(40f)

        val view = View(this).apply {
            setOnTouchListener { v, event ->
                if (shadeViewModel.state.value.isOpen) return@setOnTouchListener false
                val wmCurrent = windowManager ?: return@setOnTouchListener false

                // Do not intercept touches when status bar is explicitly hidden in immersive mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val rootInsets = v.rootWindowInsets
                    if (rootInsets != null && !rootInsets.isVisible(android.view.WindowInsets.Type.statusBars())) {
                        return@setOnTouchListener false
                    }
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        triggered = false
                        val edgeExclusionPx = 18f * density
                        val screenWidth = resources.displayMetrics.widthPixels
                        if (startX < edgeExclusionPx || startX > (screenWidth - edgeExclusionPx)) {
                            return@setOnTouchListener false
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        // Require deliberate downward pull (at least 28dp, predominantly vertical > 52 degrees)
                        if (!triggered && deltaY > dragThreshold && deltaY > deltaX * 1.30f) {
                            triggered = true
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            val screenWidth = resources.displayMetrics.widthPixels.coerceAtLeast(1)
                            val ratio = startX / screenWidth.toFloat()
                            val mode = currentSplitGestureMode

                            val isCutoutDeadband = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val insets = wmCurrent.currentWindowMetrics.windowInsets
                                val cutout = insets?.displayCutout
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
                        // Never trigger on ACTION_UP. If the drag didn't reach 28dp during
                        // ACTION_MOVE, lifting the finger is a tap, NOT a shade pull gesture!
                        triggered = false
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
                windowManager?.removeView(view)
            } catch (e: Exception) {
                android.util.Log.w("SuperShadeA11y", "Error removing touch window", e)
            }
        }
        touchCaptureView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isSuperShadeActive || event == null) return

        when (event.eventType) {
            // Real system shade expansion updates the window list
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                if (shadeViewModel.state.value.isOpen) return
                try {
                    val currentWindows = windows
                    val isSystemShadeVisible = currentWindows.any { w ->
                        w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM &&
                            w.title?.toString() == "NotificationShade" &&
                            (w.isActive || w.layer > 0)
                    }
                    if (isSystemShadeVisible) {
                        android.util.Log.d("SuperShadeA11y", "Intercepted native NotificationShade window expansion")
                        openSuperShade(expandQs = false)
                    }
                } catch (_: Exception) {}
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
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (!isSuperShadeActive || event == null) return super.onKeyEvent(event)
        val keyCode = event.keyCode

        // Samsung Good Lock One Hand Operation+ and Knox inject keycodes:
        // - 1003: Samsung One UI SEM_KEYCODE_EXPAND_NOTI_PANEL
        // - 1004: Samsung One UI SEM_KEYCODE_EXPAND_QUICK_PANEL
        // - 83:   KeyEvent.KEYCODE_NOTIFICATION (Stock AOSP notification panel key)
        if (keyCode == 1003 || keyCode == 83) {
            if (event.action == KeyEvent.ACTION_UP) {
                android.util.Log.d("SuperShadeA11y", "Intercepted notification panel keyevent ($keyCode) from Good Lock / System")
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

    private fun openSuperShade(expandQs: Boolean = false) {
        // Safely collapse system panel without injecting global BACK keycodes that kill SuperShade
        scope.launch { governor.collapse() }

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
        governor.enableExpansionBlocking()
        scope.cancel()
        instance = null
    }
}

