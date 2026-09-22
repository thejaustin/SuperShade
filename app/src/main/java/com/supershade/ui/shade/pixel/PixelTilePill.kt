package com.supershade.ui.shade.pixel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.tile.TileCapability
import com.supershade.domain.tile.TileDefinition
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.shade.tileIcon
import com.supershade.ui.theme.getCardBorder

/**
 * Android 15/16 Material Expressive (Pixel) Quick Settings Tile Pill.
 * Features a 2-column wide stadium pill with an embedded circular icon container
 * on the left and stacked Title + Subtitle on the right.
 */
@Composable
fun PixelTilePill(
    tile: TileDefinition,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    isEditing: Boolean = false,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.05f
            isPressed -> 0.96f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pixelTileScale",
    )

    val isActive = tile.isActive
    val pillBgColor by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.surfaceContainerHigh
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "pixelTilePillBg",
    )

    val onPillColor by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.onSurface
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "pixelTileOnPill",
    )

    val iconContainerColor by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.surfaceContainerHighest
            isActive -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.20f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "pixelTileIconBg",
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.onSurfaceVariant
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "pixelTileIconTint",
    )

    val subtitleText = tile.subtitle ?: if (isActive) "On" else "Off"

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isDragging) 12f else 0f
            },
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = pillBgColor,
            border = getCardBorder(alpha = if (isActive) 0.15f else 0.30f),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(28.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (!isEditing) {
                            if (tile.capability != TileCapability.READ_ONLY) {
                                if (isActive) haptics.tileToggleOff() else haptics.tileToggleOn()
                            }
                            onClick()
                        }
                    },
                    onLongClick = {
                        if (!isEditing && onLongClick != null) {
                            haptics.sheetDetent()
                            onLongClick()
                        }
                    },
                )
                .semantics {
                    role = Role.Switch
                    stateDescription = if (isActive) "Active" else "Inactive"
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Circular icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tileIcon(tile.id, isActive, tile.subtitle),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title and subtitle stacked vertically
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = tile.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.1.sp,
                        ),
                        color = onPillColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = onPillColor.copy(alpha = if (isActive) 0.85f else 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Edit mode remove badge
        if (isEditing && onRemove != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable {
                        haptics.tileToggleOff()
                        onRemove()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove ${tile.label}",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
