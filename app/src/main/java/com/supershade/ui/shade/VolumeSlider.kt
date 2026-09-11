package com.supershade.ui.shade

import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
fun VolumeSlider(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var isDragging by remember { mutableStateOf(false) }
    var localValue by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }

    // Poll for hardware-key volume changes (e.g. user adjusts while shade is open).
    LaunchedEffect(Unit) {
        while (true) {
            delay(400L)
            if (!isDragging) {
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                if (kotlin.math.abs(current - localValue) >= 1f) localValue = current
            }
        }
    }

    val fraction = if (maxVol > 0f) localValue / maxVol else 0f
    val volumeIcon = when {
        localValue == 0f -> Icons.Default.VolumeOff
        fraction < 0.5f  -> Icons.Default.VolumeDown
        else             -> Icons.Default.VolumeUp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconButton(
            onClick = {
                try {
                    val panelIntent = Intent(Settings.Panel.ACTION_VOLUME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(panelIntent)
                } catch (_: Exception) {
                    if (localValue > 0f) {
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                            localValue = 0f
                        } catch (_: Exception) {}
                    } else {
                        val half = (maxVol / 2f).coerceAtLeast(1f)
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, half.toInt(), 0)
                            localValue = half
                        } catch (_: Exception) {}
                    }
                }
            },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = volumeIcon,
                contentDescription = "Volume Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (localValue == 0f) 1f else 0.85f
                ),
                modifier = Modifier.size(20.dp),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1A237E), Color(0xFF42A5F5)),
                        )
                    )
            )
            Slider(
                value = localValue,
                onValueChange = {
                    isDragging = true
                    localValue = it
                    try {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            it.toInt().coerceIn(0, maxVol.toInt()),
                            0,
                        )
                    } catch (_: Exception) {}
                },
                onValueChangeFinished = {
                    isDragging = false
                    try {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            localValue.toInt().coerceIn(0, maxVol.toInt()),
                            0,
                        )
                    } catch (_: Exception) {}
                },
                valueRange = 0f..maxVol,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
}
