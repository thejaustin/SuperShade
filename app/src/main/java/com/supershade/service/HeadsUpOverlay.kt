package com.supershade.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Bug 6: unique token per show() invocation to prevent stale postDelayed from
    // dismissing a card that was shown after the one that scheduled it.
    private var currentDismissToken: Any? = null

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
                    HeadsUpCard(
                        notification = notification,
                        onTap = {
                            try { notification.contentIntent?.send() } catch (_: Exception) {}
                            handler.post { dismissCurrent() }
                        },
                    )
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

            // Bug 6: mint a new token and only dismiss if it's still the current one,
            // preventing the first card's delayed callback from killing the second card.
            val token = Any()
            currentDismissToken = token
            handler.postDelayed({
                if (currentDismissToken === token) dismissCurrent()
            }, autoDismissMs)
        }
    }

    /**
     * Removes any currently-displayed peek card and cancels pending auto-dismiss
     * callbacks.  Must be called when the owning service is stopping.
     */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        // Bug 5: call synchronously — destroy() is always called on the main thread,
        // and a posted message may never run if the process is killed immediately.
        dismissCurrent()
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
    private fun HeadsUpCard(notification: ShadeNotification, onTap: () -> Unit) {
        val ctx = LocalContext.current
        val appIcon by produceState<ImageBitmap?>(null, notification.packageName) {
            value = withContext(Dispatchers.IO) {
                try {
                    ctx.packageManager.getApplicationIcon(notification.packageName)
                        .toBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                        .asImageBitmap()
                } catch (_: Exception) { null }
            }
        }
        val largeIcon by produceState<ImageBitmap?>(null, notification.largeIcon) {
            value = withContext(Dispatchers.IO) {
                try {
                    notification.largeIcon?.loadDrawable(ctx)
                        ?.toBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                        ?.asImageBitmap()
                } catch (_: Exception) { null }
            }
        }
        // For the header: prefer sender avatar (largeIcon) over tiny app icon.
        val headerIcon = largeIcon ?: appIcon
        val headerIconSize = if (largeIcon != null) 28.dp else 14.dp
        val headerIconCorner = if (largeIcon != null) 50 else 3
        val appName = remember(notification.packageName) {
            try {
                val info = ctx.packageManager.getApplicationInfo(notification.packageName, 0)
                ctx.packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                notification.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
        val timeLabel = remember(notification.postTime) {
            val delta = System.currentTimeMillis() - notification.postTime
            when {
                delta < 60_000L -> "now"
                delta < 3_600_000L -> "${delta / 60_000}m ago"
                else -> "${delta / 3_600_000}h ago"
            }
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(240)) { -it } + fadeIn(tween(180)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E1E).copy(alpha = 0.97f))
                    .clickable(onClick = onTap)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                // Header: app icon + app name + time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (headerIcon != null) {
                            Image(
                                bitmap = headerIcon!!,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(headerIconSize)
                                    .clip(RoundedCornerShape(headerIconCorner)),
                            )
                        }
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                }
                // Title
                if (notification.title.isNotBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Body
                if (notification.text.isNotBlank()) {
                    Text(
                        text = notification.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
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
