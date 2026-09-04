package com.supershade.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

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
    private val onSwipeDown: () -> Unit,
) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    // 56 dp strip just below the status bar — wide enough to catch a quick downward flick.
    private val captureHeight = (56 * context.resources.displayMetrics.density).toInt()

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        captureHeight,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    fun attach() {
        if (overlayView != null) return
        var startX = 0f
        var startY = 0f
        var startTime = 0L
        var triggered = false

        val view = View(context).apply {
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
                        if (!triggered && deltaY > 40f && deltaY > deltaX * 1.1f) {
                            triggered = true
                            onSwipeDown()
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val deltaX = kotlin.math.abs(event.rawX - startX)
                        val deltaY = event.rawY - startY
                        val duration = System.currentTimeMillis() - startTime
                        if (!triggered && deltaY > 30f && deltaY > deltaX && duration < 600) {
                            triggered = true
                            onSwipeDown()
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
