package com.supershade.ui.shade

import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VolumeSlider(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var isDragging by remember { mutableStateOf(false) }
    var localValue by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var lastNonZeroVolume by remember { mutableFloatStateOf((maxVol / 2f).coerceAtLeast(1f)) }

    val audioRepo = remember(context) { com.supershade.domain.audio.AudioRepository(context) }

    // Reactively receive hardware-key volume events without polling.
    LaunchedEffect(audioRepo) {
        audioRepo.musicVolume.collect { vs ->
            if (!isDragging) {
                localValue = vs.current.toFloat()
                if (vs.current > 0) lastNonZeroVolume = vs.current.toFloat()
            }
        }
    }

    val fraction = if (maxVol > 0f) (localValue / maxVol).coerceIn(0f, 1f) else 0f
    val volumeIcon = when {
        localValue == 0f -> Icons.AutoMirrored.Filled.VolumeOff
        fraction < 0.5f  -> Icons.AutoMirrored.Filled.VolumeDown
        else             -> Icons.AutoMirrored.Filled.VolumeUp
    }

    fun toggleMute() {
        if (localValue > 0f) {
            lastNonZeroVolume = localValue
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                localValue = 0f
                haptics.tileToggleOff()
            } catch (_: Exception) {}
        } else {
            val restore = lastNonZeroVolume.coerceIn(1f, maxVol)
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restore.toInt(), 0)
                localValue = restore
                haptics.tileToggleOn()
            } catch (_: Exception) {}
        }
    }

    fun openVolumePanel() {
        haptics.sheetDetent()
        try {
            val panelIntent = Intent(Settings.Panel.ACTION_VOLUME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(panelIntent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SOUND_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
        }
    }

    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    fun updateValueFromFraction(newFraction: Float) {
        val clamped = newFraction.coerceIn(0f, 1f)
        val newVol = (clamped * maxVol).roundToInt().coerceIn(0, maxVol.toInt())
        if (newVol.toFloat() != localValue) {
            haptics.sliderTick()
        }
        localValue = newVol.toFloat()
        if (newVol > 0) lastNonZeroVolume = localValue
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        } catch (_: Exception) {}
    }

    val fillGradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF1565C0),
            Color(0xFF42A5F5),
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 4.dp else 16.dp,
                vertical = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shapes = LocalShadeShapeScheme.current
        val border = getCardBorder(alpha = 0.35f)

        // Main Tactile Volume Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(shapes.slider)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .then(if (border != null) Modifier.border(border, shapes.slider) else Modifier)
                .semantics {
                    contentDescription = "Media volume"
                    stateDescription = if (localValue == 0f) "Muted" else "${(fraction * 100).roundToInt()}%"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = localValue,
                        range = 0f..maxVol,
                        steps = 0,
                    )
                    setProgress { targetValue ->
                        val clamped = targetValue.coerceIn(0f, maxVol)
                        localValue = clamped
                        try {
                            audioManager.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                clamped.toInt(),
                                0,
                            )
                        } catch (_: Exception) {}
                        true
                    }
                }
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        haptics.sliderTick()
                        updateValueFromFraction(offset.x / trackWidthPx)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            haptics.sliderTick()
                            updateValueFromFraction(offset.x / trackWidthPx)
                        },
                        onDragEnd = {
                            isDragging = false
                            haptics.sliderTick()
                            try {
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    localValue.toInt().coerceIn(0, maxVol.toInt()),
                                    0,
                                )
                            } catch (_: Exception) {}
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            updateValueFromFraction(change.position.x / trackWidthPx)
                        }
                    )
                },
        ) {
            // Active Progress Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(fillGradient)
            )

            // Embedded Content Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Speaker icon on the left (tap to mute/unmute, long press for sound settings)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = { toggleMute() },
                            onLongClick = { openVolumePanel() },
                            role = Role.Button,
                        )
                        .semantics {
                            contentDescription = if (localValue == 0f) "Unmute volume" else "Mute volume"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = null,
                        tint = if (fraction > 0.18f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Volume percentage text on the right
                Text(
                    text = if (localValue == 0f) "Mute" else "${(fraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    ),
                    color = if (fraction > 0.85f) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Volume Panel / Sound settings button
        val mixerBorder = getCardBorder(alpha = 0.35f)
        IconButton(
            onClick = { openVolumePanel() },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f))
                .then(if (mixerBorder != null) Modifier.border(mixerBorder, CircleShape) else Modifier)
                .semantics {
                    role = Role.Button
                    contentDescription = "Volume mixer panel"
                },
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Volume Mixer Panel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
