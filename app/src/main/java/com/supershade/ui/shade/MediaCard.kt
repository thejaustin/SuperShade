package com.supershade.ui.shade

import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.supershade.domain.media.MediaState
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.shade.pixel.PixelMediaCard
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.ChamferedCornerShape
import com.supershade.ui.theme.LocalBackdropTheme
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.getCardBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

/**
 * Animated audio spectrum visualizer bars that bounce dynamically while playing.
 */
@Composable
fun AudioEqualizerVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(470, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar4",
    )

    val currentHeights = if (isPlaying) listOf(bar1, bar2, bar3, bar4) else listOf(0.25f, 0.25f, 0.25f, 0.25f)

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        currentHeights.forEach { fraction ->
            val animatedFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "barHeight",
            )
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((14 * animatedFraction).dp.coerceAtLeast(2.5.dp))
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

/**
 * Theme-aware audio output device chip.
 * Routes to system media output selector panel or sound settings.
 */
@Composable
fun AudioOutputChip(
    packageName: String,
    theme: ShadeTheme = LocalShadeTheme.current,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val (currentOutput, outputIcon) = remember(audioManager, packageName) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val devices = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val btDevice = devices?.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                }
                if (btDevice != null) {
                    val icon = when (btDevice.type) {
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLE_HEADSET -> Icons.Default.Headphones
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    (btDevice.productName?.toString() ?: "Bluetooth Audio") to icon
                } else {
                    "Phone Speaker" to Icons.AutoMirrored.Filled.VolumeUp
                }
            } else {
                "Phone Speaker" to Icons.AutoMirrored.Filled.VolumeUp
            }
        } catch (_: Exception) {
            "Phone Speaker" to Icons.AutoMirrored.Filled.VolumeUp
        }
    }

    val chipShape = when (theme) {
        is ShadeTheme.Cyberpunk -> ChamferedCornerShape(6.dp)
        is ShadeTheme.Nothing -> RoundedCornerShape(6.dp)
        is ShadeTheme.OneUI -> RoundedCornerShape(14.dp)
        else -> RoundedCornerShape(12.dp)
    }

    val chipBorder = when (theme) {
        is ShadeTheme.Cyberpunk -> BorderStroke(1.dp, Color(0xFF00F0FF))
        is ShadeTheme.Nothing -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
        is ShadeTheme.OneUI -> getCardBorder(borderColor = Color.White.copy(alpha = 0.22f))
        else -> getCardBorder(borderColor = Color.White.copy(alpha = 0.25f))
    }

    val chipBg = when (theme) {
        is ShadeTheme.Cyberpunk -> Color(0xFF0D1424)
        is ShadeTheme.Nothing -> Color(0xFF141619)
        is ShadeTheme.OneUI -> Color.White.copy(alpha = 0.16f)
        else -> Color.White.copy(alpha = 0.15f)
    }

    Surface(
        onClick = {
            haptics.sheetDetent()
            try {
                val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                    putExtra("com.android.settings.panel.extra.PACKAGE_NAME", packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {}
            }
        },
        shape = chipShape,
        color = chipBg,
        border = chipBorder,
        modifier = modifier.height(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = outputIcon,
                contentDescription = null,
                tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else Color.White,
                modifier = Modifier.size(13.dp),
            )
            val displayText = when (theme) {
                is ShadeTheme.OneUI -> if (currentOutput == "Phone Speaker") "Media output" else currentOutput
                is ShadeTheme.Nothing -> currentOutput.uppercase()
                is ShadeTheme.Cyberpunk -> currentOutput
                else -> currentOutput
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (theme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                    letterSpacing = if (theme is ShadeTheme.Nothing) 0.8.sp else 0.sp,
                ),
                color = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = if (theme is ShadeTheme.OneUI) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (theme is ShadeTheme.Cyberpunk) Color(0xFF00F0FF).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.70f),
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

/**
 * Samsung One UI 8.5/9 signature thick pill scrubber bar.
 * Features a rounded capsule track with fine haptic tick detents on scrubbing.
 */
@Composable
fun OneUi9PillScrubber(
    media: MediaState,
    onSeek: (Long) -> Unit,
    haptics: SuperHaptics,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val duration = media.duration.coerceAtLeast(1L)
    val progressFraction = (media.position.toFloat() / duration).coerceIn(0f, 1f)
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    val effectiveFraction = if (isSeeking) seekFraction else progressFraction
    val displayPosition = if (isSeeking) (seekFraction * duration).toLong() else media.position

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(media.duration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        isSeeking = true
                        seekFraction = (down.position.x / w).coerceIn(0f, 1f)
                        haptics.segmentTick()
                        var lastStep = (seekFraction * 20).toInt()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                val finalMs = (seekFraction * duration).toLong()
                                haptics.sheetDetent()
                                onSeek(finalMs)
                                isSeeking = false
                                break
                            }
                            change.consume()
                            seekFraction = (change.position.x / w).coerceIn(0f, 1f)
                            val step = (seekFraction * 20).toInt()
                            if (step != lastStep) {
                                haptics.segmentTick()
                                lastStep = step
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // Track background capsule (height 8dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
            ) {
                // Active progress fill pill
                val fillFraction = effectiveFraction.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.85f),
                                    accentColor,
                                )
                            )
                        ),
                )
            }
        }

        // Timestamps below scrubber
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatMs(displayPosition),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.70f),
            )
            Text(
                text = formatMs(media.duration),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.70f),
            )
        }
    }
}

