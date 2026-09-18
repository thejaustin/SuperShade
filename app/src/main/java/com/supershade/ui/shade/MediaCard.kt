package com.supershade.ui.shade

import android.media.session.PlaybackState
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import androidx.palette.graphics.Palette
import com.supershade.domain.media.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
private fun AudioEqualizerVisualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(530, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bar3",
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(470, easing = FastOutSlowInEasing),
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

@Composable
private fun AudioOutputChip(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val currentOutput = remember(audioManager, packageName) {
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
                btDevice?.productName?.toString() ?: "Phone Speaker"
            } else {
                "Phone Speaker"
            }
        } catch (_: Exception) {
            "Phone Speaker"
        }
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
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = modifier.height(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = currentOutput,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    val fallbackDark = MaterialTheme.colorScheme.surfaceContainerHighest

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

    val animatedBg by animateColorAsState(
        targetValue = dominantColor.copy(alpha = 0.95f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MediaBg",
    )
    val animatedAccent by animateColorAsState(
        targetValue = lightAccent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MediaAccent",
    )

    // Local liked state per-track (persists until track changes)
    var isLiked by remember(media.title + media.artist) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
            .background(animatedBg)
            .animateContentSize(),
    ) {
        // Full-bleed album art background (blurred, low alpha) when art is available
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

        // Foreground scrim gradient for readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f),
                        )
                    )
                )
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // Album art + title/artist + transport controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Album art thumbnail with nice shadow
                media.albumArt?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(14.dp))
                }

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = media.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        AudioEqualizerVisualizer(
                            isPlaying = media.isPlaying,
                            color = animatedAccent.let { if (it == fallbackColor) Color.White else it },
                        )
                    }
                    if (media.artist.isNotBlank()) {
                        Text(
                            text = media.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (media.album.isNotBlank() && media.album != media.title) {
                        Text(
                            text = media.album,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Audio Output Switcher + Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AudioOutputChip(packageName = media.packageName)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (isLiked) 0.25f else 0.08f))
                            .clickable {
                                isLiked = !isLiked
                                if (isLiked) haptics.tileToggleOn() else haptics.tileToggleOff()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val likeScale by animateFloatAsState(
                            targetValue = if (isLiked) 1.2f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh,
                            ),
                            label = "likeScale",
                        )
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color(0xFFFF5C8D) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { scaleX = likeScale; scaleY = likeScale },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Transport controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Skip previous
                IconButton(
                    onClick = {
                        onSkipPrevious()
                        haptics.sliderTick()
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp),
                    )
                }

                // Play/Pause — large filled circle button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(animatedAccent.copy(alpha = 0.9f).let { c ->
                            // Ensure it's bright enough against the dark bg
                            if (c == fallbackColor) Color.White.copy(alpha = 0.9f) else c
                        })
                        .clickable {
                            onPlayPause()
                            if (!media.isPlaying) haptics.tileToggleOn() else haptics.tileToggleOff()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (media.isPlaying) "Pause" else "Play",
                        tint = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Skip next
                IconButton(
                    onClick = {
                        onSkipNext()
                        haptics.sliderTick()
                    },
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Seek bar with timestamps
            if (media.duration > 0) {
                Spacer(Modifier.height(4.dp))
                var isSeeking by remember { mutableStateOf(false) }
                var seekPreview by remember {
                    mutableFloatStateOf(media.position.toFloat())
                }
                LaunchedEffect(media.position) {
                    if (!isSeeking) {
                        seekPreview = media.position.toFloat()
                    }
                }
                val displayPosition = if (isSeeking) seekPreview.toLong() else media.position

                Slider(
                    value = if (isSeeking) seekPreview else media.position.toFloat().coerceIn(0f, media.duration.toFloat()),
                    onValueChange = { seekPreview = it; isSeeking = true },
                    onValueChangeFinished = {
                        haptics.sliderTick()
                        onSeek(seekPreview.toLong())
                        isSeeking = false
                    },
                    valueRange = 0f..media.duration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.9f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMs(displayPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Text(
                        text = formatMs(media.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
            } else if (media.isPlaying) {
                // Live stream / continuous broadcast indicator
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
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
        }
    }
}
