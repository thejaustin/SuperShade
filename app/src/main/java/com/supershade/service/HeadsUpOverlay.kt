package com.supershade.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.supershade.domain.notification.model.ShadeNotification

/**
 * Manages heads-up (peek) notification overlays that float above running apps.
 *
 * Each call to [show] replaces any currently-displayed peek card with a new one
 * and schedules an auto-dismiss after [autoDismissMs] milliseconds.  The overlay
 * is removed immediately on [destroy], which must be called when the owning
 * service is stopping.
 *
 * The peek card is rendered by a [ComposeView] with a self-contained
 * [LifecycleOwner] so that Compose state restoration APIs work correctly inside
 * the non-Activity window.
 */
class HeadsUpOverlay(private val context: Context) {

    private val wm: WindowManager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var currentView: ComposeView? = null
    private var currentLifecycleOwner: HeadsUpLifecycleOwner? = null

    // ---------------------------------------------------------------------------
    // WindowManager params — full-width, wrap-height strip at the top of screen
    // ---------------------------------------------------------------------------

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    // ---------------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------------

    /**
     * Displays a peek card for [notification].  Any previously-displayed card
     * is dismissed first.  The card is automatically removed after [autoDismissMs].
     *
     * Must be called from the main thread (or it will be posted there).
     */
    fun show(notification: ShadeNotification, autoDismissMs: Long = 4_000L) {
        handler.post {
            dismissCurrent()
            val owner = HeadsUpLifecycleOwner()
            currentLifecycleOwner = owner

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    HeadsUpCard(notification = notification)
                }
            }
            currentView = view
            try {
                wm.addView(view, params)
            } catch (_: Exception) {
                // Window token invalid — service may be stopping.
                owner.destroy()
                currentView = null
                currentLifecycleOwner = null
                return@post
            }

            // Schedule automatic dismissal.
            handler.postDelayed({ dismissCurrent() }, autoDismissMs)
        }
    }

    /**
     * Removes any currently-displayed peek card and cancels pending auto-dismiss
     * callbacks.  Must be called when the owning service is stopping.
     */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        handler.post { dismissCurrent() }
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun dismissCurrent() {
        currentView?.let { view ->
            try {
                wm.removeView(view)
            } catch (_: Exception) {}
            currentView = null
        }
        currentLifecycleOwner?.destroy()
        currentLifecycleOwner = null
    }

    // ---------------------------------------------------------------------------
    // Peek card composable
    // ---------------------------------------------------------------------------

    /**
     * A simple card that shows the notification title and body text.
     *
     * In a complete implementation this would animate in from the top, support
     * swipe-to-dismiss, and mirror the full SuperShade design language.  The
     * [ShadeRoot] composable (owned by another agent) defines the full design
     * system; this card intentionally uses only raw Material 3 primitives so it
     * compiles independently of that layer.
     */
    @androidx.compose.runtime.Composable
    private fun HeadsUpCard(notification: ShadeNotification) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(
                    color = Color(0xFF1C1C1E).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
            }
            if (notification.text.isNotBlank()) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Lifecycle owner for non-Activity ComposeViews
    // ---------------------------------------------------------------------------

    private class HeadsUpLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        private val vmStore = ViewModelStore()

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry =
            savedStateController.savedStateRegistry
        override val viewModelStore: ViewModelStore = vmStore

        init {
            savedStateController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }

        fun destroy() {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            vmStore.clear()
        }
    }
}
