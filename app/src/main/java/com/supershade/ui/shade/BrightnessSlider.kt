package com.supershade.ui.shade

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
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
import kotlin.math.roundToInt

@Composable
fun BrightnessSlider(
    brightness: Int,
    onBrightnessChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { SuperHaptics(context) }

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
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    newMode,
                )
                isAuto = !isAuto
                if (isAuto) haptics.tileToggleOn() else haptics.tileToggleOff()
            } catch (_: SecurityException) {}
        } else {
            try {
                val intent = android.content.Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(
                        android.content.Intent(Settings.ACTION_DISPLAY_SETTINGS)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {}
            }
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var localValue by remember { mutableFloatStateOf(brightness.toFloat().coerceIn(1f, 255f)) }

    LaunchedEffect(brightness) {
        if (!isDragging) {
            localValue = brightness.toFloat().coerceIn(1f, 255f)
        }
    }

    val fraction = ((localValue - 1f) / 254f).coerceIn(0f, 1f)
    val animatedProgressFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh) else spring(dampingRatio = 0.82f, stiffness = 450f),
        label = "animatedBrightnessFraction",
    )

    val sunIcon = when {
        fraction < 0.33f -> Icons.Default.BrightnessLow
        fraction < 0.67f -> Icons.Default.BrightnessMedium
        else             -> Icons.Default.BrightnessHigh
    }

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
        val newInt = (1f + clamped * 254f).roundToInt().coerceIn(1, 255)
        if (newInt / 16 != localValue.roundToInt() / 16) {
            haptics.sliderTick()
        }
        localValue = newInt.toFloat()
        onBrightnessChange(newInt)
    }

    val shadeTheme = LocalShadeTheme.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val fillGradient = remember(shadeTheme, primaryColor, secondaryColor, isAuto) {
        val baseColors = when (shadeTheme) {
            is ShadeTheme.Cyberpunk -> listOf(primaryColor, secondaryColor)
            is ShadeTheme.Nothing -> listOf(primaryColor, primaryColor.copy(alpha = 0.85f))
            is ShadeTheme.Pixel, is ShadeTheme.PureMaterial -> listOf(primaryColor, primaryColor)
            is ShadeTheme.OneUI -> listOf(
                Color(0xFFF57C00),
                Color(0xFFFF9800),
                Color(0xFFFFCA28),
            )
        }
        val alpha = if (isAuto) 0.60f else 1.0f
        Brush.horizontalGradient(baseColors.map { it.copy(alpha = alpha) })
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
            label = "brightnessSliderScaleY",
        )
        val iconScale by animateFloatAsState(
            targetValue = if (isDragging) 1.15f else 1.0f,
            animationSpec = spring(dampingRatio = 0.60f, stiffness = 800f),
            label = "brightnessIconScale",
        )
        val iconRotation by animateFloatAsState(
            targetValue = (fraction - 0.5f) * 24f,
            animationSpec = spring(dampingRatio = 0.70f, stiffness = 750f),
            label = "brightnessIconRotation",
        )

        // Main Tactile Slider Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(sliderHeight)
                .graphicsLayer { scaleY = trackScaleY }
                .clip(sliderShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .then(if (border != null) Modifier.border(border, sliderShape) else Modifier)
                .semantics {
                    contentDescription = "Screen brightness"
                    stateDescription = if (isAuto) "Auto ${(fraction * 100).roundToInt()}%" else "${(fraction * 100).roundToInt()}%"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = localValue,
                        range = 1f..255f,
                        steps = 0,
                    )
                    setProgress { targetValue ->
                        val clamped = targetValue.coerceIn(1f, 255f)
                        localValue = clamped
                        onBrightnessChange(clamped.roundToInt())
                        true
                    }
                }
                .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(isAuto) {
                    detectTapGestures { offset ->
                        haptics.sliderTick()
                        updateValueFromFraction(offset.x / trackWidthPx)
                    }
                }
                .pointerInput(isAuto) {
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
                            onBrightnessChange(localValue.roundToInt().coerceIn(1, 255))
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
                                .background(Color(0xFF00F0FF).copy(alpha = 0.50f))
                        )
                    }
                }
            }

            // Embedded Content Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Sun icon on the left
                Icon(
                    imageVector = sunIcon,
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

                // Percentage indicator and One UI integrated Auto badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = when (shadeTheme) {
                            is ShadeTheme.Cyberpunk -> if (isAuto) "AUTO // ${(fraction * 100).roundToInt()}%" else "LUM // ${(fraction * 100).roundToInt()}%"
                            is ShadeTheme.Nothing -> if (isAuto) "AUTO ${(fraction * 100).roundToInt()}%" else "${(fraction * 100).roundToInt()}%"
                            is ShadeTheme.OneUI -> "${(fraction * 100).roundToInt()}%"
                            else -> if (isAuto) "Auto ${(fraction * 100).roundToInt()}%" else "${(fraction * 100).roundToInt()}%"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                            letterSpacing = if (shadeTheme is ShadeTheme.Nothing) 1.sp else 0.sp,
                        ),
                        color = if (fraction > 0.82f) Color.White else MaterialTheme.colorScheme.onSurface,
                    )

                    // One UI 8.5/9 Integrated "A" Squircle Badge inside the track
                    if (shadeTheme is ShadeTheme.OneUI) {
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 24.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAuto) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.18f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = if (isAuto) 0.9f else 0.35f),
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable {
                                    haptics.sheetDetent()
                                    toggleAuto()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "A",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.sp,
                                ),
                                color = if (isAuto) Color(0xFFF57C00) else Color.White,
                            )
                        }
                    }
                }
            }
        }

        // Auto Toggle Pill Button for non-OneUI themes
        if (shadeTheme !is ShadeTheme.OneUI) {
            val autoBorder = if (isAuto) null else getCardBorder(alpha = 0.35f)
            IconButton(
                onClick = { toggleAuto() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(autoBg)
                    .then(if (autoBorder != null) Modifier.border(autoBorder, CircleShape) else Modifier)
                    .semantics {
                        role = Role.Switch
                        stateDescription = if (isAuto) "Auto brightness on" else "Auto brightness off"
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessAuto,
                    contentDescription = if (isAuto) "Disable auto brightness" else "Enable auto brightness",
                    tint = autoIconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
