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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.supershade.settings.SplitGestureMode
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.LocalBackdropTheme
import com.supershade.ui.theme.LocalCardBorderWidth
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.ShadeShapeScheme
import com.supershade.ui.theme.getCardBorder
import com.supershade.ui.tile.TilePreferencesActivity
import com.supershade.viewmodel.ShadePanel
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

    val isTucked = state.isQuickControlsTucked
    val activePanel = state.activePanel

    val nestedScrollConnection = remember(isQsExpanded, isTucked, activePanel) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                // Swiping UP while on notifications panel: tuck quick controls to maximize notification room
                if (dy < -10f && activePanel == ShadePanel.NOTIFICATIONS && !isTucked) {
                    haptics.sheetDetent()
                    viewModel.setQuickControlsTucked(true)
                    return Offset(0f, dy * 0.4f)
                }
                // Swiping UP while Quick Settings is expanded:
                if (dy < -8f && isQsExpanded) {
                    haptics.sheetDetent()
                    viewModel.setQsExpanded(false)
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                // Pulled down at top of notifications list (available.y > 0):
                if (dy > 12f && activePanel == ShadePanel.NOTIFICATIONS && isTucked) {
                    haptics.sheetDetent()
                    viewModel.setQuickControlsTucked(false)
                    return Offset(0f, dy)
                }
                if (dy > 16f && !isQsExpanded && !isTucked) {
                    haptics.sheetDetent()
                    viewModel.setQsExpanded(true)
                    return Offset(0f, dy)
                }
                // Swiping up when feed cannot scroll further up:
                if (dy < -10f && !isQsExpanded) {
                    coroutineScope.launch {
                        dragOffset.snapTo((dragOffset.value + dy * 0.45f).coerceAtMost(0f))
                    }
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (available.y < -velocityThresholdPxPerSec && !isQsExpanded && dragOffset.value < -20f) {
                    dragOffset.animateTo(-3000f, tween(200))
                    onDismiss()
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (dragOffset.value < -dismissThresholdPx || available.y < -velocityThresholdPxPerSec) {
                    if (!isQsExpanded) {
                        dragOffset.animateTo(-3000f, tween(200))
                        onDismiss()
                        return available
                    }
                } else if (dragOffset.value < 0f) {
                    dragOffset.animateTo(0f, spring(0.55f, 450f))
                }
                return Velocity.Zero
            }
        }
    }

    val shapeScheme = remember(state.tileShape) {
        ShadeShapeScheme.fromTileShape(state.tileShape)
    }

    val backdropTheme = state.backdropTheme

    CompositionLocalProvider(
        LocalCardBorderWidth provides state.cardBorderWidth,
        LocalShadeShapeScheme provides shapeScheme,
        LocalBackdropTheme provides backdropTheme,
    ) {
        themeWrapper {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dimmer scrim — tapping it dismisses the shade.
                val opacity = state.backdropOpacity.coerceIn(0.20f, 1.00f)
                val scrimAlpha = when {
                    opacity >= 0.99f -> 0.70f
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> (opacity * 0.36f).coerceIn(0.12f, 0.45f)
                    else -> (opacity * 0.65f).coerceIn(0.25f, 0.75f)
                }
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
                        opacity >= 0.99f -> {
                            if (isAmoled) Color(0xFF000000) else MaterialTheme.colorScheme.surface
                        }
                        isAmoled -> Color(0xFF05070A).copy(alpha = opacity)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = opacity)
                    }
                    val isCombined = state.splitGestureMode == SplitGestureMode.ALWAYS_NOTIFICATIONS ||
                                     state.splitGestureMode == SplitGestureMode.ALWAYS_QUICK_SETTINGS
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                            .background(glassBackdrop)
                            .displayCutoutPadding()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .draggable(
                                orientation = Orientation.Horizontal,
                                enabled = !isCombined,
                                state = rememberDraggableState { delta ->
                                    if (delta < -24f && state.activePanel == ShadePanel.NOTIFICATIONS) {
                                        haptics.sheetDetent()
                                        viewModel.setActivePanel(ShadePanel.QUICK_SETTINGS)
                                    } else if (delta > 24f && state.activePanel == ShadePanel.QUICK_SETTINGS) {
                                        haptics.sheetDetent()
                                        viewModel.setActivePanel(ShadePanel.NOTIFICATIONS)
                                    }
                                },
                            ),
                    ) {
                        // Top Status Bar (Clock, Battery, Lock, Settings, Power, Edit)
                        StatusBarRow(
                            statusBar = state.statusBar,
                            onOpenPowerMenu = { showPowerMenu = true },
                            onOpenEdit = {
                                try {
                                    val intent = Intent(context, TilePreferencesActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    onDismiss()
                                } catch (_: Exception) {}
                            },
                            onOpenDeviceSettings = {
                                onDismiss()
                            },
                            onOpenSettings = {
                                try {
                                    val intent = Intent(context, com.supershade.MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    onDismiss()
                                } catch (_: Exception) {}
                            },
                            onLockScreen = { viewModel.lockScreen() },
                        )

                        // Active View Content
                        if (state.activePanel == ShadePanel.NOTIFICATIONS) {
                            // Quick Controls section on Notifications panel
                            AnimatedVisibility(
                                visible = !state.isQuickControlsTucked,
                                enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(tween(150)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(150)),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .draggable(
                                            orientation = Orientation.Vertical,
                                            state = rememberDraggableState { delta ->
                                                if (delta < -8f) {
                                                    haptics.sheetDetent()
                                                    viewModel.setQuickControlsTucked(true)
                                                } else if (delta > 14f && isCombined) {
                                                    haptics.sheetDetent()
                                                    viewModel.setActivePanel(ShadePanel.QUICK_SETTINGS)
                                                } else if (delta < -4f) {
                                                    coroutineScope.launch {
                                                        dragOffset.snapTo((dragOffset.value + delta).coerceAtMost(0f))
                                                    }
                                                }
                                            },
                                            onDragStopped = { velocity ->
                                                if (velocity < -velocityThresholdPxPerSec || dragOffset.value < -dismissThresholdPx) {
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
                                        ),
                                ) {
                                    QuickSettingsGrid(
                                        tiles = state.tiles,
                                        theme = state.theme,
                                        isShizukuConnected = state.isShizukuConnected,
                                        isExpanded = false,
                                        tileShape = state.tileShape,
                                        tileSize = state.tileSize,
                                        tileColumns = state.tileColumns,
                                        showWideCards = state.showWideCards,
                                        onTileClick = { viewModel.toggleTile(it) },
                                        onTileLongClick = { viewModel.openTileDetail(it) },
                                    )

                                    Surface(
                                        shape = shapeScheme.container,
                                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                                        border = getCardBorder(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 2.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                        ) {
                                            BrightnessSlider(
                                                brightness = state.brightness,
                                                onBrightnessChange = { viewModel.setBrightness(it) },
                                                compact = false,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }

                                    // Pill handle to tuck quick controls
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f),
                                            border = getCardBorder(),
                                            modifier = Modifier.clickable {
                                                haptics.sheetDetent()
                                                viewModel.setQuickControlsTucked(true)
                                            },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(24.dp)
                                                        .height(3.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)),
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Tuck Quick Controls",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Minimal slim bar when quick controls are tucked
                            AnimatedVisibility(
                                visible = state.isQuickControlsTucked,
                                enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(tween(150)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(150)),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                                    border = getCardBorder(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 3.dp)
                                        .clickable {
                                            haptics.sheetDetent()
                                            viewModel.setQuickControlsTucked(false)
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Text(
                                                text = "Quick Controls tucked",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Text(
                                                text = "Swipe down to show",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
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

                            // Notification feed with full remaining space and responsive nested-scroll coordination
                            NotificationFeed(
                                notifications = state.visibleNotifications,
                                onDismiss = { viewModel.dismissNotification(it) },
                                onClearAll = { viewModel.clearAllNotifications() },
                                onNotificationClick = { notification ->
                                    viewModel.launchNotification(notification)
                                    onDismiss()
                                },
                                onSnooze = { key, delayMs -> viewModel.snoozeNotification(key, delayMs) },
                                modifier = Modifier
                                    .weight(1f)
                                    .nestedScroll(nestedScrollConnection),
                            )
                        } else {
                            // QUICK SETTINGS PANEL (Full Control Center)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                QuickSettingsGrid(
                                    tiles = state.tiles,
                                    theme = state.theme,
                                    isShizukuConnected = state.isShizukuConnected,
                                    isExpanded = true,
                                    tileShape = state.tileShape,
                                    tileSize = state.tileSize,
                                    tileColumns = state.tileColumns,
                                    showWideCards = state.showWideCards,
                                    onTileClick = { viewModel.toggleTile(it) },
                                    onTileLongClick = { viewModel.openTileDetail(it) },
                                )

                                // Full tactile sliders island (Brightness & Volume)
                                Surface(
                                    shape = shapeScheme.container,
                                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
                                    border = getCardBorder(),
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
                                        VolumeSlider(
                                            compact = false,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
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
                            }
                        }

                        // Bottom Panel Switcher Pill (accessibility and quick-switching dock)
                        if (state.showPanelSwitcherPill) {
                            if (!isCombined) {
                                // Separate (Split) mode: Horizontal segmented pill dock
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f),
                                        border = getCardBorder(),
                                    ) {
                                        Row(modifier = Modifier.padding(3.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (state.activePanel == ShadePanel.NOTIFICATIONS) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        haptics.lightTap()
                                                        viewModel.setActivePanel(ShadePanel.NOTIFICATIONS)
                                                    },
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        text = "Notifications",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (state.activePanel == ShadePanel.NOTIFICATIONS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                    if (state.allNotifications.isNotEmpty()) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (state.activePanel == ShadePanel.NOTIFICATIONS) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                                            modifier = Modifier.size(18.dp),
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = "${state.allNotifications.size}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                                    color = if (state.activePanel == ShadePanel.NOTIFICATIONS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (state.activePanel == ShadePanel.QUICK_SETTINGS) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        haptics.lightTap()
                                                        viewModel.setActivePanel(ShadePanel.QUICK_SETTINGS)
                                                    },
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                ) {
                                                    Text(
                                                        text = "Quick Settings",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = if (state.activePanel == ShadePanel.QUICK_SETTINGS) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Combined (Together) mode: Vertical compact pill button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f),
                                        border = getCardBorder(),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                haptics.lightTap()
                                                if (state.activePanel == ShadePanel.NOTIFICATIONS) {
                                                    viewModel.setActivePanel(ShadePanel.QUICK_SETTINGS)
                                                } else {
                                                    viewModel.setActivePanel(ShadePanel.NOTIFICATIONS)
                                                }
                                            },
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (state.activePanel == ShadePanel.NOTIFICATIONS)
                                                    Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = if (state.activePanel == ShadePanel.NOTIFICATIONS)
                                                    "Quick Settings" else "Notifications",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom drag-handle — swipe up to dismiss with spring physics, or tap for fast quick-close
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 46.dp)
                                .clickable {
                                    haptics.sheetDetent()
                                    coroutineScope.launch {
                                        dragOffset.animateTo(-3000f, tween(180))
                                        onDismiss()
                                    }
                                }
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
                                    .width(44.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)),
                            )
                        }
                    }
                }
            }

            // In-shade Tile Detail Sheet (Flashlight multi-level control, Wi-Fi details, Bluetooth devices)
            state.activeTileDetail?.let { detail ->
                QuickTileDetailSheet(
                    detailState = detail,
                    onDismiss = { viewModel.closeTileDetail() },
                    onSetTorchStrength = { viewModel.setTorchStrength(it) },
                    onToggleTorch = { viewModel.toggleTorchInDetail() },
                )
            }
        }
    }
}
