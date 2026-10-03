package com.supershade.ui.shade.pixel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.ChevronRight
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
import com.supershade.ui.theme.M3ExpressiveMotion
import com.supershade.ui.theme.M3ExpressiveShapes
import com.supershade.ui.theme.getCardBorder
import com.supershade.ui.theme.rememberMorphShape

/**
 * Android 15/16 Material Expressive (Pixel) Quick Settings Tile Pill.
 * Features reactive shape morphing:
 * - Outer container compresses from 28dp pill down to 20dp on press.
 * - Inner icon container morphs from 23dp circle (inactive) to 12dp squircle (active).
 * - Icon micro-rotation and scale bounce on activation pop.
 * - Live status dot smoothly expands with spatial spring physics.
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
    val isActive = tile.isActive

    // 1. Reactive Outer Corner Radius Morphing (Android 16 M3E: 28dp pill -> 14dp expressive squircle)
    val outerCornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 18.dp
            isActive -> 14.dp
            else -> 28.dp
        },
        animationSpec = M3ExpressiveMotion.spatialDefault(),
        label = "pixelTileOuterCornerRadius",
    )

    // 2. M3E Fast Spatial Scale
    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.06f
            isPressed -> 0.94f
            else -> 1.0f
        },
        animationSpec = M3ExpressiveMotion.spatialFast(),
        label = "pixelTileScale",
    )

    // 3. Real-Time M3 Expressive Polygon Shape Morphing: Circle <-> SoftBurst8
    val morphProgress by animateFloatAsState(
        targetValue = if (isActive) 1.0f else 0.0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 650f),
        label = "pixelTileMorphProgress",
    )

    val morphShape = rememberMorphShape(
        morph = M3ExpressiveShapes.MorphCircleToBurst,
        progress = morphProgress,
        rotationAngle = if (isActive) 0f else -10f,
    )

    // 4. Icon Container Scale Bounce
    val iconContainerScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = M3ExpressiveMotion.spatialFast(),
        label = "pixelTileIconContainerScale",
    )

    // 5. Icon Micro-Rotation / Tilt
    val iconRotation by animateFloatAsState(
        targetValue = when {
            tile.id.contains("rotate") -> if (isActive) 90f else 0f
            tile.id.contains("sync") -> if (isActive) 180f else 0f
            isActive -> 0f
            else -> -5f
        },
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 850f),
        label = "pixelTileIconRotation",
    )

    // 6. Live Status Dot Expansion
    val dotWidth by animateDpAsState(
        targetValue = if (isActive && !isEditing) 6.dp else 0.dp,
        animationSpec = M3ExpressiveMotion.spatialDefault(),
        label = "pixelTileDotWidth",
    )

    val pillBgColor by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.surfaceContainerHigh
            isActive -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = M3ExpressiveMotion.effectsDefault(),
        label = "pixelTilePillBg",
    )

    val onPillColor by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.onSurface
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = M3ExpressiveMotion.effectsDefault(),
        label = "pixelTileOnPill",
    )

    val iconContainerColor by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.surfaceContainerHighest
            isActive -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = M3ExpressiveMotion.effectsDefault(),
        label = "pixelTileIconBg",
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            isEditing -> MaterialTheme.colorScheme.onSurfaceVariant
            isActive -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = M3ExpressiveMotion.effectsDefault(),
        label = "pixelTileIconTint",
    )

    val displayLabel = when {
        tile.id.lowercase().contains("rotation") -> if (isActive) "Auto-rotate" else "Portrait"
        tile.id.lowercase().contains("mute") || tile.id.lowercase().contains("sound") -> tile.subtitle ?: tile.label
        else -> tile.label.ifBlank { com.supershade.domain.tile.humanizeTileLabel(tile.id) }
    }
    val subtitleText = when {
        tile.id.lowercase().contains("rotation") -> if (isActive) "On" else "Off"
        tile.id.lowercase().contains("mute") || tile.id.lowercase().contains("sound") -> if (isActive) "Active" else "Off"
        else -> tile.subtitle ?: if (isActive) "On" else "Off"
    }
    val isExpandable = tile.id in setOf("internet", "wifi", "bt", "bluetooth", "hotspot", "dnd", "mute", "sound", "volume")

    val pillShape = RoundedCornerShape(outerCornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isDragging) 12f else 0f
            },
    ) {
        Surface(
            shape = pillShape,
            color = pillBgColor,
            border = getCardBorder(alpha = if (isActive) 0.12f else 0.30f),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(pillShape)
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
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Morphing Squircle / Circle Icon Container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = iconContainerScale
                            scaleY = iconContainerScale
                        }
                        .clip(morphShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    if (tile.customIcon != null) {
                        androidx.compose.foundation.Image(
                            bitmap = tile.customIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    rotationZ = iconRotation
                                },
                        )
                    } else {
                        Icon(
                            imageVector = tileIcon(tile.id, isActive, tile.subtitle),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    rotationZ = iconRotation
                                },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title and Subtitle with Live Dot
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.15.sp,
                        ),
                        color = onPillColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (dotWidth > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .size(width = dotWidth, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(onPillColor.copy(alpha = 0.85f)),
                            )
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            color = onPillColor.copy(alpha = if (isActive) 0.85f else 0.70f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Trailing expander chevron for expandable tiles
                if (isExpandable && !isEditing) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = onPillColor.copy(alpha = if (isActive) 0.75f else 0.50f),
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(18.dp),
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
                    .size(24.dp)
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

