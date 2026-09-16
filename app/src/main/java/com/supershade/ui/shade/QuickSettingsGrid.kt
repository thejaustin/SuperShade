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
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

/**
 * Quick settings grid supporting compact mode (1 row, 4 primary quick tiles)
 * and expanded mode enclosed in a modern One UI 8 island container.
 * In One UI expanded mode, prominent dual connectivity pills (Wi-Fi & Bluetooth)
 * sit at the top of the island matching Samsung One UI 8.
 */
@Composable
fun QuickSettingsGrid(
    tiles: List<TileDefinition>,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    isExpanded: Boolean = false,
    onTileClick: (TileDefinition) -> Unit,
) {
    val showOneUiIslandCards = isExpanded && theme is ShadeTheme.OneUI
    val wifiTile = if (showOneUiIslandCards) tiles.firstOrNull { it.id == "wifi" || it.id == "internet" } else null
    val btTile = if (showOneUiIslandCards) tiles.firstOrNull { it.id == "bt" || it.id == "bluetooth" } else null
    val hasWideCards = wifiTile != null && btTile != null

    val displayedTiles = when {
        hasWideCards -> tiles.filter { it != wifiTile && it != btTile }.take(8)
        isExpanded -> tiles.take(12)
        else -> tiles.take(4)
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
        ),
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
                        onClick = { onTileClick(wifiTile) },
                        modifier = Modifier.weight(1f),
                    )
                    ConnectivityWideCard(
                        tile = btTile,
                        theme = theme,
                        onClick = { onTileClick(btTile) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            displayedTiles.chunked(4).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowTiles.forEach { tile ->
                        Box(modifier = Modifier.weight(1f)) {
                            TileCard(
                                tile = tile,
                                theme = theme,
                                isShizukuConnected = isShizukuConnected,
                                onClick = { onTileClick(tile) },
                            )
                        }
                    }
                    repeat(4 - rowTiles.size) {
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
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

    val borderStroke = if (tile.isActive) null else BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .height(62.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        val stateDesc = tile.subtitle ?: if (tile.isActive) "Connected" else "Off"
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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = tile.settingsAction?.let { action ->
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            try {
                                context.startActivity(
                                    Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                )
                            } catch (_: Exception) {}
                        }
                    },
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
