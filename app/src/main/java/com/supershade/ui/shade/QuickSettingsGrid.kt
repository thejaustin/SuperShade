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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Quick settings grid supporting compact mode (1 row, dynamic columns)
 * and expanded mode enclosed in a modern One UI 8 island container.
 * In expanded mode, optional prominent dual connectivity pills (Wi-Fi & Bluetooth)
 * sit at the top of the island matching Samsung One UI 8.
 */
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
    onTileClick: (TileDefinition) -> Unit,
    onTileLongClick: ((TileDefinition) -> Unit)? = null,
) {
    val colCount = tileColumns.count
    val showOneUiIslandCards = isExpanded && showWideCards
    val wifiTile = if (showOneUiIslandCards) tiles.firstOrNull { it.id == "wifi" || it.id == "internet" } else null
    val btTile = if (showOneUiIslandCards) tiles.firstOrNull { it.id == "bt" || it.id == "bluetooth" } else null
    val hasWideCards = wifiTile != null && btTile != null

    val displayedTiles = when {
        hasWideCards -> tiles.filter { it != wifiTile && it != btTile }.take(colCount * 2)
        isExpanded -> tiles.take(colCount * 3)
        else -> tiles.take(colCount)
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
            if (wifiTile != null && btTile != null) {
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
                            )
                        }
                    }
                    repeat(colCount - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
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
