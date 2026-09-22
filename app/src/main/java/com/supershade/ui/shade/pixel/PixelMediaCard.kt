package com.supershade.ui.shade.pixel

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.domain.media.MediaState
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.getCardBorder
import kotlin.math.sin

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

/**
 * Pixel (Material You / Android 15/16) Media Player Card.
 * Features the signature squiggly sinusoidal wavy seekbar that undulates while
 * music is playing and straightens when paused.
 */
@Composable
fun PixelMediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

    val duration = media.duration.coerceAtLeast(1L)
    val progressFraction = (media.position.toFloat() / duration).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "wavySeekbar")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (media.isPlaying) 6.28318f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = getCardBorder(alpha = 0.25f),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Track Info & Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Album Art (Rounded Squircle 56dp)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Title & Artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = media.title.ifEmpty { "Unknown Title" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = media.artist.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Skip Prev, Play/Pause, Skip Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            haptics.lightTap()
                            onSkipPrevious()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Play/Pause circular button
                    Surface(
                        onClick = {
                            haptics.sheetDetent()
                            onPlayPause()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (media.isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptics.lightTap()
                            onSkipNext()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Material You Squiggly Wavy Seekbar
            val primaryColor = MaterialTheme.colorScheme.primary
            val unplayedColor = MaterialTheme.colorScheme.surfaceContainerHighest

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(media.duration) {
                        detectTapGestures { offset ->
                            val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val targetMs = (newFraction * duration).toLong()
                            haptics.lightTap()
                            onSeek(targetMs)
                        }
                    },
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val activeEnd = w * progressFraction

                    // Draw Unplayed Track (straight line)
                    if (activeEnd < w) {
                        drawLine(
                            color = unplayedColor,
                            start = androidx.compose.ui.geometry.Offset(activeEnd, centerY),
                            end = androidx.compose.ui.geometry.Offset(w, centerY),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }

                    // Draw Played Track (Wavy when isPlaying, straight when paused)
                    if (activeEnd > 0f) {
                        if (media.isPlaying) {
                            val path = Path()
                            path.moveTo(0f, centerY)
                            val wavelength = 36.dp.toPx()
                            val amplitude = 3.5.dp.toPx()
                            var x = 0f
                            val step = 3f
                            while (x <= activeEnd) {
                                val y = centerY + amplitude * sin((x / wavelength) * 6.28318f - wavePhase)
                                path.lineTo(x, y)
                                x += step
                            }
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            )
                        } else {
                            drawLine(
                                color = primaryColor,
                                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                end = androidx.compose.ui.geometry.Offset(activeEnd, centerY),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(media.position),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatTime(media.duration),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
