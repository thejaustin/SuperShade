package com.supershade.ui.shade.pixel

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.ui.theme.getCardBorder
import kotlin.math.roundToInt

/**
 * Android 15/16 Material Expressive (Pixel) Brightness Slider.
 * 56dp thick stadium pill with an integrated Sun icon inside the left track.
 */
@Composable
fun PixelBrightnessSlider(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }
    val fraction = (brightness / 255f).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(fraction) }

    val currentFraction = if (isDragging) dragFraction else fraction

    val animatedFraction by animateFloatAsState(
        targetValue = currentFraction,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "pixelBrightnessFraction",
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "pixelSliderScale",
    )

    val pillShape = RoundedCornerShape(28.dp)

    val sunIcon = when {
        currentFraction < 0.33f -> Icons.Default.BrightnessLow
        currentFraction < 0.67f -> Icons.Default.BrightnessMedium
        else -> Icons.Default.BrightnessHigh
    }

    Surface(
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = getCardBorder(alpha = 0.25f),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(pillShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    haptics.sliderTick()
                    val newFrac = (down.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    dragFraction = newFrac
                    onBrightnessChange((newFrac * 255).roundToInt())

                    var lastStep = (newFrac * 10).toInt()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        change.consume()
                        val movedFrac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        dragFraction = movedFrac

                        val currentStep = (movedFrac * 10).toInt()
                        if (currentStep != lastStep) {
                            haptics.lightTap()
                            lastStep = currentStep
                        }

                        onBrightnessChange((movedFrac * 255).roundToInt())
                    }

                    isDragging = false
                    haptics.sliderTick()
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(currentFraction, 0f..1f)
                stateDescription = "${(currentFraction * 100).roundToInt()}% brightness"
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Active filled track (accent colored)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .clip(pillShape)
                    .background(MaterialTheme.colorScheme.primary),
            )

            // Content inside the track
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sun Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (animatedFraction > 0.15f) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.20f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = sunIcon,
                        contentDescription = "Brightness",
                        tint = if (animatedFraction > 0.15f) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Percentage indicator when dragging
                if (isDragging) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "${(currentFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = if (animatedFraction > 0.85f) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
