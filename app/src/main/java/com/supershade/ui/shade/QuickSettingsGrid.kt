package com.supershade.ui.shade

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.BorderStroke
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.LocalBackdropTheme
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.domain.tile.TileDefinition
import com.supershade.settings.TileGridColumns
import com.supershade.settings.TileShape
import com.supershade.settings.TileSize
import com.supershade.ui.theme.ShadeTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import com.supershade.domain.tile.KNOWN_TILES
import kotlin.math.roundToInt

/**
 * Quick settings grid supporting compact mode (1 row, dynamic columns)
 * and expanded mode enclosed in a modern One UI 8 island container.
 * In expanded mode, optional prominent dual connectivity pills (Wi-Fi & Bluetooth)
 * sit at the top of the island matching Samsung One UI 8.
 * In edit mode: freehand drag-and-drop reorder (like home screen), no arrows.
 * Edit mode only entered via the Edit pencil button — NOT by long press.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickSettingsGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    isExpanded: Boolean = false,
    tileShape: TileShape = TileShape.SQUIRCLE,
    tileSize: TileSize = TileSize.STANDARD,
    tileColumns: TileGridColumns = TileGridColumns.STANDARD,
    showWideCards: Boolean = true,
    isEditing: Boolean = false,
    onToggleEdit: () -> Unit = {},
    onMoveTile: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRemoveTile: (String) -> Unit = {},
    onAddTile: (String) -> Unit = {},
    onResetTiles: () -> Unit = {},
    onTileClick: (TileDefinition) -> Unit,
    onTileLongClick: ((TileDefinition) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val colCount = tileColumns.count
    val showOneUiIslandCards = !isEditing && isExpanded && showWideCards
    val wifiTile = if (showOneUiIslandCards) tiles.firstOrNull { it.id == "wifi" || it.id == "internet" } else null
    val btTile   = if (showOneUiIslandCards) tiles.firstOrNull { it.id == "bt"   || it.id == "bluetooth" } else null
    val hasWideCards = wifiTile != null && btTile != null

    val displayedTiles = when {
        isEditing    -> tiles
        hasWideCards -> tiles.filter { it != wifiTile && it != btTile }.take(colCount * 2)
        isExpanded   -> tiles.take(colCount * 3)
        else         -> tiles.take(colCount)
    }

    Surface(
        shape = LocalShadeShapeScheme.current.container,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
        border = getCardBorder(alpha = 0.30f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring<IntSize>(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Edit mode header ───────────────────────────────────────────────
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Edit buttons",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Drag to reorder",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextButton(
                            onClick = {
                                haptics.tileToggleOff()
                                onResetTiles()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }
                        Surface(
                            onClick = {
                                haptics.tileToggleOn()
                                onToggleEdit()
                            },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                }
            }

            // ── Wide connectivity cards (Wi-Fi / BT) ──────────────────────────
            if (!isEditing && wifiTile != null && btTile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ConnectivityWideCard(
                        tile = wifiTile,
                        theme = theme,
                        tileShape = tileShape,
                        onClick = { onTileClick(wifiTile) },
                        onLongClick = onTileLongClick?.let { cb -> { cb(wifiTile) } },
                        modifier = Modifier.weight(1f),
                    )
                    ConnectivityWideCard(
                        tile = btTile,
                        theme = theme,
                        tileShape = tileShape,
                        onClick = { onTileClick(btTile) },
                        onLongClick = onTileLongClick?.let { cb -> { cb(btTile) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Tile grid — normal or drag-to-reorder ─────────────────────────
            if (isEditing) {
                DraggableTileGrid(
                    tiles = displayedTiles,
                    theme = theme,
                    tileShape = tileShape,
                    tileSize = tileSize,
                    colCount = colCount,
                    haptics = haptics,
                    onMoveTile = onMoveTile,
                    onRemoveTile = onRemoveTile,
                )
            } else {
                displayedTiles.chunked(colCount).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (colCount >= 5) 6.dp else 8.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            Box(modifier = Modifier.weight(1f)) {
                                TileCard(
                                    tile = tile,
                                    theme = theme,
                                    isShizukuConnected = isShizukuConnected,
                                    tileShape = tileShape,
                                    tileSize = tileSize,
                                    columns = colCount,
                                    onClick = { onTileClick(tile) },
                                    onLongClick = onTileLongClick?.let { cb -> { cb(tile) } },
                                    isEditing = false,
                                )
                            }
                        }
                        repeat(colCount - rowTiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Available Buttons drawer ───────────────────────────────────────
            if (isEditing) {
                val availableTileIds = remember(tiles) {
                    val currentIds = tiles.map { it.id }.toSet()
                    KNOWN_TILES.keys.filter { it !in currentIds }
                }
                if (availableTileIds.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                    Text(
                        text = "Available buttons (tap + to add)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        availableTileIds.forEach { tileId ->
                            val label = KNOWN_TILES[tileId]?.first ?: tileId
                            Surface(
                                onClick = {
                                    haptics.tileToggleOn()
                                    onAddTile(tileId)
                                },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = getCardBorder(alpha = 0.35f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add $label",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Icon(
                                        imageVector = tileIcon(tileId, false, null),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Freehand drag-to-reorder grid for tile editing.
 * Works like home screen app reordering:
 * - Press and hold a tile → it lifts and follows your finger
 * - Drag over another tile slot → that tile shifts to make room (preview swap)
 * - Release → swap commits via [onMoveTile]
 *
 * No arrow buttons. No sliders.
 */
