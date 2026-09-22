package com.supershade.ui.shade.pixel

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.tile.KNOWN_TILES
import com.supershade.domain.tile.TileDefinition
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.shade.tileIcon
import com.supershade.ui.theme.getCardBorder
import kotlin.math.roundToInt

/**
 * Pixel (Material You / Expressive) Quick Settings Grid.
 * Displays 2 columns of wide stadium pills with smooth spring transitions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelQuickSettingsGrid(
    tiles: List<TileDefinition>,
    isExpanded: Boolean = false,
    isEditing: Boolean = false,
    onToggleEdit: () -> Unit = {},
    onMoveTile: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRemoveTile: (String) -> Unit = {},
    onAddTile: (String) -> Unit = {},
    onResetTiles: () -> Unit = {},
    onTileClick: (TileDefinition) -> Unit,
    onTileLongClick: ((TileDefinition) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val displayedTiles = when {
        isEditing -> tiles
        isExpanded -> tiles.take(8)
        else -> tiles.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .animateContentSize(
                animationSpec = spring<IntSize>(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Edit mode header
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
                        text = "Edit tiles",
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

        // Tile Grid: 2 columns
        if (isEditing) {
            DraggablePixelTileGrid(
                tiles = displayedTiles,
                haptics = haptics,
                onMoveTile = onMoveTile,
                onRemoveTile = onRemoveTile,
            )
        } else {
            displayedTiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowTiles.forEach { tile ->
                        Box(modifier = Modifier.weight(1f)) {
                            PixelTilePill(
                                tile = tile,
                                onClick = { onTileClick(tile) },
                                onLongClick = onTileLongClick?.let { cb -> { cb(tile) } },
                                isEditing = false,
                            )
                        }
                    }
                    if (rowTiles.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Available tiles drawer during edit
        if (isEditing) {
            val availableTileIds = remember(tiles) {
                val currentIds = tiles.map { it.id }.toSet()
                KNOWN_TILES.keys.filter { it !in currentIds }
            }
            if (availableTileIds.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
                Text(
                    text = "Available tiles (tap + to add)",
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

@Composable
private fun DraggablePixelTileGrid(
    tiles: List<TileDefinition>,
    haptics: SuperHaptics,
    onMoveTile: (Int, Int) -> Unit,
    onRemoveTile: (String) -> Unit,
) {
    val tileRects = remember { mutableMapOf<Int, Rect>() }
    var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var dragIndex by remember { mutableIntStateOf(-1) }
    var targetIndex by remember { mutableIntStateOf(-1) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }

    val hapticFeedback = LocalHapticFeedback.current
    val lastTargetRef = remember { mutableIntStateOf(-1) }

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
            .onGloballyPositioned { coords ->
                gridCoordinates = coords
            }
            .pointerInput(tiles.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPos = down.position

                    val pressedIndex = tileRects.entries.firstOrNull { (_, rect) ->
                        rect.contains(downPos)
                    }?.key ?: return@awaitEachGesture

                    var isLongPress = false
                    var elapsedMs = 0L
                    val longPressMs = viewConfiguration.longPressTimeoutMillis

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        elapsedMs += 16L
                        if (!change.pressed) break

                        if (!isLongPress && elapsedMs >= longPressMs) {
                            isLongPress = true
                            dragIndex = pressedIndex
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

                    if (isLongPress && dragIndex >= 0 && targetIndex >= 0 && dragIndex != targetIndex) {
                        haptics.tileToggleOn()
                        onMoveTile(dragIndex, targetIndex)
                    }
                    dragIndex = -1
                    targetIndex = -1
                }
            },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            previewTiles.chunked(2).forEachIndexed { rowIdx, rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowTiles.forEachIndexed { colIdx, tile ->
                        val globalIndex = rowIdx * 2 + colIdx
                        val isDragging = dragIndex >= 0 && tiles.getOrNull(dragIndex) == tile

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    val grid = gridCoordinates
                                    val pos = if (grid != null && grid.isAttached && coords.isAttached) {
                                        grid.localPositionOf(coords, Offset.Zero)
                                    } else {
                                        coords.positionInParent()
                                    }
                                    tileRects[globalIndex] = Rect(
                                        offset = pos,
                                        size = Size(coords.size.width.toFloat(), coords.size.height.toFloat()),
                                    )
                                }
                                .graphicsLayer {
                                    alpha = if (dragIndex >= 0 && isDragging) 0.35f else 1f
                                },
                        ) {
                            PixelTilePill(
                                tile = tile,
                                onClick = {},
                                onLongClick = null,
                                isEditing = true,
                                isDragging = isDragging,
                                onRemove = { onRemoveTile(tile.id) },
                            )
                        }
                    }
                    if (rowTiles.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Floating ghost tile during drag
        if (dragIndex >= 0 && dragIndex < previewTiles.size) {
            val ghostTile = tiles.getOrNull(dragIndex)
            val ghostRect = tileRects[dragIndex]
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
                        .size(with(androidx.compose.ui.platform.LocalDensity.current) { ghostW.toDp() }, with(androidx.compose.ui.platform.LocalDensity.current) { ghostH.toDp() })
                        .graphicsLayer {
                            scaleX = 1.08f
                            scaleY = 1.08f
                            shadowElevation = 24f
                        },
                ) {
                    PixelTilePill(
                        tile = ghostTile,
                        onClick = {},
                        onLongClick = null,
                        isEditing = true,
                        isDragging = true,
                        onRemove = null,
                    )
                }
            }
        }
    }
}
