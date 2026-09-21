package com.supershade.ui.shade

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.LocalBackdropTheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.tile.TileDefinition
import com.supershade.settings.TileShape
import com.supershade.settings.TileSize
import com.supershade.ui.theme.ShadeTheme

@Composable
fun TileCard(
    tile: TileDefinition,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    tileShape: TileShape = TileShape.SQUIRCLE,
    tileSize: TileSize = TileSize.STANDARD,
    columns: Int = 4,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isEditing: Boolean = false,
    /** Set to true when this card is being dragged (renders with lifted shadow/scale) */
    isDragging: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val shapeScheme = com.supershade.ui.theme.LocalShadeShapeScheme.current

    val cardShape = when {
        theme is ShadeTheme.Pixel -> CircleShape
        else -> shapeScheme.tile
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale: pressed shrink, dragging lift, otherwise 1f
    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.08f
            isPressed -> 0.92f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label = "tileScale",
    )

    val containerColor by animateColorAsState(
        targetValue = if (tile.isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "tileContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (tile.isActive)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "tileContent",
    )

    val indication = LocalIndication.current
    val borderStroke = if (tile.isActive) null else getCardBorder(alpha = 0.40f)

    val stateDesc = if (tile.isActive) {
        tile.subtitle ?: "Active"
    } else {
        "Off"
    }

    val cardHeight = tileSize.heightDp.dp
    val rawIconSize = if (columns >= 5) (tileSize.iconSizeDp - 2).coerceAtLeast(18) else tileSize.iconSizeDp
    val iconSize = rawIconSize.dp
    val horizPadding = if (columns >= 5) (shapeScheme.tilePaddingHorizontal - 2.dp).coerceAtLeast(4.dp) else shapeScheme.tilePaddingHorizontal
    val vertPadding = when (tileSize) {
        TileSize.COMPACT    -> (shapeScheme.tilePaddingVertical - 3.dp).coerceAtLeast(4.dp)
        TileSize.COMFORTABLE -> shapeScheme.tilePaddingVertical + 2.dp
        TileSize.STANDARD   -> shapeScheme.tilePaddingVertical
    }

    Surface(
        shape = cardShape,
        color = containerColor,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Subtle shadow lift when dragging
                shadowElevation = if (isDragging) 24f else 0f
            }
            .semantics {
                role = Role.Switch
                stateDescription = stateDesc
                tile.settingsAction?.let { action ->
                    customActions = listOf(
                        CustomAccessibilityAction("Open settings") {
                            try {
                                context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                true
                            } catch (_: Exception) { false }
                        }
                    )
                }
            },
    ) {
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
                                end = Offset(200f, 200f),
                            )
                        )
                )
            }

            // Click / long-press — disabled while editing (parent handles drag)
            val clickModifier = if (!isEditing) {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = indication,
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
                    role = Role.Switch,
                )
            } else Modifier

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(clickModifier)
                    .padding(horizontal = horizPadding, vertical = vertPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = tileIcon(tile.id, tile.isActive, tile.subtitle),
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize),
                    )
                    if (tile.isActive && !isEditing) {
                        Box(
                            modifier = Modifier
                                .size(if (columns >= 5) 5.dp else 6.dp)
                                .clip(CircleShape)
                                .background(contentColor.copy(alpha = 0.85f)),
                        )
                    }
                }
                val baseSize = when (tileSize) {
                    TileSize.COMPACT    -> if (columns >= 5) 8.5.sp else 9.5.sp
                    TileSize.COMFORTABLE -> if (columns >= 5) 9.5.sp else 12.sp
                    TileSize.STANDARD   -> if (columns >= 5) 8.5.sp else 10.5.sp
                }
                val displayLabel = when {
                    tile.id.lowercase().contains("rotation") -> if (tile.isActive) "Auto rotate" else "Portrait"
                    tile.id.lowercase().contains("mute") || tile.id.lowercase().contains("sound") -> tile.subtitle ?: tile.label
                    else -> tile.label
                }
                val displaySubtitle = when {
                    tile.id.lowercase().contains("rotation") -> null
                    tile.id.lowercase().contains("mute") || tile.id.lowercase().contains("sound") -> null
                    else -> tile.subtitle
                }
                val titleFontSize = when {
                    columns >= 5 && displayLabel.length > 8 -> (baseSize.value - 1f).sp
                    displayLabel.length > 13 -> (baseSize.value - 0.75f).sp
                    else -> baseSize
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = titleFontSize,
                            lineHeight = (titleFontSize.value + 2).sp,
                        ),
                        color = contentColor,
                        maxLines = if (tileSize == TileSize.COMPACT || displaySubtitle != null) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true,
                    )
                    if (displaySubtitle != null && tileSize != TileSize.COMPACT && !isEditing) {
                        Text(
                            text = displaySubtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (columns >= 5) 8.sp else 9.sp,
                                lineHeight = if (columns >= 5) 9.sp else 10.sp,
                            ),
                            color = contentColor.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Remove (×) badge shown when editing — top-right corner
            if (isEditing && onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable {
                            haptics.lightTap()
                            onRemove()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${tile.label}",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

fun tileIcon(id: String, isActive: Boolean, subtitle: String?): ImageVector {
    return when {
        id.contains("wifi") || id.contains("internet") -> Icons.Default.Wifi
        id.contains("bt") || id.contains("bluetooth")  -> Icons.Default.Bluetooth
        id.contains("airplane")                        -> Icons.Default.AirplanemodeActive
        id.contains("hotspot") || id.contains("tether") -> Icons.Default.WifiTethering
        id.contains("dnd") || id.contains("disturb")   -> Icons.Default.DoNotDisturb
        id.contains("rotation") || id.contains("rotate") -> Icons.Default.ScreenRotation
        id.contains("dark") || id.contains("night")    -> Icons.Default.DarkMode
        id.contains("flash") || id.contains("torch")   -> Icons.Default.FlashOn
        id.contains("location") || id.contains("gps")  -> Icons.Default.LocationOn
        id.contains("nfc")                             -> Icons.Default.Nfc
        id.contains("sync")                            -> Icons.Default.Sync
        id.contains("cast") || id.contains("screen")  -> Icons.Default.Cast
        id.contains("vpn")                             -> Icons.Default.VpnKey
        id.contains("data") || id.contains("mobile")  -> Icons.Default.SignalCellularAlt
        id.contains("battery") || id.contains("saver") -> Icons.Default.Battery5Bar
        id.contains("chargi")                          -> Icons.Default.BatteryChargingFull
        id.contains("work") || id.contains("focus")    -> Icons.Default.Work
        id.contains("alarm")                           -> Icons.Default.Alarm
        id.contains("lock") || id.contains("secure")  -> Icons.Default.ScreenLockPortrait
        id.contains("radio") || id.contains("nrs")    -> Icons.Default.RadioButtonChecked
        id.contains("sensor")                         -> Icons.Default.PanTool
        id.contains("vibrate") || id.contains("vibration") -> Icons.Default.Vibration
        id.contains("mute") || id.contains("sound") || id.contains("volume") -> {
            if (isActive || subtitle?.contains("Vibrate", ignoreCase = true) == true)
                Icons.AutoMirrored.Filled.VolumeOff
            else
                Icons.AutoMirrored.Filled.VolumeUp
        }
        id.contains("bedtime") || id.contains("sleep") -> Icons.Default.NightsStay
        id.contains("usage") || id.contains("stats")  -> Icons.Default.DataUsage
        else -> Icons.Default.Settings
    }
}