/**
 * Samsung One UI 8.5/9 inline expandable volume slider.
 * Adjusts STREAM_MUSIC directly with tactile haptic steps.
 */
@Composable
fun OneUi9InlineVolumeSlider(
    haptics: SuperHaptics,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat().coerceAtLeast(1f) }
    var currentVol by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var isDragging by remember { mutableStateOf(false) }

    val audioRepo = remember(context) { com.supershade.domain.audio.AudioRepository(context) }
    LaunchedEffect(audioRepo) {
        audioRepo.musicVolume.collect { vs ->
            if (!isDragging) {
                currentVol = vs.current.toFloat()
            }
        }
    }

    val volFraction = (currentVol / maxVol).coerceIn(0f, 1f)
    val pct = (volFraction * 100).roundToInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = {
                val newVol = if (currentVol > 0) 0f else maxVol / 3f
                currentVol = newVol
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.roundToInt(), 0)
                haptics.tileToggleOff()
            },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = if (currentVol == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = "Mute",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .pointerInput(maxVol) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        isDragging = true
                        val newVol = ((down.position.x / w).coerceIn(0f, 1f) * maxVol).roundToInt()
                        currentVol = newVol.toFloat()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        haptics.segmentTick()
                        var lastStep = newVol

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                isDragging = false
                                haptics.sheetDetent()
                                break
                            }
                            change.consume()
                            val v = ((change.position.x / w).coerceIn(0f, 1f) * maxVol).roundToInt()
                            if (v != lastStep) {
                                currentVol = v.toFloat()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0)
                                haptics.segmentTick()
                                lastStep = v
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.20f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = volFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor),
                )
            }
        }

        Text(
            text = "$pct%",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.width(34.dp),
        )

        IconButton(
            onClick = {
                currentVol = maxVol
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol.roundToInt(), 0)
                haptics.tileToggleOn()
            },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Max Volume",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Samsung One UI 8.5/9 Media Player Card.
 * Redesigned with squircle geometry, top-right "Media output" chip, One UI 9 pill scrubber,
 * inline volume expander, and tactile Samsung haptic ticks.
 */
