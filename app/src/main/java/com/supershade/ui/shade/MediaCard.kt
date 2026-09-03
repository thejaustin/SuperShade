package com.supershade.ui.shade

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.supershade.domain.media.MediaState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
fun MediaCard(
    media: MediaState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
) {
    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant

    val dominantColor by produceState(initialValue = fallbackColor, key1 = media.albumArt) {
        value = if (media.albumArt == null) {
            fallbackColor
        } else {
            withContext(Dispatchers.IO) {
                val palette = Palette.from(media.albumArt).generate()
                val argb = palette.getMutedColor(
                    palette.getVibrantColor(fallbackColor.value.toInt())
                )
                Color(argb)
            }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "MediaCardGradientColor",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        animatedColor.copy(alpha = 0.9f),
                        animatedColor.copy(alpha = 0.4f),
                    )
                )
            ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Album art — 56 dp, 12 dp corner radius
                media.albumArt?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Album art",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(14.dp))
                }

                // Title + artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleSmall,
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

                // Transport controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (media.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (media.duration > 0) {
                LinearProgressIndicator(
                    progress = {
                        (media.position.toFloat() / media.duration.toFloat()).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 4.dp),
                    color = Color.White.copy(alpha = 0.8f),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatMs(media.position),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = formatMs(media.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}
