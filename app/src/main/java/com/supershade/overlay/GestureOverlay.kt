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
    // Gesture detection
    // ---------------------------------------------------------------------------

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            /** Must return true so subsequent events in the gesture are delivered. */
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                val startY = e1?.y ?: 0f
                val deltaY = e2.y - startY
                if (velocityY > FLING_VELOCITY_THRESHOLD && deltaY > FLING_DISTANCE_THRESHOLD) {
                    onSwipeDown()
                    return true
                }
                return false
            }
        }
    )

    // ---------------------------------------------------------------------------
    // WindowManager params
    // ---------------------------------------------------------------------------

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        /* height = 1 px */ 1,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        y = getStatusBarHeight()
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /** Adds the overlay view to the WindowManager. Safe to call only once. */
    fun attach() {
        check(overlayView == null) { "GestureOverlay is already attached" }
        val view = View(context).apply {
            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }
        }
        overlayView = view
        windowManager.addView(view, params)
    }

    /** Removes the overlay view.  Swallows all exceptions (system WM may already be gone). */
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
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }

    private companion object {
        /** Minimum downward velocity (px/s) to qualify as a shade-open fling. */
        const val FLING_VELOCITY_THRESHOLD = 500f

        /** Minimum downward travel distance (px) to qualify as a shade-open fling. */
        const val FLING_DISTANCE_THRESHOLD = 50f
    }
}