@Composable
fun OneUi9MediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    mediaCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    val dominantColor by produceState(initialValue = fallbackColor, key1 = media.albumArt) {
        value = if (media.albumArt == null) {
            fallbackColor
        } else {
            withContext(Dispatchers.IO) {
                val palette = Palette.from(media.albumArt).generate()
                val argb = palette.getDarkVibrantColor(
                    palette.getVibrantColor(
                        palette.getMutedColor(fallbackColor.value.toInt())
                    )
                )
                Color(argb)
            }
        }
    }

    val lightAccent by produceState(initialValue = fallbackColor, key1 = media.albumArt) {
        value = if (media.albumArt == null) {
            fallbackColor
        } else {
            withContext(Dispatchers.IO) {
                val palette = Palette.from(media.albumArt).generate()
                val argb = palette.getLightVibrantColor(
                    palette.getVibrantColor(fallbackColor.value.toInt())
                )
                Color(argb)
            }
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val animatedSurface by animateColorAsState(
        targetValue = surfaceColor.copy(alpha = 0.52f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MediaSurface",
    )
    val animatedBg by animateColorAsState(
        targetValue = dominantColor.copy(alpha = 0.36f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MediaBg",
    )
    val animatedAccent by animateColorAsState(
        targetValue = if (lightAccent == fallbackColor) Color.White else lightAccent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MediaAccent",
    )

    var isLiked by remember(media.title + media.artist) { mutableStateOf(false) }
    var showVolumeSlider by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(26.dp)

    val appLabel = remember(media.packageName) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(media.packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            "Media"
        }
    }

    val appIconBitmap = remember(media.packageName) {
        try {
            val d = context.packageManager.getApplicationIcon(media.packageName)
            d.toBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(elevation = 6.dp, shape = cardShape, clip = false)
            .clip(cardShape)
            .background(animatedSurface)
            .background(animatedBg)
            .then(
                getCardBorder(alpha = 0.32f)?.let {
                    Modifier.border(it, cardShape)
                } ?: Modifier
            )
            .animateContentSize(),
    ) {
        // Full-bleed album art background (blurred)
        media.albumArt?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.18f },
                contentScale = ContentScale.Crop,
            )
        }

        // Liquid Glass specular shimmer sweep
        if (LocalBackdropTheme.current == BackdropTheme.LIQUID_GLASS) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(450f, 650f),
                        )
                    )
            )
        }

        // Foreground scrim gradient
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.48f),
                        )
                    )
                )
        )

        if (mediaCollapsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (media.albumArt != null) {
                    Image(
                        bitmap = media.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (media.isRecording) Icons.Default.Mic else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title.ifBlank { "Unknown Title" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (media.artist.isNotBlank()) {
                        Text(
                            text = media.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(animatedAccent.copy(alpha = 0.92f))
                        .clickable {
                            onPlayPause()
                            if (!media.isPlaying) haptics.tileToggleOn() else haptics.tileToggleOff()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (media.isPlaying) "Pause" else "Play",
                        tint = Color.Black.copy(alpha = 0.88f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = {
                        haptics.lightTap()
                        onToggleCollapse()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                // One UI 9 Header Row: App info on left, Media Output Chip on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (appIconBitmap != null) {
                            Image(
                                bitmap = appIconBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.80f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }

                    AudioOutputChip(
                        packageName = media.packageName,
                        theme = ShadeTheme.OneUI,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Track Info & Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // One UI Squircle Album Art (60dp with 20dp smooth corners)
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(6.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(14.dp))
                    } else if (media.isRecording) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .shadow(6.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.30f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Recording",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                    }

                    // Track Title & Artist
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = media.title.ifBlank { "Unknown Title" },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            AudioEqualizerVisualizer(
                                isPlaying = media.isPlaying,
                                color = animatedAccent,
                            )
                        }
                        if (media.artist.isNotBlank()) {
                            Text(
                                text = media.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (media.album.isNotBlank() && media.album != media.title) {
                            Text(
                                text = media.album,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.50f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Action buttons: Like + Volume Expander Toggle + Collapse
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Like Button with bouncy spring
                        val likeScale by animateFloatAsState(
                            targetValue = if (isLiked) 1.25f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh,
                            ),
                            label = "likeScale",
                        )
                        IconButton(
                            onClick = {
                                isLiked = !isLiked
                                if (isLiked) haptics.tileToggleOn() else haptics.tileToggleOff()
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) Color(0xFFFF5C8D) else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .size(19.dp)
                                    .graphicsLayer { scaleX = likeScale; scaleY = likeScale },
                            )
                        }

                        // One UI 9 Volume Expander Toggle
                        IconButton(
                            onClick = {
                                showVolumeSlider = !showVolumeSlider
                                haptics.lightTap()
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Volume Slider",
                                tint = if (showVolumeSlider) animatedAccent else Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(19.dp),
                            )
                        }

                        // Collapse Button
                        IconButton(
                            onClick = {
                                haptics.lightTap()
                                onToggleCollapse()
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExpandLess,
                                contentDescription = "Collapse",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }

                // One UI 9 Inline Volume Slider Expander
                AnimatedVisibility(
                    visible = showVolumeSlider,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        OneUi9InlineVolumeSlider(
                            haptics = haptics,
                            accentColor = animatedAccent,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // One UI 9 Pill Scrubber Bar
                if (media.duration > 0) {
                    OneUi9PillScrubber(
                        media = media,
                        onSeek = onSeek,
                        haptics = haptics,
                        accentColor = animatedAccent,
                    )
                } else if (media.isPlaying) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5252)),
                            )
                            Text(
                                text = "LIVE STREAM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                                color = Color.White.copy(alpha = 0.90f),
                            )
                        }
                        Text(
                            text = "Continuous Playback",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // One UI 9 Transport Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (media.isRecording) {
                        if (media.customStopAction != null) {
                            IconButton(
                                onClick = {
                                    media.customStopAction.invoke()
                                    haptics.tileToggleOff()
                                },
                                modifier = Modifier.size(46.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                        }
                    } else {
                        // Skip Previous with spring bounce
                        val prevInteraction = remember { MutableInteractionSource() }
                        val isPrevPressed by prevInteraction.collectIsPressedAsState()
                        val prevScale by animateFloatAsState(
                            targetValue = if (isPrevPressed) 0.85f else 1.0f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                            label = "prevScale",
                        )
                        IconButton(
                            onClick = {
                                onSkipPrevious()
                                haptics.sliderTick()
                            },
                            interactionSource = prevInteraction,
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer { scaleX = prevScale; scaleY = prevScale },
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    // Central One UI 9 Play/Pause Action Button
                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    val playButtonScale by animateFloatAsState(
                        targetValue = if (isPlayPressed) 0.88f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                        label = "playButtonScale",
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = playButtonScale
                                scaleY = playButtonScale
                            }
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(animatedAccent)
                            .clickable(
                                interactionSource = playInteractionSource,
                                indication = null,
                            ) {
                                onPlayPause()
                                if (!media.isPlaying) haptics.tileToggleOn() else haptics.tileToggleOff()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = Color.Black.copy(alpha = 0.88f),
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    if (!media.isRecording) {
                        // Skip Next with spring bounce
                        val nextInteraction = remember { MutableInteractionSource() }
                        val isNextPressed by nextInteraction.collectIsPressedAsState()
                        val nextScale by animateFloatAsState(
                            targetValue = if (isNextPressed) 0.85f else 1.0f,
                            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
                            label = "nextScale",
                        )
                        IconButton(
                            onClick = {
                                onSkipNext()
                                haptics.sliderTick()
                            },
                            interactionSource = nextInteraction,
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer { scaleX = nextScale; scaleY = nextScale },
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nothing OS Media Player Card.
 * Minimalist monochrome aesthetic with dot-matrix typography, Nothing glyph red accents,
 * 2.5dp wireframe progress bar, and red dot playhead.
 */
@Composable
fun NothingMediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    mediaCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    var isLiked by remember(media.title + media.artist) { mutableStateOf(false) }

    val duration = media.duration.coerceAtLeast(1L)
    val progressFraction = (media.position.toFloat() / duration).coerceIn(0f, 1f)
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    val effectiveFraction = if (isSeeking) seekFraction else progressFraction
    val displayPosition = if (isSeeking) (seekFraction * duration).toLong() else media.position

    val cardShape = RoundedCornerShape(16.dp)

    Surface(
        shape = cardShape,
        color = Color(0xFF0A0C0E),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        if (mediaCollapsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (media.albumArt != null) {
                    Image(
                        bitmap = media.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title.ifBlank { "UNKNOWN" }.uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            fontSize = 13.sp,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (media.artist.isNotBlank()) {
                        Text(
                            text = "// ${media.artist.uppercase()}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                letterSpacing = 0.8.sp,
                                fontSize = 11.sp,
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    onClick = {
                        onPlayPause()
                        haptics.lightTap()
                    },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Nothing Header Row: Red dot + "NOW PLAYING" + Output Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD71920)),
                        )
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 11.sp,
                            ),
                            color = Color.White.copy(alpha = 0.90f),
                        )
                    }

                    AudioOutputChip(
                        packageName = media.packageName,
                        theme = ShadeTheme.Nothing,
                    )
                }

                // Track Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.title.ifBlank { "UNKNOWN TITLE" }.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp,
                                fontSize = 14.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (media.artist.isNotBlank()) {
                            Text(
                                text = "// ${media.artist.uppercase()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            haptics.lightTap()
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color(0xFFD71920) else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Collapse",
                            tint = Color.White.copy(alpha = 0.80f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Nothing Wireframe Scrubber with Red Playhead Dot
                if (media.duration > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(media.duration) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    isSeeking = true
                                    seekFraction = (down.position.x / w).coerceIn(0f, 1f)
                                    haptics.lightTap()
                                    var lastStep = (seekFraction * 20).toInt()

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            val finalMs = (seekFraction * duration).toLong()
                                            haptics.sheetDetent()
                                            onSeek(finalMs)
                                            isSeeking = false
                                            break
                                        }
                                        change.consume()
                                        seekFraction = (change.position.x / w).coerceIn(0f, 1f)
                                        val step = (seekFraction * 20).toInt()
                                        if (step != lastStep) {
                                            haptics.lightTap()
                                            lastStep = step
                                        }
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // Inactive line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .background(Color.White.copy(alpha = 0.20f))
                            )
                            // Active line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = effectiveFraction.coerceIn(0f, 1f))
                                    .height(2.5.dp)
                                    .background(Color.White.copy(alpha = 0.90f))
                            )
                            // Signature Nothing Red Playhead Dot
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = effectiveFraction.coerceIn(0f, 1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD71920))
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = formatMs(displayPosition),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                                color = Color.White.copy(alpha = 0.60f),
                            )
                            Text(
                                text = formatMs(media.duration),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                                color = Color.White.copy(alpha = 0.60f),
                            )
                        }
                    }
                }

                // Nothing Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(
                        onClick = {
                            onSkipPrevious()
                            haptics.lightTap()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Surface(
                        onClick = {
                            onPlayPause()
                            haptics.lightTap()
                        },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (media.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onSkipNext()
                            haptics.lightTap()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cyberpunk 2077 HUD Media Player Card.
 * High-tech aesthetic with chamfered geometry, glowing neon cyan/magenta borders,
 * monospace bracketed telemetry, and cyber visualizer.
 */
@Composable
fun CyberpunkMediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    mediaCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    var isLiked by remember(media.title + media.artist) { mutableStateOf(false) }

    val duration = media.duration.coerceAtLeast(1L)
    val progressFraction = (media.position.toFloat() / duration).coerceIn(0f, 1f)
    var isSeeking by remember { mutableStateOf(false) }
    var seekFraction by remember { mutableFloatStateOf(0f) }
    val effectiveFraction = if (isSeeking) seekFraction else progressFraction
    val displayPosition = if (isSeeking) (seekFraction * duration).toLong() else media.position

    val cardShape = ChamferedCornerShape(12.dp)

    Surface(
        shape = cardShape,
        color = Color(0xFF080C14),
        border = BorderStroke(1.5.dp, Color(0xFF00F0FF)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        if (mediaCollapsed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (media.albumArt != null) {
                    Image(
                        bitmap = media.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(ChamferedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF00F0FF), ChamferedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "[TRK // ${media.title.ifBlank { "ONLINE" }}]",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        ),
                        color = Color(0xFF00F0FF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (media.artist.isNotBlank()) {
                        Text(
                            text = "[ART // ${media.artist}]",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            ),
                            color = Color(0xFFFF007F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    onClick = {
                        onPlayPause()
                        haptics.sliderTick()
                    },
                    shape = ChamferedCornerShape(6.dp),
                    color = Color(0xFF00F0FF),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Cyberpunk Header: Telemetry + Output Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "[SYS.AUDIO // STREAM: ONLINE]",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = Color(0xFF00F0FF),
                    )

                    AudioOutputChip(
                        packageName = media.packageName,
                        theme = ShadeTheme.Cyberpunk,
                    )
                }

                // Track Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(ChamferedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF00F0FF), ChamferedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "[TRK // ${media.title.ifBlank { "UNKNOWN" }}]",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                ),
                                color = Color(0xFF00F0FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            AudioEqualizerVisualizer(
                                isPlaying = media.isPlaying,
                                color = Color(0xFFFF007F),
                            )
                        }
                        if (media.artist.isNotBlank()) {
                            Text(
                                text = "[ART // ${media.artist}]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                ),
                                color = Color(0xFFFF007F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            isLiked = !isLiked
                            haptics.sliderTick()
                        },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color(0xFFFF007F) else Color(0xFF00F0FF).copy(alpha = 0.70f),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "Collapse",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Cyberpunk Segmented Neon Progress Bar
                if (media.duration > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(media.duration) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    isSeeking = true
                                    seekFraction = (down.position.x / w).coerceIn(0f, 1f)
                                    haptics.sliderTick()
                                    var lastStep = (seekFraction * 20).toInt()

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            val finalMs = (seekFraction * duration).toLong()
                                            haptics.sheetDetent()
                                            onSeek(finalMs)
                                            isSeeking = false
                                            break
                                        }
                                        change.consume()
                                        seekFraction = (change.position.x / w).coerceIn(0f, 1f)
                                        val step = (seekFraction * 20).toInt()
                                        if (step != lastStep) {
                                            haptics.sliderTick()
                                            lastStep = step
                                        }
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(ChamferedCornerShape(2.dp))
                                .background(Color(0xFF141F30)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = effectiveFraction.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(ChamferedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF00F0FF),
                                                Color(0xFFFF007F),
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "[POS: ${formatMs(displayPosition)}]",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                ),
                                color = Color(0xFF00F0FF).copy(alpha = 0.80f),
                            )
                            Text(
                                text = "[DUR: ${formatMs(media.duration)}]",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                ),
                                color = Color(0xFFFF007F).copy(alpha = 0.80f),
                            )
                        }
                    }
                }

                // Cyberpunk Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Surface(
                        onClick = {
                            onSkipPrevious()
                            haptics.sliderTick()
                        },
                        shape = ChamferedCornerShape(6.dp),
                        color = Color(0xFF0D1424),
                        border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            onPlayPause()
                            haptics.sliderTick()
                        },
                        shape = ChamferedCornerShape(8.dp),
                        color = Color(0xFF00F0FF),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (media.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            onSkipNext()
                            haptics.sliderTick()
                        },
                        shape = ChamferedCornerShape(6.dp),
                        color = Color(0xFF0D1424),
                        border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                        modifier = Modifier.size(42.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color(0xFF00F0FF),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pure Material 3 Media Player Card.
 */
@Composable
fun PureMaterialMediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    mediaCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val cardShape = RoundedCornerShape(24.dp)

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = getCardBorder(alpha = 0.25f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AudioOutputChip(
                    packageName = media.packageName,
                    theme = ShadeTheme.PureMaterial,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (media.albumArt != null) {
                    Image(
                        bitmap = media.albumArt.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title.ifBlank { "Unknown Title" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (media.artist.isNotBlank()) {
                        Text(
                            text = media.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (media.duration > 0) {
                var isSeeking by remember { mutableStateOf(false) }
                var seekPreview by remember { mutableFloatStateOf(media.position.toFloat()) }
                LaunchedEffect(media.position) {
                    if (!isSeeking) seekPreview = media.position.toFloat()
                }

                Slider(
                    value = if (isSeeking) seekPreview else media.position.toFloat().coerceIn(0f, media.duration.toFloat()),
                    onValueChange = {
                        seekPreview = it
                        isSeeking = true
                    },
                    onValueChangeFinished = {
                        haptics.sheetDetent()
                        onSeek(seekPreview.toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..media.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = onSkipPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                Surface(
                    onClick = {
                        onPlayPause()
                        haptics.sheetDetent()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}

/**
 * Master multi-theme Media Card.
 * Dynamically switches between Samsung One UI 8.5/9, Google Pixel (squiggly seekbar),
 * Nothing OS (monochrome dot-matrix / glyph red), Cyberpunk 2077 HUD (chamfered neon),
 * and Pure Material 3 based on [LocalShadeTheme].
 */
@Composable
fun MediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    mediaCollapsed: Boolean = false,
    onToggleCollapse: () -> Unit = {},
) {
    val theme = LocalShadeTheme.current
    when (theme) {
        is ShadeTheme.Pixel -> {
            PixelMediaCard(
                media = media,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeek = onSeek,
                modifier = modifier,
                mediaCollapsed = mediaCollapsed,
                onToggleCollapse = onToggleCollapse,
            )
        }
        is ShadeTheme.Nothing -> {
            NothingMediaCard(
                media = media,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeek = onSeek,
                modifier = modifier,
                mediaCollapsed = mediaCollapsed,
                onToggleCollapse = onToggleCollapse,
            )
        }
        is ShadeTheme.Cyberpunk -> {
            CyberpunkMediaCard(
                media = media,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeek = onSeek,
                modifier = modifier,
                mediaCollapsed = mediaCollapsed,
                onToggleCollapse = onToggleCollapse,
            )
        }
        is ShadeTheme.PureMaterial -> {
            PureMaterialMediaCard(
                media = media,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeek = onSeek,
                modifier = modifier,
                mediaCollapsed = mediaCollapsed,
                onToggleCollapse = onToggleCollapse,
            )
        }
        is ShadeTheme.OneUI -> {
            OneUi9MediaCard(
                media = media,
                onPlayPause = onPlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onSeek = onSeek,
                modifier = modifier,
                mediaCollapsed = mediaCollapsed,
                onToggleCollapse = onToggleCollapse,
            )
        }
    }
}
