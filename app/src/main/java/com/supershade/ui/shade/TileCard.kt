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
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sync
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
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val cardShape = when {
        theme is ShadeTheme.Pixel -> CircleShape
        tileShape == TileShape.CIRCLE -> CircleShape
        tileShape == TileShape.ROUNDED -> RoundedCornerShape(16.dp)
        tileShape == TileShape.PILL -> RoundedCornerShape(28.dp)
        tileShape == TileShape.SOFT -> RoundedCornerShape(12.dp)
        tileShape == TileShape.LEAF -> RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
        tileShape == TileShape.SHARP -> RoundedCornerShape(6.dp)
        else -> RoundedCornerShape(22.dp)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
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

    val borderStroke = if (tile.isActive) null else BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
    )

    val stateDesc = if (tile.isActive) {
        tile.subtitle ?: "Active"
    } else {
        "Off"
    }

    val cardHeight = tileSize.heightDp.dp
    val rawIconSize = if (columns >= 5) (tileSize.iconSizeDp - 2).coerceAtLeast(18) else tileSize.iconSizeDp
    val iconSize = rawIconSize.dp
    val vertPadding = when (tileSize) {
        TileSize.COMPACT -> 5.dp
        TileSize.COMFORTABLE -> 10.dp
        TileSize.STANDARD -> 8.dp
    }

    Surface(
        shape = cardShape,
        color = containerColor,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
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
                .padding(horizontal = if (columns >= 5) 6.dp else 8.dp, vertical = vertPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = tileIcon(tile.id),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize),
                )
                if (tile.isActive) {
                    Box(
                        modifier = Modifier
                            .size(if (columns >= 5) 5.dp else 6.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.85f)),
                    )
                }
            }
            val baseSize = when (tileSize) {
                TileSize.COMPACT -> if (columns >= 5) 8.5.sp else 9.5.sp
                TileSize.COMFORTABLE -> if (columns >= 5) 9.5.sp else 12.sp
                TileSize.STANDARD -> if (columns >= 5) 8.5.sp else 10.5.sp
            }
            val titleFontSize = when {
                columns >= 5 && tile.label.length > 8 -> (baseSize.value - 1f).sp
                tile.label.length > 13 -> (baseSize.value - 0.75f).sp
                else -> baseSize
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = titleFontSize,
                        lineHeight = (titleFontSize.value + 2).sp,
                    ),
                    color = contentColor,
                    maxLines = if (tileSize == TileSize.COMPACT || tile.subtitle != null) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                )
                if (tile.subtitle != null && tileSize != TileSize.COMPACT) {
                    Text(
                        text = tile.subtitle,
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
    }
}

internal fun tileIcon(id: String): ImageVector = when (id) {
    "internet"     -> Icons.Default.Wifi
    "wifi"         -> Icons.Default.Wifi
    "bt"           -> Icons.Default.Bluetooth
    "nfc"          -> Icons.Default.Nfc
    "hotspot"      -> Icons.Default.WifiTethering
    "airplane"     -> Icons.Default.AirplanemodeActive
    "cell"         -> Icons.Default.SignalCellularAlt
    "vpn"          -> Icons.Default.VpnKey
    "dark"         -> Icons.Default.DarkMode
    "night"        -> Icons.Default.NightsStay
    "rotation"     -> Icons.Default.ScreenRotation
    "cast"         -> Icons.Default.Cast
    "screenrecord" -> Icons.Default.RadioButtonChecked
    "dnd"          -> Icons.Default.DoNotDisturb
    "flashlight"   -> Icons.Default.FlashOn
    "mute"         -> Icons.AutoMirrored.Filled.VolumeOff
    "volume"       -> Icons.AutoMirrored.Filled.VolumeUp
    "battery"      -> Icons.Default.Battery5Bar
    "powershare"   -> Icons.Default.BatteryChargingFull
    "location"     -> Icons.Default.LocationOn
    "alarm"        -> Icons.Default.Alarm
    "sync"         -> Icons.Default.Sync
    "datasaver"    -> Icons.Default.DataUsage
    "work"         -> Icons.Default.Work
    "onehanded"    -> Icons.Default.PanTool
    else           -> Icons.Default.Settings
}
