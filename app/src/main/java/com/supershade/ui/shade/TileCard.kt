package com.supershade.ui.shade

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.unit.dp
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

@Composable
fun TileCard(
    tile: TileDefinition,
    theme: ShadeTheme,
    onClick: () -> Unit,
) {
    val cornerRadius = if (theme is ShadeTheme.Pixel) 50 else 16

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "tileScale",
    )

    val containerColor by animateColorAsState(
        targetValue = if (tile.isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
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
            stiffness = Spring.StiffnessMedium,
        ),
        label = "tileContent",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(width = 80.dp, height = 64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = tileIcon(tile.id),
                contentDescription = tile.label,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

private fun tileIcon(id: String): ImageVector = when (id) {
    "internet"   -> Icons.Default.Wifi
    "bt"         -> Icons.Default.Bluetooth
    "airplane"   -> Icons.Default.AirplanemodeActive
    "dnd"        -> Icons.Default.DoNotDisturb
    "flashlight" -> Icons.Default.FlashOn
    "rotation"   -> Icons.Default.ScreenRotation
    "nfc"        -> Icons.Default.Nfc
    "hotspot"    -> Icons.Default.Wifi
    "location"   -> Icons.Default.LocationOn
    "battery"    -> Icons.Default.Battery5Bar
    else         -> Icons.Default.Settings
}
