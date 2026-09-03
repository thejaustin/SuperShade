package com.supershade.ui.shade

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AirplanemodeActive
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.supershade.domain.tile.TileCapability
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

@Composable
fun TileCard(
    tile: TileDefinition,
    theme: ShadeTheme,
    isShizukuConnected: Boolean,
    onClick: () -> Unit,
) {
    // Pixel uses a full pill; OneUI uses a very rounded rect
    val cornerRadius = if (theme is ShadeTheme.Pixel) 50 else 22

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness    = Spring.StiffnessHigh,
        ),
        label = "tileScale",
    )

    // Dim FULL_TOGGLE tiles when Shizuku is absent — tap opens Settings instead.
    val tileAlpha by animateFloatAsState(
        targetValue = if (tile.capability == TileCapability.FULL_TOGGLE && !isShizukuConnected) 0.5f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileAlpha",
    )

    val containerColor by animateColorAsState(
        targetValue = if (tile.isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
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
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "tileContent",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = tileAlpha },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = tileIcon(tile.id),
                contentDescription = tile.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun tileIcon(id: String): ImageVector = when (id) {
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
    "mute"         -> Icons.Default.VolumeOff
    "volume"       -> Icons.Default.VolumeUp
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
