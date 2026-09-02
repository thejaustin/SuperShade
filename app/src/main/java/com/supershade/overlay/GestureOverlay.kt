package com.supershade.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * A 1 px-tall invisible overlay window positioned immediately below the status
 * bar.  It owns a [GestureDetector] that translates a downward fling into a
 * call to [onSwipeDown], which in turn tells [ShadeWindowManager] to present
 * the full shade UI.
 *
 * Call [attach] after constructing, and [detach] when the service is destroyed.
 */
class GestureOverlay(
    private val context: Context,
    private val onSwipeDown: () -> Unit,
) {

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    // ---------------------------------------------------------------------------
    // WindowManager params
    // ---------------------------------------------------------------------------

    private val statusBarHeight = getStatusBarHeight()
    private val captureHeight = maxOf(statusBarHeight, (48 * context.resources.displayMetrics.density).toInt())

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
        x = 0
        y = 0
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /** Adds the overlay view to the WindowManager. Safe to call only once. */
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

    /** Removes the overlay view. Swallows all exceptions (system WM may already be gone). */
    fun detach() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // WindowManager.BadTokenException or IllegalArgumentException — view is gone.
            }
        }
        overlayView = null
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun getStatusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else (24 * context.resources.displayMetrics.density).toInt()
    }
}
