package com.supershade.ui.shade

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.supershade.ui.theme.LocalShadeShapeScheme
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

    fun updateValueFromFraction(newFraction: Float) {
        val clamped = newFraction.coerceIn(0f, 1f)
        val newInt = (1f + clamped * 254f).roundToInt().coerceIn(1, 255)
        if (newInt / 16 != localValue.roundToInt() / 16) {
            haptics.sliderTick()
        }
        localValue = newInt.toFloat()
        onBrightnessChange(newInt)
    }

    val fillGradient = Brush.horizontalGradient(
        if (isAuto) {
            listOf(
                Color(0xFFFFA000).copy(alpha = 0.55f),
                Color(0xFFFFD54F).copy(alpha = 0.55f),
            )
        } else {
            listOf(
                Color(0xFFFFA000),
                Color(0xFFFFD54F),
            )
        }
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

        // Main Tactile Slider Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(shapes.slider)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = shapes.slider,
                )
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
                            haptics.sliderTick()
                            onBrightnessChange(localValue.roundToInt().coerceIn(1, 255))
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
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Sun icon on the left
                Icon(
                    imageVector = sunIcon,
                    contentDescription = null,
                    tint = if (fraction > 0.18f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )

                // Percentage indicator or Auto badge on the right
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (isAuto) "Auto ${(fraction * 100).roundToInt()}%" else "${(fraction * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = if (fraction > 0.85f) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Auto Toggle Pill Button
        IconButton(
            onClick = { toggleAuto() },
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(autoBg)
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
