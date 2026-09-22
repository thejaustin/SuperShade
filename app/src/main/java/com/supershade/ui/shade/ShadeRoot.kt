package com.supershade.ui.shade

import android.content.Intent
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.shade.pixel.PixelHeader
import com.supershade.ui.shade.pixel.PixelQuickSettingsGrid
import com.supershade.ui.shade.pixel.PixelBrightnessSlider
import com.supershade.ui.shade.pixel.PixelMediaCard
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
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
    var isEditingTiles by remember { mutableStateOf(false) }

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
                darkThemeMode = state.darkThemeMode,
                accentColor = state.accentColor,
                content = content,
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    val horizontalPanOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }.coerceAtLeast(1f)
    val dismissThresholdPx = with(density) { 96.dp.toPx() }
    val velocityThresholdPxPerSec = with(density) { 850.dp.toPx() }
    val maxHorizontalPanPx = with(density) { 60.dp.toPx() }

    var openTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var isSettled by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = if (state.activePanel == ShadePanel.QUICK_SETTINGS) 1 else 0,
        pageCount = { 2 }
    )

    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    LaunchedEffect(state.activePanel) {
        val targetPage = if (state.activePanel == ShadePanel.QUICK_SETTINGS) 1 else 0
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(
                targetPage,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            )
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetPanel = if (pagerState.currentPage == 1) ShadePanel.QUICK_SETTINGS else ShadePanel.NOTIFICATIONS
        if (state.activePanel != targetPanel) {
            haptics.sheetDetent()
            viewModel.setActivePanel(targetPanel)
        }
    }

    // Reset drag position and editing state whenever the shade re-opens or closes.
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            isSettled = false
            openTimeMs = System.currentTimeMillis()
            dragOffset.snapTo(0f)
            horizontalPanOffset.snapTo(0f)
            delay(400L)
            isSettled = true
        } else {
            isSettled = false
            isEditingTiles = false
        }
    }

    val isTogether = state.splitGestureMode.isTogether
    val isTucked = state.isQuickControlsTucked
    val activePanel = state.activePanel

    val nestedScrollConnection = remember(isTogether, isQsExpanded, isTucked, activePanel) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                // Swiping UP while on notifications panel and quick controls are untucked:
                // Tucks quick controls on deliberate swipe without swallowing scroll delta
                if (dy < -24f && activePanel == ShadePanel.NOTIFICATIONS && !isTucked && !isTogether) {
                    haptics.sheetDetent()
                    viewModel.setQuickControlsTucked(true)
                }
                // Swiping UP in Together mode while Quick Settings is expanded:
                if (dy < -12f && isTogether && isQsExpanded) {
                    haptics.sheetDetent()
                    viewModel.setQsExpanded(false)
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                // Pulled down at top of notifications list (available.y > 0):
                if (dy > 16f && activePanel == ShadePanel.NOTIFICATIONS && isTucked && !isTogether) {
                    haptics.sheetDetent()
                    viewModel.setQuickControlsTucked(false)
                    return Offset(0f, dy)
                }
                // Pulled down in Together mode: expand Quick Settings
                if (dy > 20f && isTogether && !isQsExpanded) {
                    haptics.sheetDetent()
                    viewModel.setQsExpanded(true)
                    return Offset(0f, dy)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (dragOffset.value < 0f) {
                    dragOffset.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                }
                return Velocity.Zero
            }
        }
    }

    val shapeScheme = remember(state.tileShape) {
        ShadeShapeScheme.fromTileShape(state.tileShape)
    }

    val backdropTheme = state.backdropTheme

    BackHandler(enabled = state.isOpen) {
        if (showPowerMenu) {
            haptics.sheetDetent()
            showPowerMenu = false
        } else if (state.activeTileDetail != null) {
            viewModel.closeTileDetail()
        } else if (isTogether && state.isQsExpanded) {
            haptics.sheetDetent()
            viewModel.setQsExpanded(false)
        } else {
            // Swallow back presses during the initial 350ms settle window to avoid
            // closing when system shade dismissal injects a back event.
            if (System.currentTimeMillis() - openTimeMs < 350L) return@BackHandler
            haptics.sheetDetent()
            coroutineScope.launch {
                dragOffset.animateTo(-screenHeightPx, tween(180))
                onDismiss()
            }
        }
    }

    CompositionLocalProvider(
        LocalCardBorderWidth provides state.cardBorderWidth,
        LocalShadeShapeScheme provides shapeScheme,
        LocalBackdropTheme provides backdropTheme,
    ) {
        themeWrapper {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dimmer scrim — tapping it dismisses the shade.
                // In AOSP/OneUI, scrim smoothly fades out in real time as the shade is pulled up.
                val opacity = state.backdropOpacity.coerceIn(0.20f, 1.00f)
                val baseScrimAlpha = when {
                    opacity >= 0.99f -> 0.70f
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> (opacity * 0.36f).coerceIn(0.12f, 0.45f)
                    else -> (opacity * 0.65f).coerceIn(0.25f, 0.75f)
                }
                val dragFraction = (kotlin.math.abs(dragOffset.value) / screenHeightPx).coerceIn(0f, 1f)
                // One UI 9 cosine scrim attenuation curve: preserves visual focus before wallpaper reveal
                val cosineFactor = kotlin.math.cos((Math.PI / 2.0) * Math.pow(dragFraction.toDouble(), 1.35)).toFloat()
                val liveScrimAlpha = (baseScrimAlpha * cosineFactor * cosineFactor).coerceIn(0f, 1f)
                val panelScale = (1f - 0.075f * Math.pow(dragFraction.toDouble(), 1.25).toFloat()).coerceIn(0.92f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = liveScrimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isSettled,
                            onClick = {
                                if (isSettled && dragOffset.value == 0f) {
                                    haptics.lightTap()
                                    onDismiss()
                                }
                            }
                        ),
                )

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
                    val isTogether = state.splitGestureMode.isTogether
                    val isCombined = isTogether ||
                                     state.splitGestureMode == SplitGestureMode.ALWAYS_NOTIFICATIONS ||
                                     state.splitGestureMode == SplitGestureMode.ALWAYS_QUICK_SETTINGS
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
                            .graphicsLayer {
                                scaleX = panelScale
                                scaleY = panelScale
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .background(glassBackdrop)
                            .pointerInput(isQsExpanded, isEditingTiles, isTogether, state.activePanel, state.visibleNotifications.size) {
                                if (isEditingTiles) return@pointerInput
                                val px = this.density
                                val edgeZonePx = (32f * px).toInt()
                                val headerZonePx = (80f * px).toInt()
                                val bottomZonePx = (72f * px).toInt()
                                val minSwipeUp = (96f * px).toInt()
                                val slopeMin = 0.45f

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    // Ignore touches that began during the initial 400ms settle window to avoid catching the opening swipe
                                    if (System.currentTimeMillis() - openTimeMs < 400L) {
                                        return@awaitEachGesture
                                    }
                                    var consumed = false
                                    var lastY = down.position.y
                                    var totalDy = 0f
                                    val tracker = VelocityTracker()
                                    tracker.addPosition(down.uptimeMillis, down.position)

                                    val hasNotifications = state.visibleNotifications.isNotEmpty()
                                    // Touch is over the scrollable feed if it occurs between top header and bottom handle zones
                                    // and there are notifications to scroll (or together mode with compact QS).
                                    val isTouchOverScrollableFeed = down.position.y >= headerZonePx &&
                                                                    down.position.y <= (size.height - bottomZonePx) &&
                                                                    (hasNotifications || (isTogether && !isQsExpanded))

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        tracker.addPosition(change.uptimeMillis, change.position)

                                        val dx = change.position.x - down.position.x
                                        val dy = change.position.y - down.position.y
                                        val deltaY = change.position.y - lastY
                                        lastY = change.position.y

                                        // Side edge inward swipe (AOSP / One UI predictive back navigation)
                                        val isInwardSwipe = (down.position.x < edgeZonePx && dx > (22f * px)) ||
                                                            (down.position.x > (size.width - edgeZonePx) && dx < -(22f * px))

                                        if (!consumed && isInwardSwipe) {
                                            change.consume()
                                            consumed = true
                                            haptics.sheetDetent()
                                            if (isTogether && isQsExpanded) {
                                                coroutineScope.launch { viewModel.setQsExpanded(false) }
                                            } else {
                                                coroutineScope.launch {
                                                    dragOffset.animateTo(-screenHeightPx, tween(180))
                                                    onDismiss()
                                                }
                                            }
                                            break
                                        }

                                        // Vertical dismiss drag tracking:
                                        // ONLY active when the touch started outside scrollable notification feed
                                        // (e.g. Header zone, Bottom handle zone, or Empty notifications state)
                                        if (!isTouchOverScrollableFeed) {
                                            val isDominantVertical = kotlin.math.abs(dy) > kotlin.math.abs(dx) * slopeMin
                                            if (isDominantVertical && (dy < -6f || dy > 6f || dragOffset.value != 0f)) {
                                                totalDy = dy
                                                val currentVal = dragOffset.value
                                                val nextVal = if (deltaY > 0f && currentVal >= 0f) {
                                                    // Downward rubber-banding when pulled past resting position
                                                    (currentVal + deltaY * 0.32f).coerceIn(0f, 64f * px)
                                                } else {
                                                    // Live 1:1 tracking upward towards dismiss or returning down
                                                    (currentVal + deltaY).coerceAtMost(64f * px)
                                                }
                                                coroutineScope.launch {
                                                    dragOffset.snapTo(nextVal)
                                                }
                                            }

                                            if (!change.pressed) {
                                                val velocity = tracker.calculateVelocity().y
                                                val isFlingUp = velocity < -velocityThresholdPxPerSec && totalDy < -(32f * px)
                                                val isPulledPastThreshold = dragOffset.value < -dismissThresholdPx || -totalDy >= minSwipeUp

                                                if (isFlingUp || isPulledPastThreshold) {
                                                    consumed = true
                                                    change.consume()
                                                    if (isTogether && isQsExpanded && velocity > -1600f * px && dragOffset.value > -screenHeightPx * 0.35f) {
                                                        haptics.sheetDetent()
                                                        coroutineScope.launch {
                                                            viewModel.setQsExpanded(false)
                                                            dragOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                                                        }
                                                    } else {
                                                        haptics.sheetDetent()
                                                        coroutineScope.launch {
                                                            dragOffset.animateTo(-screenHeightPx, spring(0.90f, 420f))
                                                            onDismiss()
                                                        }
                                                    }
                                                } else if (dragOffset.value > 0f) {
                                                    coroutineScope.launch {
                                                        dragOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                                                    }
                                                } else if (dragOffset.value < 0f) {
                                                    coroutineScope.launch {
                                                        dragOffset.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
                                                    }
                                                }
                                                break
                                            }
                                        } else {
                                            // When touching inside scrollable feed, allow child views to handle scroll entirely
                                            if (!change.pressed) {
                                                break
                                            }
                                        }
                                    }
                                }
                            },
                    )
 {
                        if (backdropTheme == BackdropTheme.LIQUID_GLASS) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.12f),
                                                Color.White.copy(alpha = 0.03f),
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                                                Color.Transparent,
                                            ),
                                            start = Offset.Zero,
                                            end = Offset(1200f, 2200f),
                                        )
                                    ),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .displayCutoutPadding()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                        ) {
                        // Top Status Bar (Clock, Battery, Lock, Settings, Power, Edit)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val onOpenEditAction: () -> Unit = {
                                isEditingTiles = !isEditingTiles
                                if (isEditingTiles) {
                                    viewModel.setQsExpanded(true)
                                    if (state.activePanel != ShadePanel.QUICK_SETTINGS) {
                                        viewModel.setActivePanel(ShadePanel.QUICK_SETTINGS)
                                    }
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                }
                            }

                            if (state.theme == ShadeTheme.Pixel) {
                                PixelHeader(
                                    statusBar = state.statusBar,
                                    isEditing = isEditingTiles,
                                    onOpenPowerMenu = { showPowerMenu = true },
                                    onOpenEdit = onOpenEditAction,
                                    onOpenDeviceSettings = { onDismiss() },
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
                            } else {
                                StatusBarRow(
                                    statusBar = state.statusBar,
                                    isEditing = isEditingTiles,
                                    onOpenPowerMenu = { showPowerMenu = true },
                                    onOpenEdit = onOpenEditAction,
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
                            }
                        }

                        // Active View Content
                        // TOGETHER mode: single unified vertical feed (One UI 8/9 style)
                        // Separate modes: fluid horizontal panel slide
                        if (isTogether) {
                            // ── TOGETHER MODE: QS tiles on top, notifications below, one scrollable feed ──
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .nestedScroll(nestedScrollConnection)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (state.theme == ShadeTheme.Pixel) {
                                    // ── Pixel Material Expressive Layout in Together Mode ──
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .draggable(
                                                orientation = Orientation.Vertical,
                                                enabled = !isEditingTiles,
                                                state = rememberDraggableState { delta ->
                                                    if (delta > 10f && !isQsExpanded) {
                                                        haptics.sheetDetent()
                                                        viewModel.setQsExpanded(true)
                                                    } else if (delta < -10f && isQsExpanded) {
                                                        haptics.sheetDetent()
                                                        viewModel.setQsExpanded(false)
                                                    }
                                                }
                                            )
                                    ) {
                                        PixelQuickSettingsGrid(
                                            tiles = state.tiles,
                                            isExpanded = isQsExpanded,
                                            isEditing = isEditingTiles,
                                            onToggleEdit = { isEditingTiles = !isEditingTiles },
                                            onMoveTile = { from, to -> viewModel.moveTile(from, to) },
                                            onRemoveTile = { viewModel.removeTile(it) },
                                            onAddTile = { viewModel.addTile(it) },
                                            onResetTiles = { viewModel.resetTiles() },
                                            onTileClick = { viewModel.toggleTile(it) },
                                            onTileLongClick = { viewModel.openTileDetail(it) },
                                        )
                                    }

                                    PixelBrightnessSlider(
                                        brightness = state.brightness,
                                        onBrightnessChange = { viewModel.setBrightness(it) },
                                    )

                                    state.media?.let { media ->
                                        PixelMediaCard(
                                            media = media,
                                            onPlayPause = { viewModel.mediaPlayPause() },
                                            onSkipNext = { viewModel.mediaSkipNext() },
                                            onSkipPrevious = { viewModel.mediaSkipPrevious() },
                                            onSeek = { viewModel.mediaSeek(it) },
                                        )
                                    }
                                } else {
                                    // ── One UI Layout in Together Mode ──
                                    // Compact QS row (always visible at top, draggable to expand/collapse with spring physics)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .draggable(
                                                orientation = Orientation.Vertical,
                                                enabled = !isEditingTiles,
                                                state = rememberDraggableState { delta ->
                                                    if (delta > 10f && !isQsExpanded) {
                                                        haptics.sheetDetent()
                                                        viewModel.setQsExpanded(true)
                                                    } else if (delta < -10f && isQsExpanded) {
                                                        haptics.sheetDetent()
                                                        viewModel.setQsExpanded(false)
                                                    }
                                                }
                                            )
                                    ) {
                                        QuickSettingsGrid(
                                            tiles = state.tiles,
                                            theme = state.theme,
                                            isShizukuConnected = state.isShizukuConnected,
                                            isExpanded = isQsExpanded,
                                            tileShape = state.tileShape,
                                            tileSize = state.tileSize,
                                            tileColumns = state.tileColumns,
                                            showWideCards = isQsExpanded && state.showWideCards,
                                            isEditing = isEditingTiles,
                                            onToggleEdit = { isEditingTiles = !isEditingTiles },
                                            onMoveTile = { from, to -> viewModel.moveTile(from, to) },
                                            onRemoveTile = { viewModel.removeTile(it) },
                                            onAddTile = { viewModel.addTile(it) },
                                            onResetTiles = { viewModel.resetTiles() },
                                            onTileClick = { viewModel.toggleTile(it) },
                                            onTileLongClick = { viewModel.openTileDetail(it) },
                                        )
                                    }

                                    // Tactile Sliders Island in Together mode:
                                    // Brightness slider is always accessible at top (matching One UI compact quick panel);
                                    // Volume slider expands smoothly when QS is expanded.
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
                                            AnimatedVisibility(
                                                visible = isQsExpanded,
                                                enter = expandVertically(spring(0.8f, 380f)) + fadeIn(tween(140)),
                                                exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
                                            ) {
                                                VolumeSlider(
                                                    compact = false,
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                            }
                                        }
                                    }

                                    // Media card
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

                                // Category bar + notification feed (inline, not in a nested LazyColumn
                                // since we're inside verticalScroll — so we expand the list inline)
                                if (state.allNotifications.isNotEmpty()) {
                                    CategoryBar(
                                        categories = ShadeCategory.entries,
                                        selected = state.selectedCategory,
                                        onSelect = { viewModel.selectCategory(it) },
                                        counts = categoryCounts,
                                    )
                                }

                                // In TOGETHER mode we use a non-lazy feed to stay inside the outer scroll
                                AnimatedContent(
                                    targetState = state.selectedCategory,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(180)) + slideInHorizontally(
                                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                                            initialOffsetX = { fullWidth -> if (targetState.ordinal > initialState.ordinal) fullWidth / 4 else -fullWidth / 4 }
                                        )).togetherWith(
                                            fadeOut(animationSpec = tween(140)) + slideOutHorizontally(
                                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                                                targetOffsetX = { fullWidth -> if (targetState.ordinal > initialState.ordinal) -fullWidth / 4 else fullWidth / 4 }
                                            )
                                        )
                                    },
                                    label = "togetherCategoryTransition",
                                ) { _ ->
                                    TogetherNotificationFeed(
                                        notifications = state.visibleNotifications,
                                        onDismiss = { viewModel.dismissNotification(it) },
                                        onClearAll = { viewModel.clearAllNotifications() },
                                        onNotificationClick = { notification ->
                                            viewModel.launchNotification(notification)
                                            onDismiss()
                                        },
                                        onSnooze = { key, delayMs -> viewModel.snoozeNotification(key, delayMs) },
                                    )
                                }
                            }
                        } else {

                        // Separate/Combined panels: fluid continuous horizontal page tracking (One UI 9 style)
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            userScrollEnabled = !isEditingTiles,
                            beyondViewportPageCount = 1,
                        ) { page ->
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            val clampedOffset = pageOffset.coerceIn(-1f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = 1f - kotlin.math.abs(clampedOffset) * 0.20f
                                        scaleX = 1f - kotlin.math.abs(clampedOffset) * 0.025f
                                        scaleY = 1f - kotlin.math.abs(clampedOffset) * 0.025f
                                    }
                            ) {
                                if (page == 0) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                    // Quick Controls section on Notifications panel
                                    AnimatedVisibility(
                                        visible = !state.isQuickControlsTucked,
                                        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(tween(150)),
                                        exit = shrinkVertically(tween(180)) + fadeOut(tween(150)),
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            if (state.theme == ShadeTheme.Pixel) {
                                                PixelQuickSettingsGrid(
                                                    tiles = state.tiles,
                                                    isExpanded = false,
                                                    isEditing = false,
                                                    onToggleEdit = {},
                                                    onMoveTile = { _, _ -> },
                                                    onRemoveTile = {},
                                                    onAddTile = {},
                                                    onResetTiles = {},
                                                    onTileClick = { viewModel.toggleTile(it) },
                                                    onTileLongClick = { viewModel.openTileDetail(it) },
                                                )

                                                PixelBrightnessSlider(
                                                    brightness = state.brightness,
                                                    onBrightnessChange = { viewModel.setBrightness(it) },
                                                )
                                            } else {
                                                QuickSettingsGrid(
                                                    tiles = state.tiles,
                                                    theme = state.theme,
                                                    isShizukuConnected = state.isShizukuConnected,
                                                    isExpanded = false,
                                                    tileShape = state.tileShape,
                                                    tileSize = state.tileSize,
                                                    tileColumns = state.tileColumns,
                                                    showWideCards = state.showWideCards,
                                                    isEditing = false,
                                                    onToggleEdit = {},
                                                    onMoveTile = { _, _ -> },
                                                    onRemoveTile = {},
                                                    onAddTile = {},
                                                    onResetTiles = {},
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
                                        if (state.theme == ShadeTheme.Pixel) {
                                            PixelMediaCard(
                                                media = media,
                                                onPlayPause = { viewModel.mediaPlayPause() },
                                                onSkipNext = { viewModel.mediaSkipNext() },
                                                onSkipPrevious = { viewModel.mediaSkipPrevious() },
                                                onSeek = { viewModel.mediaSeek(it) },
                                            )
                                        } else {
                                            MediaCard(
                                                media = media,
                                                onPlayPause = { viewModel.mediaPlayPause() },
                                                onSkipNext = { viewModel.mediaSkipNext() },
                                                onSkipPrevious = { viewModel.mediaSkipPrevious() },
                                                onSeek = { viewModel.mediaSeek(it) },
                                            )
                                        }
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
                                    AnimatedContent(
                                        targetState = state.selectedCategory,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(180)) + slideInHorizontally(
                                                animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                                                initialOffsetX = { fullWidth -> if (targetState.ordinal > initialState.ordinal) fullWidth / 4 else -fullWidth / 4 }
                                            )).togetherWith(
                                                fadeOut(animationSpec = tween(140)) + slideOutHorizontally(
                                                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f),
                                                    targetOffsetX = { fullWidth -> if (targetState.ordinal > initialState.ordinal) -fullWidth / 4 else fullWidth / 4 }
                                                )
                                            )
                                        },
                                        label = "separateCategoryTransition",
                                        modifier = Modifier
                                            .weight(1f)
                                            .nestedScroll(nestedScrollConnection),
                                    ) { _ ->
                                        NotificationFeed(
                                            notifications = state.visibleNotifications,
                                            onDismiss = { viewModel.dismissNotification(it) },
                                            onClearAll = { viewModel.clearAllNotifications() },
                                            onNotificationClick = { notification ->
                                                viewModel.launchNotification(notification)
                                                onDismiss()
                                            },
                                            onSnooze = { key, delayMs -> viewModel.snoozeNotification(key, delayMs) },
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            } else {
                                // QUICK SETTINGS PANEL (Full Control Center)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(nestedScrollConnection)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (state.theme == ShadeTheme.Pixel) {
                                        PixelQuickSettingsGrid(
                                            tiles = state.tiles,
                                            isExpanded = true,
                                            isEditing = isEditingTiles,
                                            onToggleEdit = { isEditingTiles = !isEditingTiles },
                                            onMoveTile = { from, to -> viewModel.moveTile(from, to) },
                                            onRemoveTile = { viewModel.removeTile(it) },
                                            onAddTile = { viewModel.addTile(it) },
                                            onResetTiles = { viewModel.resetTiles() },
                                            onTileClick = { viewModel.toggleTile(it) },
                                            onTileLongClick = { viewModel.openTileDetail(it) },
                                        )

                                        PixelBrightnessSlider(
                                            brightness = state.brightness,
                                            onBrightnessChange = { viewModel.setBrightness(it) },
                                        )

                                        state.media?.let { media ->
                                            PixelMediaCard(
                                                media = media,
                                                onPlayPause = { viewModel.mediaPlayPause() },
                                                onSkipNext = { viewModel.mediaSkipNext() },
                                                onSkipPrevious = { viewModel.mediaSkipPrevious() },
                                                onSeek = { viewModel.mediaSeek(it) },
                                            )
                                        }
                                    } else {
                                        QuickSettingsGrid(
                                            tiles = state.tiles,
                                            theme = state.theme,
                                            isShizukuConnected = state.isShizukuConnected,
                                            isExpanded = true,
                                            tileShape = state.tileShape,
                                            tileSize = state.tileSize,
                                            tileColumns = state.tileColumns,
                                            showWideCards = state.showWideCards,
                                            isEditing = isEditingTiles,
                                            onToggleEdit = { isEditingTiles = !isEditingTiles },
                                            onMoveTile = { from, to -> viewModel.moveTile(from, to) },
                                            onRemoveTile = { viewModel.removeTile(it) },
                                            onAddTile = { viewModel.addTile(it) },
                                            onResetTiles = { viewModel.resetTiles() },
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
                            }
                        }
                    }
                } // end else isTogether

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
                                .defaultMinSize(minHeight = 52.dp)
                                .clickable {
                                    haptics.sheetDetent()
                                    coroutineScope.launch {
                                        dragOffset.animateTo(-screenHeightPx, tween(180))
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 10.dp)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    enabled = isSettled && !isEditingTiles,
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
                                                (velocity < -velocityThresholdPxPerSec && dragOffset.value < -(16f * density.density))
                                            ) {
                                                dragOffset.animateTo(
                                                    targetValue = -screenHeightPx,
                                                    animationSpec = tween(durationMillis = 180),
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
            }

            // In-shade Tile Detail Sheet (Flashlight multi-level control, Wi-Fi details, Bluetooth devices)
            AnimatedVisibility(
                visible = state.activeTileDetail != null,
                enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = 420f)) { it } + fadeIn(tween(160)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(140)),
            ) {
                state.activeTileDetail?.let { detail ->
                    QuickTileDetailSheet(
                        detailState = detail,
                        onDismiss = { viewModel.closeTileDetail() },
                        onSetTorchStrength = { viewModel.setTorchStrength(it) },
                        onToggleTorch = { viewModel.toggleTorchInDetail() },
                    )
                }
            }

            // Quick Power Menu Dialog (Overlaid on top of shade panel)
            AnimatedVisibility(
                visible = showPowerMenu,
                enter = fadeIn(tween(160)) + scaleIn(spring(dampingRatio = 0.82f, stiffness = 420f), initialScale = 0.92f),
                exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.92f),
            ) {
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
        }
    }
}
