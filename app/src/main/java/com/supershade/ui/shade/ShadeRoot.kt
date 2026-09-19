package com.supershade.ui.shade

import android.content.Intent
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

    val context = LocalContext.current
    var showPowerMenu by remember { mutableStateOf(false) }

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
        ShadeTheme.Pixel -> { content ->
            PixelShadeTheme(
                isAmoled = isAmoled,
                accentColor = state.accentColor,
                content = content,
            )
        }
        ShadeTheme.PureMaterial -> { content ->
            PureMaterialShadeTheme(
                isAmoled = isAmoled,
                darkThemeMode = state.darkThemeMode,
                accentColor = state.accentColor,
                content = content,
            )
        }
        else -> { content ->
            OneUiShadeTheme(
                isAmoled = isAmoled,
                accentColor = state.accentColor,
                content = content,
            )
        }
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

    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    themeWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dimmer scrim — tapping it dismisses the shade.
            val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.22f else 0.55f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(onClick = {
                        haptics.lightTap()
                        onDismiss()
                    }),
            )

            // Quick Power Menu Dialog
            if (showPowerMenu) {
                PowerMenuDialog(
                    onDismiss = { showPowerMenu = false },
                    onLockScreen = {
                        showPowerMenu = false
                        viewModel.lockScreen()
                        onDismiss()
                    },
                    onRestart = {
                        showPowerMenu = false
                        viewModel.restartDevice()
                        onDismiss()
                    },
                    onPowerOff = {
                        showPowerMenu = false
                        viewModel.powerOffDevice()
                        onDismiss()
                    },
                    onSystemPowerDialog = {
                        showPowerMenu = false
                        viewModel.openSystemPowerDialog()
                        onDismiss()
                    },
                )
            }

            // Shade panel: expands down from the top, rounded bottom corners, frosted glass backdrop
            AnimatedVisibility(
                visible = state.isOpen,
                enter = slideInVertically(spring(dampingRatio = 0.78f, stiffness = 420f)) { -it } + fadeIn(tween(180)),
                exit  = slideOutVertically(tween(220)) { -it } + fadeOut(tween(180)),
            ) {
                val glassBackdrop = when {
                    isAmoled -> Color(0xF005070A)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                        .background(glassBackdrop)
                        .displayCutoutPadding()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    StatusBarRow(
                        statusBar = state.statusBar,
                        onOpenPowerMenu = { showPowerMenu = true },
                        onOpenSettings = {
                            try {
                                val intent = Intent(context, com.supershade.MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                onDismiss()
                            } catch (_: Exception) {}
                        },
                    )

                    // Quick Settings grid (compact 1-row or expanded 2-row)
                    QuickSettingsGrid(
                        tiles = state.tiles,
                        theme = state.theme,
                        isShizukuConnected = state.isShizukuConnected,
                        isExpanded = isQsExpanded,
                        tileShape = state.tileShape,
                        tileColumns = state.tileColumns,
                        showWideCards = state.showWideCards,
                        onTileClick = { viewModel.toggleTile(it) },
                    )

                    // Full-Width Tactile Sliders Island
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 3.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BrightnessSlider(
                                brightness = state.brightness,
                                onBrightnessChange = { viewModel.setBrightness(it) },
                                compact = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (isQsExpanded) {
                                VolumeSlider(
                                    compact = false,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    // Quick Settings expansion pill handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f),
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {
                                    role = Role.Button
                                    contentDescription = if (isQsExpanded) "Collapse Quick Settings" else "Expand Quick Settings"
                                    stateDescription = if (isQsExpanded) "Expanded" else "Collapsed"
                                }
                                .defaultMinSize(minWidth = 72.dp, minHeight = 36.dp)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { delta ->
                                        if (delta > 8f && !isQsExpanded) {
                                            haptics.sheetDetent()
                                            viewModel.setQsExpanded(true)
                                        } else if (delta < -8f && isQsExpanded) {
                                            haptics.sheetDetent()
                                            viewModel.setQsExpanded(false)
                                        } else if (delta < -4f && !isQsExpanded) {
                                            coroutineScope.launch {
                                                dragOffset.snapTo((dragOffset.value + delta).coerceAtMost(0f))
                                            }
                                        }
                                    },
                                    onDragStopped = { velocity ->
                                        if (!isQsExpanded && (velocity < -velocityThresholdPxPerSec || dragOffset.value < -dismissThresholdPx)) {
                                            coroutineScope.launch {
                                                dragOffset.animateTo(-3000f, tween(200))
                                                onDismiss()
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                dragOffset.animateTo(0f, spring(0.55f, 450f))
                                            }
                                        }
                                    },
                                )
                                .clickable {
                                    haptics.sheetDetent()
                                    viewModel.setQsExpanded(!isQsExpanded)
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)),
                                )
                                Icon(
                                    imageVector = if (isQsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
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
                    if (state.allNotifications.isNotEmpty()) {
                        CategoryBar(
                            categories = ShadeCategory.entries,
                            selected = state.selectedCategory,
                            onSelect = { viewModel.selectCategory(it) },
                            counts = categoryCounts,
                        )
                    }

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
