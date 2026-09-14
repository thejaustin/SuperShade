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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.supershade.ui.theme.PureMaterialShadeTheme
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
    val isQsExpanded = state.isQsExpanded

    val categoryCounts by remember {
        derivedStateOf {
            val all = state.allNotifications
            val counts = mutableMapOf<ShadeCategory, Int>()
            all.forEach { n -> counts[n.category] = (counts[n.category] ?: 0) + 1 }
            counts[ShadeCategory.All] = all.size
            counts as Map<ShadeCategory, Int>
        }
    }

    val isAmoled = state.darkThemeMode == com.supershade.ui.theme.DarkThemeMode.AMOLED
    val themeWrapper: @Composable (@Composable () -> Unit) -> Unit = when (state.theme) {
        ShadeTheme.Pixel -> { content -> PixelShadeTheme(isAmoled = isAmoled, content = content) }
        ShadeTheme.PureMaterial -> { content ->
            PureMaterialShadeTheme(
                isAmoled = isAmoled,
                darkThemeMode = state.darkThemeMode,
                content = content,
            )
        }
        else -> { content -> OneUiShadeTheme(isAmoled = isAmoled, content = content) }
    }

    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 72.dp.toPx() }
    val velocityThresholdPxPerSec = with(density) { 400.dp.toPx() }

    // Reset drag position whenever the shade re-opens.
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            dragOffset.snapTo(0f)
        }
    }

    themeWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dimmer scrim — tapping it dismisses the shade.
            val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.35f else 0.60f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(onClick = onDismiss),
            )

            // Shade panel: expands down from the top, rounded bottom corners
            AnimatedVisibility(
                visible = state.isOpen,
                enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(200)),
                exit  = slideOutVertically(tween(250)) { -it } + fadeOut(tween(200)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.93f)
                        .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding(),
                ) {
                    StatusBarRow(statusBar = state.statusBar)

                    // Quick Settings grid (compact 1-row or expanded 2-row)
                    QuickSettingsGrid(
                        tiles = state.tiles,
                        theme = state.theme,
                        isShizukuConnected = state.isShizukuConnected,
                        isExpanded = isQsExpanded,
                        onTileClick = { viewModel.toggleTile(it) },
                    )

                    // Compact Dual Sliders Row: Brightness & Volume side-by-side
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrightnessSlider(
                            brightness = state.brightness,
                            onBrightnessChange = { viewModel.setBrightness(it) },
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        VolumeSlider(
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Quick Settings expansion chevron / drag handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clickable { viewModel.setQsExpanded(!isQsExpanded) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isQsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isQsExpanded) "Collapse Quick Settings" else "Expand Quick Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Media playback card (if active)
                    state.media?.let { media ->
                        MediaCard(
                            media = media,
                            onPlayPause = { viewModel.mediaPlayPause() },
                            onSkipNext = { viewModel.mediaSkipNext() },
                            onSkipPrevious = { viewModel.mediaSkipPrevious() },
                            onSeek = { viewModel.mediaSeek(it) },
                        )
                    }

                    // Notification category bar
                    CategoryBar(
                        categories = ShadeCategory.entries,
                        selected = state.selectedCategory,
                        onSelect = { viewModel.selectCategory(it) },
                        counts = categoryCounts,
                    )

                    // Notification feed with full remaining space
                    NotificationFeed(
                        notifications = state.visibleNotifications,
                        onDismiss = { viewModel.dismissNotification(it) },
                        onClearAll = { viewModel.clearAllNotifications() },
                        onNotificationClick = { notification ->
                            viewModel.launchNotification(notification)
                            onDismiss()
                        },
                        onSnooze = { key, delayMs -> viewModel.snoozeNotification(key, delayMs) },
                        modifier = Modifier.weight(1f),
                    )

                    // Bottom drag-handle — swipe up to dismiss with spring physics
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    coroutineScope.launch {
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
                                            dragOffset.animateTo(
                                                targetValue = -3000f,
                                                animationSpec = tween(durationMillis = 200),
                                            )
                                            onDismiss()
                                        } else {
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
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
        }
    }
}
