package com.supershade.ui.shade

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

@Composable
fun BrightnessSlider(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local state drives smooth Slider movement during drag without calling
    // Settings.System.putInt on every pixel. The write happens once on finger-up
    // via onValueChangeFinished. Resync from the ViewModel value whenever it
    // changes externally (e.g. auto-brightness event).
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dimAlpha),
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
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF37474F), Color(0xFFFFEE58)),
                        )
                    )
            )
            Slider(
                value = localValue,
                onValueChange = { localValue = it },
                onValueChangeFinished = { onBrightnessChange(localValue.toInt()) },
                valueRange = 1f..255f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
        Icon(
            imageVector = Icons.Default.BrightnessHigh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = brightAlpha),
            modifier = Modifier.size(20.dp),
        )
    }
}
