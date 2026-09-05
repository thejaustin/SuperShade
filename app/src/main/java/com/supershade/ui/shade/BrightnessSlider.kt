package com.supershade.ui.shade

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

@Composable
fun BrightnessSlider(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Auto-brightness state: read once from Settings.System, then track locally.
    var isAuto by remember {
        mutableStateOf(
            try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            } catch (_: Exception) { false }
        )
    }

    fun toggleAuto() {
        val newMode = if (isAuto)
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        else
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                newMode,
            )
            isAuto = !isAuto
        } catch (_: SecurityException) {}
    }

    // Local float drives the slider during drag; writes Settings on finger-up.
    var localValue by remember(brightness) { mutableFloatStateOf(brightness.toFloat()) }

    val fraction = (localValue - 1f) / 254f
    val dimAlpha by animateFloatAsState(
        targetValue = lerp(1f, 0.3f, fraction),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dimAlpha",
    )
    val brightAlpha by animateFloatAsState(
        targetValue = lerp(0.3f, 1f, fraction),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "brightAlpha",
    )
    val autoIconTint by animateColorAsState(
        targetValue = if (isAuto)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "autoIconTint",
    )
    val autoBg by animateColorAsState(
        targetValue = if (isAuto)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "autoBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Default.BrightnessLow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isAuto) 0.3f else dimAlpha),
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isAuto) Brush.horizontalGradient(
                            listOf(
                                Color(0xFF37474F).copy(alpha = 0.35f),
                                Color(0xFFFFEE58).copy(alpha = 0.35f),
                            )
                        ) else Brush.horizontalGradient(
                            listOf(Color(0xFF37474F), Color(0xFFFFEE58))
                        )
                    )
            )
            Slider(
                value = localValue,
                onValueChange = { if (!isAuto) localValue = it },
                onValueChangeFinished = { if (!isAuto) onBrightnessChange(localValue.toInt()) },
                valueRange = 1f..255f,
                enabled = !isAuto,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    disabledThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                ),
            )
        }
        Icon(
            imageVector = Icons.Default.BrightnessHigh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isAuto) 0.3f else brightAlpha),
            modifier = Modifier.size(20.dp),
        )
        // Auto-brightness toggle pill
        IconButton(
            onClick = { toggleAuto() },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(autoBg),
        ) {
            Icon(
                imageVector = Icons.Default.BrightnessAuto,
                contentDescription = if (isAuto) "Disable auto brightness" else "Enable auto brightness",
                tint = autoIconTint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
