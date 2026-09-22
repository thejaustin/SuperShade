package com.supershade.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

import com.supershade.settings.SplitGestureMode

/**
 * An invisible touch-capture strip positioned just below the status bar.
 *
 * Removing FLAG_LAYOUT_IN_SCREEN places this window in stable-bounds coordinates
 * (y=0 = bottom of the system status bar). This avoids competing with the
 * TYPE_STATUS_BAR window for touch events — the system shade handles swipes that
 * start inside the status bar itself, while SuperShade handles swipes that start
 * in the content area just below it. Without Shizuku this coexistence is the
 * correct behaviour: both shades are reachable from slightly different drag origins.
 */
class GestureOverlay(
    private val context: Context,
    private val isShadeOpen: () -> Boolean = { false },
    private val splitGestureMode: () -> SplitGestureMode = { SplitGestureMode.SEPARATE_70_30 },
    private val onSwipeDown: (expandQs: Boolean) -> Unit,
) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    // Top bezel strip: matches exact status bar height, avoiding overlap with top app bars
    private val captureHeight = run {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val insets = windowManager?.currentWindowMetrics?.windowInsets?.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.statusBars()
            )
            val top = insets?.top ?: 0
            if (top > 0) return@run top
        }
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val h = if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
        h.coerceAtLeast((28 * context.resources.displayMetrics.density).toInt())
    }

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        captureHeight,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    fun attach() {
        if (overlayView != null) return
        var startX = 0f
        var startY = 0f
        var triggered = false
        val density = context.resources.displayMetrics.density
        val dragThreshold = (28f * density).coerceAtLeast(40f)

        val view = View(context).apply {
            setOnTouchListener { v, event ->
                if (isShadeOpen()) return@setOnTouchListener false
                val wmCurrent = windowManager ?: return@setOnTouchListener false

                // Do not intercept touches when status bar is hidden (immersive full-screen games/videos)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val insets = wmCurrent.currentWindowMetrics.windowInsets
                    if (!insets.isVisible(android.view.WindowInsets.Type.statusBars())) {
                        return@setOnTouchListener false
                    }
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (com.supershade.service.SuperShadeAccessibilityService.isRunning()) {
                            return@setOnTouchListener false
                        }
                        startX = event.rawX
                        startY = event.rawY
                        triggered = false
                        val edgeExclusionPx = 18f * density
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        if (startX < edgeExclusionPx || startX > (screenWidth - edgeExclusionPx)) {
                            return@setOnTouchListener false
                        }
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        if (!triggered && deltaY > dragThreshold && deltaY > deltaX * 1.30f) {
                            triggered = true
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            val screenWidth = context.resources.displayMetrics.widthPixels.coerceAtLeast(1)
                            val ratio = startX / screenWidth.toFloat()
                            val mode = splitGestureMode()

                            val isCutoutDeadband = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                val insets = wmCurrent.currentWindowMetrics.windowInsets
                                val cutout = insets?.displayCutout
                                val topRect = cutout?.boundingRectTop
                                if (topRect != null && !topRect.isEmpty) {
                                    val d = context.resources.displayMetrics.density
                                    startX >= (topRect.left - 12 * d) && startX <= (topRect.right + 12 * d)
                                } else false
                            } else false

                            val expandQs = when {
                                mode == SplitGestureMode.ALWAYS_NOTIFICATIONS -> false
                                mode == SplitGestureMode.ALWAYS_QUICK_SETTINGS -> true
                                mode.isTogether -> false
                                isCutoutDeadband -> false
                                mode == SplitGestureMode.SEPARATE_30_70 -> ratio < 0.30f
                                mode == SplitGestureMode.SEPARATE_50_50 -> ratio > 0.50f
                                mode == SplitGestureMode.SEPARATE_70_30 -> ratio > 0.70f
                                else -> false
                            }
                            onSwipeDown(expandQs)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        triggered = false
                        true
                    }
                    else -> false
                }
            }
        }
        overlayView = view
        try {
            windowManager.addView(view, params)
        } catch (_: Exception) {}
    }

    fun detach() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
    }
}
