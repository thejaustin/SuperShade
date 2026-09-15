package com.supershade.service

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
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
import com.supershade.domain.notification.NotificationRepository
import com.supershade.domain.notification.model.NotificationAction
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.settings.ShadeSettings
import com.supershade.ui.theme.DarkThemeMode
import com.supershade.ui.theme.OneUiShadeTheme
import com.supershade.ui.theme.PixelShadeTheme
import com.supershade.ui.theme.PureMaterialShadeTheme
import com.supershade.ui.theme.ShadeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Manages heads-up (peek) notification overlays that float above running apps.
 *
 * Supports:
 * 1. Swipe UP to hide (preserves notification in shade).
 * 2. Swipe LEFT or RIGHT to dismiss (cancels notification from system).
 * 3. Hold down (long-press) to edit notification settings like native operating systems.
 * 4. Inline action buttons (quick reply, mark as read, etc.).
 * 5. Dynamic theming matching One UI, Pixel, or Pure Material.
 */
class HeadsUpOverlay(
    private val context: Context,
    private val notificationRepo: NotificationRepository? = null,
    private val settings: ShadeSettings? = null,
) {

    private val wm: WindowManager = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var currentView: ComposeView? = null
    private var currentLifecycleOwner: HeadsUpLifecycleOwner? = null
    private var currentDismissToken: Any? = null

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    /**
     * Displays a peek card for [notification].
     */
    fun show(notification: ShadeNotification, autoDismissMs: Long = 4_500L) {
        handler.post {
            dismissCurrent()
            val owner = HeadsUpLifecycleOwner()
            currentLifecycleOwner = owner

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    val activeTheme by produceState<ShadeTheme>(ShadeTheme.OneUI) {
                        settings?.theme?.collect { value = it }
                    }
                    val activeDarkMode by produceState(DarkThemeMode.SYSTEM) {
                        settings?.darkThemeMode?.collect { value = it }
                    }

                    val isAmoled = activeDarkMode == DarkThemeMode.AMOLED
                    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when (activeTheme) {
                        ShadeTheme.Pixel -> { content -> PixelShadeTheme(isAmoled = isAmoled, content = content) }
                        ShadeTheme.PureMaterial -> { content ->
                            PureMaterialShadeTheme(
                                isAmoled = isAmoled,
                                darkThemeMode = activeDarkMode,
                                content = content,
                            )
                        }
                        else -> { content -> OneUiShadeTheme(isAmoled = isAmoled, content = content) }
                    }

                    themeWrapper {
                        HeadsUpCard(
                            notification = notification,
                            onTap = {
                                try { notification.contentIntent?.send() } catch (_: Exception) {}
                                handler.post { dismissCurrent() }
                            },
                            onHide = {
                                handler.post { dismissCurrent() }
                            },
                            onDismiss = {
                                notificationRepo?.cancelAndRemove(notification.key)
                                handler.post { dismissCurrent() }
                            },
                            onSnooze = { durationMs ->
                                notificationRepo?.snooze(notification.key, durationMs)
                                handler.post { dismissCurrent() }
                            },
                            onPauseAutoDismiss = {
                                currentDismissToken = null
                                handler.removeCallbacksAndMessages(null)
                            },
                        )
                    }
                }
            }
            currentView = view
            try {
                wm.addView(view, params)
            } catch (_: Exception) {
                owner.destroy()
                currentView = null
                currentLifecycleOwner = null
                return@post
            }

            val token = Any()
            currentDismissToken = token
            handler.postDelayed({
                if (currentDismissToken === token) dismissCurrent()
            }, autoDismissMs)
        }
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        dismissCurrent()
    }

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
    // HeadsUpCard Composable
    // ---------------------------------------------------------------------------

    @Composable
    private fun HeadsUpCard(
        notification: ShadeNotification,
        onTap: () -> Unit,
        onHide: () -> Unit,
        onDismiss: () -> Unit,
        onSnooze: (Long) -> Unit,
        onPauseAutoDismiss: () -> Unit,
    ) {
        val ctx = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val viewConfig = LocalViewConfiguration.current
        val scope = rememberCoroutineScope()

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

        val headerIcon = largeIcon ?: appIcon
        val headerIconSize = if (largeIcon != null) 28.dp else 16.dp
        val headerIconCorner = if (largeIcon != null) 50 else 4

        val appName = remember(notification.packageName) {
            try {
                val info = ctx.packageManager.getApplicationInfo(notification.packageName, 0)
                ctx.packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                notification.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }

        var visible by remember { mutableStateOf(false) }
        var isSettingsMode by remember { mutableStateOf(false) }
        var replyingAction by remember { mutableStateOf<NotificationAction?>(null) }
        var replyText by remember { mutableStateOf("") }

        LaunchedEffect(Unit) { visible = true }

        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }

        val hideThresholdPx = ctx.resources.displayMetrics.density * 36f
        val dismissThresholdPx = ctx.resources.displayMetrics.density * 110f

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(260)) { -it } + fadeIn(tween(200)),
        ) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .graphicsLayer {
                        val maxDelta = max(abs(offsetX.value) / (dismissThresholdPx * 1.5f), abs(offsetY.value) / (hideThresholdPx * 2f))
                        alpha = (1f - maxDelta).coerceIn(0f, 1f)
                    }
                    .animateContentSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isLongPressed = false
                            var isDragging = false

                            val longPressJob = scope.launch {
                                delay(viewConfig.longPressTimeoutMillis)
                                if (!isDragging && !isSettingsMode && replyingAction == null) {
                                    isLongPressed = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPauseAutoDismiss()
                                    isSettingsMode = true
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (!change.pressed) {
                                    longPressJob.cancel()
                                    if (!isDragging && !isLongPressed) {
                                        if (!isSettingsMode && replyingAction == null) {
                                            onTap()
                                        }
                                    } else if (isDragging) {
                                        val dy = offsetY.value
                                        val dx = offsetX.value
                                        if (dy < -hideThresholdPx) {
                                            // Swipe UP to hide
                                            scope.launch {
                                                offsetY.animateTo(-600f, tween(180))
                                                onHide()
                                            }
                                        } else if (abs(dx) >= dismissThresholdPx) {
                                            // Swipe LEFT/RIGHT to dismiss
                                            scope.launch {
                                                val targetX = if (dx > 0) 1400f else -1400f
                                                offsetX.animateTo(targetX, tween(180))
                                                onDismiss()
                                            }
                                        } else {
                                            // Snap back
                                            scope.launch {
                                                launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                                launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                                            }
                                        }
                                    }
                                    break
                                }

                                val curX = change.position.x - down.position.x
                                val curY = change.position.y - down.position.y

                                if (!isDragging && !isSettingsMode && replyingAction == null) {
                                    if (abs(curX) > viewConfig.touchSlop || abs(curY) > viewConfig.touchSlop) {
                                        isDragging = true
                                        longPressJob.cancel()
                                        onPauseAutoDismiss()
                                    }
                                }

                                if (isDragging && !isSettingsMode && replyingAction == null) {
                                    change.consume()
                                    scope.launch {
                                        // Allow upward dragging (curY <= 0) and horizontal dragging
                                        offsetY.snapTo(curY.coerceAtMost(0f))
                                        offsetX.snapTo(curX)
                                    }
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (isSettingsMode) {
                    // ---- OS-STYLE NOTIFICATION SETTINGS VIEW ----
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (headerIcon != null) {
                                    Image(
                                        bitmap = headerIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                    )
                                }
                                Text(
                                    text = "$appName Settings",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            IconButton(
                                onClick = { onHide() },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        if (!notification.channelId.isNullOrBlank()) {
                            Text(
                                text = "Channel: ${notification.channelId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Quick actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, notification.packageName)
                                            putExtra(Settings.EXTRA_CHANNEL_ID, notification.channelId.orEmpty())
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(intent)
                                    } catch (_: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, notification.packageName)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            ctx.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    onHide()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Turn off", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, notification.packageName)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        ctx.startActivity(intent)
                                    } catch (_: Exception) {}
                                    onHide()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Settings", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Snooze shortcuts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text("Snooze:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf(
                                "15m" to 15 * 60 * 1000L,
                                "1h" to 60 * 60 * 1000L,
                                "4h" to 4 * 60 * 60 * 1000L,
                            ).forEach { (label, duration) ->
                                OutlinedButton(
                                    onClick = { onSnooze(duration) },
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                }
                            }
                        }
                    }
                } else {
                    // ---- STANDARD PEEK NOTIFICATION CARD ----
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        bitmap = headerIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(headerIconSize)
                                            .clip(RoundedCornerShape(headerIconCorner)),
                                    )
                                }
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Text(
                                text = "now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }

                        // Title
                        if (notification.title.isNotBlank()) {
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Body text
                        if (notification.text.isNotBlank()) {
                            Text(
                                text = notification.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Action buttons (Reply / Mark read / etc.)
                        if (notification.actions.isNotEmpty() && replyingAction == null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                notification.actions.take(3).forEach { action ->
                                    OutlinedButton(
                                        onClick = {
                                            onPauseAutoDismiss()
                                            if (action.replyInput != null) {
                                                replyingAction = action
                                            } else {
                                                try { action.pendingIntent?.send() } catch (_: Exception) {}
                                                onHide()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = action.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        // Inline quick reply field
                        if (replyingAction != null) {
                            val action = replyingAction!!
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = { Text("Reply…", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                )
                                IconButton(
                                    onClick = {
                                        val ri = action.replyInput ?: return@IconButton
                                        val intent = Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                        RemoteInput.addResultsToIntent(
                                            arrayOf(ri), intent,
                                            Bundle().apply { putCharSequence(ri.resultKey, replyText) }
                                        )
                                        try { action.pendingIntent?.send(ctx, 0, intent) } catch (_: Exception) {}
                                        onHide()
                                    },
                                    enabled = replyText.isNotBlank(),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send reply", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
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
