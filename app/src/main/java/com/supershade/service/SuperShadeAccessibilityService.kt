package com.supershade.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var touchCaptureView: View? = null
    @Volatile private var isSuperShadeActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        android.util.Log.i("SuperShadeA11y", "onServiceConnected: initializing settings observer")

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
    }


    private fun attachAccessibilityTouchCapture() {
        if (touchCaptureView != null) return

        val contextForWindow = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val disp = display ?: (getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager)
                ?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            if (disp != null) {
                createWindowContext(disp, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null)
            } else {
                this
            }
        } else {
            this
        }
        val wm = contextForWindow.getSystemService(WindowManager::class.java) ?: return
        windowManager = wm

        val statusBarHeightPx = run {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resId > 0) resources.getDimensionPixelSize(resId) else 0
            h.coerceAtLeast((48 * resources.displayMetrics.density).toInt())
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
        var startTime = 0L
        var triggered = false

        val view = View(contextForWindow).apply {
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        startY = event.rawY
                        startTime = System.currentTimeMillis()
                        triggered = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        if (!triggered && deltaY > 25f && deltaY > deltaX * 1.05f) {
                            triggered = true
                            val screenWidth = resources.displayMetrics.widthPixels
                            val expandQs = startX > screenWidth * 0.72f
                            openSuperShade(expandQs)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        val duration = System.currentTimeMillis() - startTime
                        if (!triggered && deltaY > 15f && deltaY > deltaX && duration < 600) {
                            triggered = true
                            val screenWidth = resources.displayMetrics.widthPixels
                            val expandQs = startX > screenWidth * 0.72f
                            openSuperShade(expandQs)
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
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
        if (!isSuperShadeActive) return
        if (event == null) return
        val eventType = event.eventType

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString().orEmpty()
            val cls = event.className?.toString().orEmpty()

            val isSystemUi = pkg == "com.android.systemui" || pkg.isEmpty()
            val isShadeClass = cls.contains("Notification", ignoreCase = true) ||
                cls.contains("Shade", ignoreCase = true) ||
                cls.contains("Panel", ignoreCase = true) ||
                cls.contains("StatusBar", ignoreCase = true) ||
                cls.contains("CentralSurfaces", ignoreCase = true) ||
                cls.contains("SecPanel", ignoreCase = true) ||
                cls.contains("SecQuick", ignoreCase = true)

            if (isSystemUi && isShadeClass) {
                android.util.Log.d("SuperShadeA11y", "System shade expansion intercepted: pkg=$pkg cls=$cls")
                if (!shadeViewModel.state.value.isOpen) {
                    openSuperShade()
                }
            }
        } else if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            try {
                val hasActiveSystemShade = windows.any { w ->
                    val title = w.title?.toString().orEmpty()
                    w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM &&
                        (title.contains("NotificationShade", ignoreCase = true) ||
                         title.contains("StatusBar", ignoreCase = true) ||
                         title.contains("Panel", ignoreCase = true))
                }
                if (hasActiveSystemShade && !shadeViewModel.state.value.isOpen) {
                    android.util.Log.d("SuperShadeA11y", "System shade window active, suppressing and showing SuperShade")
                    dismissSystemShade()
                    openSuperShade()
                }
            } catch (_: Exception) {}
        }
    }

    private fun openSuperShade(expandQs: Boolean = false) {
        dismissSystemShade()
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
    }

    fun dismissSystemShade(): Boolean {
        val result = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } catch (_: Exception) {
            false
        }
        scope.launch { governor.collapse() }
        return result
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        detachAccessibilityTouchCapture()
        governor.enableExpansionBlocking()
        scope.cancel()
        instance = null
    }
}

