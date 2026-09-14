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
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var touchCaptureView: View? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        attachAccessibilityTouchCapture()
    }

    private fun attachAccessibilityTouchCapture() {
        if (touchCaptureView != null || windowManager == null) return

        val statusBarHeightPx = run {
            val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val h = if (resId > 0) resources.getDimensionPixelSize(resId) else 0
            h.coerceAtLeast((40 * resources.displayMetrics.density).toInt())
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

        val view = View(this).apply {
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
                        if (!triggered && deltaY > 35f && deltaY > deltaX * 1.05f) {
                            triggered = true
                            openSuperShade()
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        val duration = System.currentTimeMillis() - startTime
                        if (!triggered && deltaY > 25f && deltaY > deltaX && duration < 600) {
                            triggered = true
                            openSuperShade()
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
            windowManager?.addView(view, params)
        } catch (_: Exception) {}
    }

    private fun detachAccessibilityTouchCapture() {
        touchCaptureView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        touchCaptureView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            val pkg = event.packageName?.toString().orEmpty()
            if (pkg == "com.android.systemui") {
                val cls = event.className?.toString().orEmpty()
                val isShadeTrigger = cls.contains("Notification", ignoreCase = true) ||
                    cls.contains("Shade", ignoreCase = true) ||
                    cls.contains("Panel", ignoreCase = true) ||
                    cls.contains("StatusBar", ignoreCase = true) ||
                    cls.contains("CentralSurfaces", ignoreCase = true) ||
                    eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED

                if (isShadeTrigger && !shadeViewModel.state.value.isOpen) {
                    dismissSystemShade()
                    openSuperShade()
                }
            }
        }
    }

    private fun openSuperShade() {
        dismissSystemShade()
        scope.launch { governor.collapse() }

        val intent = Intent(this, ShadeService::class.java).apply {
            action = ShadeService.ACTION_OPEN_SHADE
        }
        try {
            startForegroundService(intent)
        } catch (_: Exception) {
            try { startService(intent) } catch (_: Exception) {}
        }
        shadeViewModel.open()
    }

    fun dismissSystemShade(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        detachAccessibilityTouchCapture()
        scope.cancel()
        instance = null
    }
}
