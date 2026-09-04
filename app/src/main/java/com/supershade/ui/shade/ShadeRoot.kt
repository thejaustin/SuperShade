package com.supershade.ui.shade

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.ui.theme.OneUiShadeTheme
import com.supershade.ui.theme.PixelShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ShadeRoot(
    viewModel: ShadeViewModel,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val categoryCounts by remember {
        derivedStateOf {
            val all = state.allNotifications
            val counts = mutableMapOf<ShadeCategory, Int>()
            all.forEach { n -> counts[n.category] = (counts[n.category] ?: 0) + 1 }
            counts[ShadeCategory.All] = all.size
            counts as Map<ShadeCategory, Int>
        }
    }

    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when (state.theme) {
        ShadeTheme.Pixel -> { content -> PixelShadeTheme(content) }
        else             -> { content -> OneUiShadeTheme(content) }
    }

    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 72.dp.toPx() }
    val velocityThresholdPxPerSec = with(density) { 400.dp.toPx() }

    // Reset drag position whenever the shade re-opens.
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) dragOffset.snapTo(0f)
    }

    themeWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dimmer scrim — tapping it dismisses the shade.
            // On Android 12+ the compositor handles background blur so we can use
            // a lighter tint; on older versions the darker value provides contrast.
            val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.25f else 0.55f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(onClick = onDismiss),
            )

            // Shade panel: slides down from the top, rounded bottom corners
            AnimatedVisibility(
                visible = state.isOpen,
                enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(200)),
                exit  = slideOutVertically(tween(250)) { -it } + fadeOut(tween(200)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.72f)
                        .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        // Push content below the system status bar. The shade background
                        // still fills from y=0 (behind the status bar), but StatusBarRow
                        // and all subsequent content start at the status bar bottom.
                        .statusBarsPadding(),
                ) {
                    StatusBarRow(statusBar = state.statusBar)
                    QuickSettingsGrid(
                        tiles = state.tiles,
                        theme = state.theme,
                        isShizukuConnected = state.isShizukuConnected,
                        onTileClick = { viewModel.toggleTile(it) },
                    )
                    BrightnessSlider(
                        brightness = state.brightness,
                        onBrightnessChange = { viewModel.setBrightness(it) },
                    )
                    state.media?.let { media ->
                        MediaCard(
                            media = media,
                            onPlayPause = { viewModel.mediaPlayPause() },
                            onSkipNext = { viewModel.mediaSkipNext() },
                            onSkipPrevious = { viewModel.mediaSkipPrevious() },
                        )
                    }
                    CategoryBar(
                        categories = ShadeCategory.entries,
                        selected = state.selectedCategory,
                        onSelect = { viewModel.selectCategory(it) },
                        counts = categoryCounts,
                    )
                    NotificationFeed(
                        notifications = state.visibleNotifications,
                        onDismiss = { viewModel.dismissNotification(it) },
                        onClearAll = { viewModel.clearAllNotifications() },
                        onNotificationClick = { notification ->
                            viewModel.launchNotification(notification)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    )

                    // Drag-handle — swipe up to dismiss with spring physics.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
                                        // Only accept upward drags (negative delta).
                                        dragOffset.snapTo(
                                            (dragOffset.value + delta).coerceAtMost(0f)
                                        )
                                    }
                                },
                                onDragStopped = { velocity ->
                                    coroutineScope.launch {
                                        if (dragOffset.value < -dismissThresholdPx ||
                                            velocity < -velocityThresholdPxPerSec
                                        ) {
                                            // Fly off screen then dismiss.
                                            dragOffset.animateTo(
                                                targetValue = -3000f,
                                                animationSpec = tween(durationMillis = 200),
                                            )
                                            onDismiss()
                                        } else {
                                            // Spring back to resting position.
                                            dragOffset.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.55f,
                                                    stiffness = 450f,
                                                ),
                                            )
                                        }
                                    }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
        }
    }
}
