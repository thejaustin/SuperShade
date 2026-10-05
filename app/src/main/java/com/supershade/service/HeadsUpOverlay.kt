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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.BorderStroke
import com.supershade.ui.theme.getCardBorder
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.MusicNote
import com.supershade.domain.media.MediaState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.supershade.ui.shade.AudioOutputChip
import com.supershade.ui.theme.ChamferedCornerShape
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
import com.supershade.ui.theme.CyberpunkShadeTheme
import com.supershade.ui.theme.DarkThemeMode
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.NothingShadeTheme
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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            val density = context.resources.displayMetrics.density
            blurBehindRadius = (22 * density).toInt().coerceIn(50, 90)
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
                        ShadeTheme.Nothing -> { content ->
                            NothingShadeTheme(
                                isAmoled = isAmoled,
                                darkThemeMode = activeDarkMode,
                                content = content,
                            )
                        }
                        ShadeTheme.Cyberpunk -> { content ->
                            CyberpunkShadeTheme(
                                isAmoled = isAmoled,
                                darkThemeMode = activeDarkMode,
                                content = content,
                            )
                        }
                        else -> { content -> OneUiShadeTheme(isAmoled = isAmoled, content = content) }
                    }

                    val superHaptics = remember { SuperHaptics(context) }
                    CompositionLocalProvider(
                        LocalShadeTheme provides activeTheme,
                        LocalSuperHaptics provides superHaptics,
                    ) {
                        themeWrapper {
                            HeadsUpCard(
                                notification = notification,
                                isAmoled = isAmoled,
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

    /**
     * Displays an ambient lockscreen/peek card for active [media].
     */
    fun showMedia(
        media: MediaState,
        onPlayPause: () -> Unit,
        onSkipNext: () -> Unit,
        onSkipPrevious: () -> Unit,
        autoDismissMs: Long = 6_000L,
    ) {
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
                        ShadeTheme.Nothing -> { content ->
                            NothingShadeTheme(
                                isAmoled = isAmoled,
                                darkThemeMode = activeDarkMode,
                                content = content,
                            )
                        }
                        ShadeTheme.Cyberpunk -> { content ->
                            CyberpunkShadeTheme(
                                isAmoled = isAmoled,
                                darkThemeMode = activeDarkMode,
                                content = content,
                            )
                        }
                        else -> { content -> OneUiShadeTheme(isAmoled = isAmoled, content = content) }
                    }

                    val superHaptics = remember { SuperHaptics(context) }
                    CompositionLocalProvider(
                        LocalShadeTheme provides activeTheme,
                        LocalSuperHaptics provides superHaptics,
                    ) {
                        themeWrapper {
                            AmbientMediaCard(
                                media = media,
                                isAmoled = isAmoled,
                                onPlayPause = onPlayPause,
                                onSkipNext = onSkipNext,
                                onSkipPrevious = onSkipPrevious,
                                onHide = {
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
        isAmoled: Boolean = false,
        onTap: () -> Unit,
        onHide: () -> Unit,
        onDismiss: () -> Unit,
        onSnooze: (Long) -> Unit,
        onPauseAutoDismiss: () -> Unit,
    ) {
        val ctx = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val haptics = LocalSuperHaptics.current ?: remember(ctx) { SuperHaptics(ctx) }
        val viewConfig = LocalViewConfiguration.current
        val scope = rememberCoroutineScope()
        val theme = LocalShadeTheme.current

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
            val cardShape = when (theme) {
                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(12.dp)
                is ShadeTheme.Nothing -> RoundedCornerShape(14.dp)
                is ShadeTheme.OneUI -> RoundedCornerShape(26.dp)
                else -> RoundedCornerShape(28.dp)
            }
            val cardBorder = when (theme) {
                is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.50f))
                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                is ShadeTheme.OneUI -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                else -> getCardBorder(alpha = 0.35f)
            }
            val cardBg = when {
                isAmoled -> Color(0xF005070A)
                theme is ShadeTheme.Cyberpunk -> Color(0xEB060A14)
                theme is ShadeTheme.Nothing -> Color(0xEB0A0B0E)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f)
            }
            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = cardBg,
                ),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .displayCutoutPadding()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .graphicsLayer {
                        val maxDelta = max(abs(offsetX.value) / (dismissThresholdPx * 1.5f), abs(offsetY.value) / (hideThresholdPx * 2f))
                        alpha = (1f - maxDelta).coerceIn(0f, 1f)
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var isLongPressed = false
                            var isDragging = false

                            val longPressJob = scope.launch {
                                delay(viewConfig.longPressTimeoutMillis)
                                if (!isDragging && !isSettingsMode && replyingAction == null) {
                                    isLongPressed = true
                                    haptics.sheetDetent()
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
                                            haptics.lightTap()
                                            onTap()
                                        }
                                    } else if (isDragging) {
                                        val dy = offsetY.value
                                        val dx = offsetX.value
                                        if (dy < -hideThresholdPx) {
                                            // Swipe UP to hide
                                            haptics.sheetDetent()
                                            scope.launch {
                                                offsetY.animateTo(-600f, tween(180))
                                                onHide()
                                            }
                                        } else if (abs(dx) >= dismissThresholdPx) {
                                            // Swipe LEFT/RIGHT to dismiss
                                            haptics.tileToggleOff()
                                            scope.launch {
                                                val targetX = if (dx > 0) 1400f else -1400f
                                                offsetX.animateTo(targetX, tween(180))
                                                onDismiss()
                                            }
                                        } else {
                                            // Snap back with fluid spring physics
                                            scope.launch {
                                                launch { offsetX.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 400f)) }
                                                launch { offsetY.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 400f)) }
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
                    },
            ) {
                if (isSettingsMode) {
                    // ---- OS-STYLE NOTIFICATION SETTINGS VIEW ----
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                    onClick = {
                                        haptics.lightTap()
                                        onSnooze(duration)
                                    },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                val headerIconShape = when (theme) {
                                    is ShadeTheme.Cyberpunk -> ChamferedCornerShape(4.dp)
                                    is ShadeTheme.Nothing -> RoundedCornerShape(6.dp)
                                    else -> RoundedCornerShape(headerIconCorner)
                                }
                                if (headerIcon != null) {
                                    Image(
                                        bitmap = headerIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(headerIconSize)
                                            .clip(headerIconShape),
                                    )
                                }
                                Text(
                                    text = if (theme is ShadeTheme.Nothing) appName.uppercase() else appName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                        letterSpacing = if (theme is ShadeTheme.Nothing) 0.5.sp else 0.sp,
                                    ),
                                    color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Text(
                                text = if (theme is ShadeTheme.Cyberpunk) "[NOW]" else "now",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                ),
                                color = if (theme is ShadeTheme.Cyberpunk) Color(0xFFFF007F).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }

                        // Title
                        if (notification.title.isNotBlank()) {
                            Text(
                                text = notification.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                    lineHeight = 18.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Body text
                        if (notification.text.isNotBlank()) {
                            Text(
                                text = notification.text,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 16.sp,
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = true,
                            )
                        }

                        // Action buttons (Reply / Mark read / etc.)
                        if (notification.actions.isNotEmpty() && replyingAction == null) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val actionShape = when (theme) {
                                    is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                                    is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                                    is ShadeTheme.OneUI -> RoundedCornerShape(14.dp)
                                    else -> RoundedCornerShape(12.dp)
                                }
                                val actionBorder = when (theme) {
                                    is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f))
                                    is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                                    else -> null
                                }
                                notification.actions.take(3).forEach { action ->
                                    val actionFontSize = if (action.label.length > 11) 10.sp else 11.sp
                                    OutlinedButton(
                                        onClick = {
                                            haptics.tileToggleOn()
                                            onPauseAutoDismiss()
                                            if (action.replyInput != null) {
                                                replyingAction = action
                                            } else {
                                                try { action.pendingIntent?.send() } catch (_: Exception) {}
                                                onHide()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp),
                                        shape = actionShape,
                                        border = actionBorder,
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = if (theme is ShadeTheme.Nothing) action.label.uppercase() else action.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = actionFontSize,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                                letterSpacing = if (theme is ShadeTheme.Nothing) 0.5.sp else 0.sp,
                                            ),
                                            color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }
                        }

                        // Inline quick reply field
                        if (replyingAction != null) {
                            val action = replyingAction!!
                            val replyShape = when (theme) {
                                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                                is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                                is ShadeTheme.OneUI -> RoundedCornerShape(20.dp)
                                else -> RoundedCornerShape(20.dp)
                            }
                            val replyPlaceholder = when (theme) {
                                is ShadeTheme.Cyberpunk -> "[INPUT_TRANSMISSION...]"
                                is ShadeTheme.Nothing -> "REPLY..."
                                else -> "Reply…"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    placeholder = {
                                        Text(
                                            replyPlaceholder,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else FontFamily.Default,
                                                letterSpacing = if (theme is ShadeTheme.Nothing) 0.6.sp else 0.sp,
                                            ),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = replyShape,
                                )
                                IconButton(
                                    onClick = {
                                        haptics.tileToggleOn()
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
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send reply",
                                        tint = when {
                                            !replyText.isNotBlank() -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            theme is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                                            theme is ShadeTheme.Nothing -> Color(0xFFD71920)
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AmbientMediaCard(
        media: MediaState,
        isAmoled: Boolean = false,
        onPlayPause: () -> Unit,
        onSkipNext: () -> Unit,
        onSkipPrevious: () -> Unit,
        onHide: () -> Unit,
        onPauseAutoDismiss: () -> Unit,
    ) {
        val ctx = LocalContext.current
        val haptics = LocalSuperHaptics.current ?: remember(ctx) { SuperHaptics(ctx) }
        val scope = rememberCoroutineScope()
        val theme = LocalShadeTheme.current

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val offsetY = remember { Animatable(0f) }
        val hideThresholdPx = ctx.resources.displayMetrics.density * 36f

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(260)) { -it } + fadeIn(tween(200)),
        ) {
            val cardShape = when (theme) {
                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(12.dp)
                is ShadeTheme.Nothing -> RoundedCornerShape(16.dp)
                is ShadeTheme.Pixel -> RoundedCornerShape(28.dp)
                else -> RoundedCornerShape(26.dp)
            }
            val cardBorder = when (theme) {
                is ShadeTheme.Cyberpunk -> BorderStroke(1.5.dp, Color(0xFF00F0FF))
                is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                else -> getCardBorder(alpha = 0.35f)
            }
            val cardBg = if (isAmoled) {
                Color(0xF005070A)
            } else when (theme) {
                is ShadeTheme.Cyberpunk -> Color(0xF0080C14)
                is ShadeTheme.Nothing -> Color(0xF00A0C0E)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)
            }

            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .displayCutoutPadding()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .graphicsLayer {
                        val delta = (abs(offsetY.value) / (hideThresholdPx * 2f)).coerceIn(0f, 1f)
                        alpha = 1f - delta
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            onPauseAutoDismiss()
                            var isDragging = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    if (isDragging) {
                                        val dy = offsetY.value
                                        if (dy < -hideThresholdPx) {
                                            haptics.sheetDetent()
                                            scope.launch {
                                                offsetY.animateTo(-hideThresholdPx * 3.5f, tween(160))
                                                onHide()
                                            }
                                        } else {
                                            scope.launch {
                                                offsetY.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 800f))
                                            }
                                        }
                                    }
                                    break
                                }
                                val dy = change.position.y - down.position.y
                                if (abs(dy) > 12f) isDragging = true
                                if (isDragging) {
                                    change.consume()
                                    scope.launch { offsetY.snapTo(dy.coerceAtMost(20f)) }
                                }
                            }
                        }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Top header badge for One UI, Nothing, Cyberpunk
                    if (theme is ShadeTheme.OneUI || theme is ShadeTheme.Nothing || theme is ShadeTheme.Cyberpunk) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            when (theme) {
                                is ShadeTheme.Cyberpunk -> {
                                    Text(
                                        text = "[SYS.AUDIO // PEEK]",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = Color(0xFF00F0FF),
                                    )
                                }
                                is ShadeTheme.Nothing -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD71920)),
                                        )
                                        Text(
                                            text = "NOW PLAYING",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.0.sp,
                                                fontSize = 10.sp,
                                            ),
                                            color = Color.White.copy(alpha = 0.85f),
                                        )
                                    }
                                }
                                is ShadeTheme.OneUI -> {
                                    Text(
                                        text = "Media output",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                else -> {}
                            }
                            AudioOutputChip(
                                packageName = media.packageName,
                                theme = theme,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Album Art
                        val albumArt = media.albumArt
                        val artShape = when (theme) {
                            is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                            is ShadeTheme.Nothing -> RoundedCornerShape(8.dp)
                            is ShadeTheme.OneUI -> RoundedCornerShape(16.dp)
                            else -> RoundedCornerShape(12.dp)
                        }
                        if (albumArt != null) {
                            Image(
                                bitmap = albumArt.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(artShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(artShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }

                        // Track Info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            val titleText = when (theme) {
                                is ShadeTheme.Cyberpunk -> "[TRK // ${media.title.ifBlank { "ONLINE" }}]"
                                is ShadeTheme.Nothing -> media.title.ifBlank { "UNKNOWN" }.uppercase()
                                else -> media.title.ifBlank { "Unknown Title" }
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                                    letterSpacing = if (theme is ShadeTheme.Nothing) 0.8.sp else 0.sp,
                                ),
                                color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (media.artist.isNotBlank()) {
                                val artistText = when (theme) {
                                    is ShadeTheme.Cyberpunk -> "[ART // ${media.artist}]"
                                    is ShadeTheme.Nothing -> "// ${media.artist.uppercase()}"
                                    else -> media.artist
                                }
                                Text(
                                    text = artistText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                                        letterSpacing = if (theme is ShadeTheme.Nothing) 0.6.sp else 0.sp,
                                    ),
                                    color = if (theme is ShadeTheme.Cyberpunk) Color(0xFFFF007F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {
                                    onPauseAutoDismiss()
                                    haptics.lightTap()
                                    onSkipPrevious()
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            val playBtnShape = when (theme) {
                                is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
                                is ShadeTheme.Nothing -> CircleShape
                                else -> CircleShape
                            }
                            val playBtnColor = when (theme) {
                                is ShadeTheme.Cyberpunk -> Color(0xFF00F0FF)
                                is ShadeTheme.Nothing -> Color.White
                                else -> MaterialTheme.colorScheme.primary
                            }
                            val playIconColor = when (theme) {
                                is ShadeTheme.Cyberpunk -> Color.Black
                                is ShadeTheme.Nothing -> Color.Black
                                else -> MaterialTheme.colorScheme.onPrimary
                            }

                            Surface(
                                onClick = {
                                    onPauseAutoDismiss()
                                    if (media.isPlaying) haptics.tileToggleOff() else haptics.tileToggleOn()
                                    onPlayPause()
                                },
                                shape = playBtnShape,
                                color = playBtnColor,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (media.isPlaying) "Pause" else "Play",
                                        tint = playIconColor,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    onPauseAutoDismiss()
                                    haptics.lightTap()
                                    onSkipNext()
                                },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // Progress bar
                    if (media.duration > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = (media.position.toFloat() / media.duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                        when (theme) {
                            is ShadeTheme.Nothing -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(Color.White.copy(alpha = 0.20f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progress)
                                            .height(2.dp)
                                            .background(Color.White.copy(alpha = 0.90f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progress)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD71920))
                                        )
                                    }
                                }
                            }
                            is ShadeTheme.Cyberpunk -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(ChamferedCornerShape(2.dp))
                                        .background(Color(0xFF141F30)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progress)
                                            .fillMaxHeight()
                                            .clip(ChamferedCornerShape(2.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF00F0FF), Color(0xFFFF007F))
                                                )
                                            )
                                    )
                                }
                            }
                            is ShadeTheme.OneUI -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = progress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                }
                            }
                            else -> {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                )
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
