package com.supershade.ui.shade

import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
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
import com.supershade.ui.theme.ChamferedCornerShape
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.getCardBorder
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VolumeSlider(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onOpenVolumeMixer: (() -> Unit)? = null,
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
    val animatedProgressFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.82f, stiffness = 450f),
        label = "animatedVolumeFraction",
    )
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
        if (onOpenVolumeMixer != null) {
            onOpenVolumeMixer()
            return
        }
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
    var hitMinBoundary by remember { mutableStateOf(false) }
    var hitMaxBoundary by remember { mutableStateOf(false) }

    fun updateValueFromFraction(newFraction: Float) {
        if (newFraction <= 0f && !hitMinBoundary) {
            haptics.sliderBoundary()
            hitMinBoundary = true
        } else if (newFraction > 0.05f) {
            hitMinBoundary = false
        }

        if (newFraction >= 1f && !hitMaxBoundary) {
            haptics.sliderBoundary()
            hitMaxBoundary = true
        } else if (newFraction < 0.95f) {
            hitMaxBoundary = false
        }

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

    val shadeTheme = LocalShadeTheme.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val fillGradient = remember(shadeTheme, primaryColor, secondaryColor) {
        val baseColors = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> listOf(secondaryColor, primaryColor) // Hot Pink to Electric Cyan
            is ShadeTheme.Nothing -> listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
            is ShadeTheme.Pixel, is ShadeTheme.PureMaterial -> listOf(primaryColor, primaryColor)
            is ShadeTheme.OneUI -> listOf(
                Color(0xFF1565C0),
                Color(0xFF1E88E5),
                Color(0xFF42A5F5),
            )
        }
        Brush.horizontalGradient(baseColors)
    }

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
        val sliderShape = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> ChamferedCornerShape(8.dp)
            else -> shapes.slider
        }
        val sliderHeight = when {
            compact -> 48.dp
            shadeTheme is ShadeTheme.OneUI -> 54.dp
            else -> 50.dp
        }

        val trackScaleY by animateFloatAsState(
            targetValue = if (isDragging) 1.05f else 1.0f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 850f),
            label = "volumeSliderScaleY",
        )
        val iconScale by animateFloatAsState(
            targetValue = if (isDragging) 1.15f else 1.0f,
            animationSpec = spring(dampingRatio = 0.60f, stiffness = 800f),
            label = "volumeIconScale",
        )
        val iconRotation by animateFloatAsState(
            targetValue = if (localValue == 0f) -10f else (fraction - 0.5f) * 16f,
            animationSpec = spring(dampingRatio = 0.70f, stiffness = 750f),
            label = "volumeIconRotation",
        )

        // Main Tactile Volume Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(sliderHeight)
                .graphicsLayer { scaleY = trackScaleY }
                .clip(sliderShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .then(if (border != null) Modifier.border(border, sliderShape) else Modifier)
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
                            hitMinBoundary = false
                            hitMaxBoundary = false
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
                            hitMinBoundary = false
                            hitMaxBoundary = false
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
                    .fillMaxWidth(animatedProgressFraction)
                    .background(fillGradient)
            )

            // Nothing OS Segmented Track Ticks
            if (shadeTheme is ShadeTheme.Nothing) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(9) {
                        Box(
                            modifier = Modifier
                                .size(width = 1.5.dp, height = 18.dp)
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                    }
                }
            }

            // Cyberpunk Laser-etched Hash Marks
            if (shadeTheme is ShadeTheme.Cyberpunk) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    repeat(16) { idx ->
                        Box(
                            modifier = Modifier
                                .size(width = 1.dp, height = if (idx % 4 == 0) 5.dp else 2.5.dp)
                                .background(Color(0xFFFF0055).copy(alpha = 0.50f))
                        )
                    }
                }
            }

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
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                                rotationZ = iconRotation
                            },
                    )
                }

                // Volume percentage text on the right
                Text(
                    text = when (shadeTheme) {
                        is ShadeTheme.Cyberpunk -> if (localValue == 0f) "VOL // MUTE" else "VOL // ${(fraction * 100).roundToInt()}%"
                        is ShadeTheme.Nothing -> if (localValue == 0f) "MUTE" else "${(fraction * 100).roundToInt()}%"
                        else -> if (localValue == 0f) "Mute" else "${(fraction * 100).roundToInt()}%"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                        letterSpacing = if (shadeTheme is ShadeTheme.Nothing) 1.sp else 0.sp,
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