@Composable
private fun DraggableTileGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    tileShape: TileShape,
    tileSize: TileSize,
    colCount: Int,
    haptics: SuperHaptics,
    onMoveTile: (Int, Int) -> Unit,
    onRemoveTile: (String) -> Unit,
) {
    // Track each tile's position in the grid so we can hit-test during drag
    val tileRects = remember { mutableMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    val density = LocalDensity.current.density

    // Drag state
    var dragIndex   by remember { mutableIntStateOf(-1) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    // Finger offset relative to the grid Box origin
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val hapticFeedback = LocalHapticFeedback.current
    val lastTargetRef  = remember { mutableIntStateOf(-1) }

    // Build a reordered preview list while dragging
    val previewTiles: List<TileDefinition> = remember(tiles, dragIndex, targetIndex) {
        if (dragIndex < 0 || targetIndex < 0 || dragIndex == targetIndex) {
            tiles
        } else {
            val mutable = tiles.toMutableList()
            val moved = mutable.removeAt(dragIndex)
            mutable.add(targetIndex, moved)
            mutable
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(tiles.size, colCount) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position

                    // Find which tile was pressed
                    val pressedIndex = tileRects.entries.firstOrNull { (_, rect) ->
                        rect.contains(downPos)
                    }?.key ?: return@awaitEachGesture

                    // Wait briefly for long-press threshold (like home screen)
                    var isLongPress = false
                    var elapsedMs   = 0L
                    val longPressMs = viewConfiguration.longPressTimeoutMillis

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val moved = (change.position - downPos).getDistance()

                        elapsedMs += 16L // approximate frame time

                        if (!change.pressed) {
                            // Released before long-press — not a drag
                            break
                        }

                        if (!isLongPress && elapsedMs >= longPressMs) {
                            // Long press threshold reached — start drag
                            isLongPress = true
                            dragIndex   = pressedIndex
                            targetIndex = pressedIndex
                            dragX = change.position.x
                            dragY = change.position.y
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            change.consume()
                        }

                        if (isLongPress) {
                            change.consume()
                            dragX = change.position.x
                            dragY = change.position.y

                            // Hit-test to find which slot the finger is over
                            val hovered = tileRects.entries.minByOrNull { (_, rect) ->
                                val cx = rect.center.x
                                val cy = rect.center.y
                                val dx = cx - change.position.x
                                val dy = cy - change.position.y
                                dx * dx + dy * dy
                            }?.key ?: -1

                            if (hovered >= 0 && hovered != targetIndex) {
                                targetIndex = hovered
                                if (lastTargetRef.intValue != hovered) {
                                    lastTargetRef.intValue = hovered
                                    haptics.lightTap()
                                }
                            }
                        }
                    }

                    // Commit the swap
                    if (isLongPress && dragIndex >= 0 && targetIndex >= 0 && dragIndex != targetIndex) {
                        haptics.tileToggleOn()
                        onMoveTile(dragIndex, targetIndex)
                    }
                    dragIndex   = -1
                    targetIndex = -1
                }
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            previewTiles.chunked(colCount).forEachIndexed { rowIdx, rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (colCount >= 5) 6.dp else 8.dp),
                ) {
                    rowTiles.forEachIndexed { colIdx, tile ->
                        val globalIndex = rowIdx * colCount + colIdx
                        // Actual index in the original tiles list (for remove)
                        val originalIndex = tiles.indexOf(tile)
                        val isDragging = dragIndex >= 0 && tiles.getOrNull(dragIndex) == tile

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    val pos  = coords.positionInParent()
                                    val size = coords.size
                                    tileRects[globalIndex] = androidx.compose.ui.geometry.Rect(
                                        offset = pos,
                                        size   = androidx.compose.ui.geometry.Size(
                                            size.width.toFloat(),
                                            size.height.toFloat(),
                                        ),
                                    )
                                }
                                // Fade non-dragged tiles slightly when drag is active
                                .graphicsLayer {
                                    alpha = if (dragIndex >= 0 && isDragging) 0.35f else 1f
                                },
                        ) {
                            TileCard(
                                tile = tile,
                                theme = theme,
                                isShizukuConnected = false,
                                tileShape = tileShape,
                                tileSize = tileSize,
                                columns = colCount,
                                onClick = {},
                                onLongClick = null,
                                isEditing = true,
                                isDragging = isDragging,
                                onRemove = if (originalIndex >= 0) {
                                    { onRemoveTile(tile.id) }
                                } else null,
                            )
                        }
                    }
                    repeat(colCount - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Floating ghost tile that follows the finger ───────────────────────
        if (dragIndex >= 0 && dragIndex < previewTiles.size) {
            val ghostTile   = tiles.getOrNull(dragIndex)
            val ghostRect   = tileRects[dragIndex]
            if (ghostTile != null && ghostRect != null) {
                val ghostW = ghostRect.width
                val ghostH = ghostRect.height
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (dragX - ghostW / 2).roundToInt(),
                                y = (dragY - ghostH / 2).roundToInt(),
                            )
                        }
                        .size(
                            width  = (ghostW / density).dp,
                            height = (ghostH / density).dp,
                        )
                        .graphicsLayer {
                            scaleX = 1.12f
                            scaleY = 1.12f
                            shadowElevation = 32f
                            alpha = 0.95f
                        },
                ) {
                    TileCard(
                        tile = ghostTile,
                        theme = theme,
                        isShizukuConnected = false,
                        tileShape = tileShape,
                        tileSize = tileSize,
                        columns = colCount,
                        onClick = {},
                        onLongClick = null,
                        isEditing = false,
                        isDragging = true,
                        onRemove = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectivityWideCard(
    tile: TileDefinition,
    theme: ShadeTheme,
    tileShape: TileShape = TileShape.SQUIRCLE,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "connScale",
    )

    val containerColor by animateColorAsState(
        targetValue = if (tile.isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "connContainer",
    )

    val contentColor by animateColorAsState(
        targetValue = if (tile.isActive)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "connContent",
    )

    val borderStroke = if (tile.isActive) null else getCardBorder(alpha = 0.40f)

    val shapeScheme = LocalShadeShapeScheme.current
    val wideCardShape = when {
        theme is ShadeTheme.Pixel -> CircleShape
        else -> shapeScheme.tile
    }

    Surface(
        shape = wideCardShape,
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .height(62.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        val stateDesc = tile.subtitle ?: if (tile.isActive) "Connected" else "Off"
        Box(modifier = Modifier.fillMaxSize()) {
            if (tile.isActive && LocalBackdropTheme.current == BackdropTheme.LIQUID_GLASS) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent,
                                ),
                                start = Offset.Zero,
                                end = Offset(300f, 200f),
                            )
                        )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics(mergeDescendants = true) {
                        role = Role.Switch
                        contentDescription = tile.label
                        stateDescription = stateDesc
                        tile.settingsAction?.let { action ->
                            customActions = listOf(
                                CustomAccessibilityAction("Open settings") {
                                    try {
                                        context.startActivity(
                                            Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                        )
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                            )
                        }
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = {
                            if (!tile.isActive) haptics.tileToggleOn() else haptics.tileToggleOff()
                            onClick()
                        },
                        onLongClick = if (onLongClick != null || tile.settingsAction != null) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (onLongClick != null) {
                                    onLongClick()
                                } else {
                                    tile.settingsAction?.let { action ->
                                        try {
                                            context.startActivity(
                                                Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                            )
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        } else null,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (tile.isActive)
                                Color.White.copy(alpha = 0.22f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                ) {
                    Icon(
                        imageVector = if (tile.id.contains("bt") || tile.id.contains("bluetooth"))
                            Icons.Default.Bluetooth
                        else
                            Icons.Default.Wifi,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = tile.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tile.subtitle ?: if (tile.isActive) "Connected" else "Off",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = contentColor.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.40f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
