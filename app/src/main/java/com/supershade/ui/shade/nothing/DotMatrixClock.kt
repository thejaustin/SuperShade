package com.supershade.ui.shade.nothing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 3x5 matrix bitmask for digits 0-9
private val DIGIT_MATRICES = mapOf(
    '0' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '1' to arrayOf(
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(1, 1, 1),
    ),
    '2' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
    ),
    '3' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '4' to arrayOf(
        intArrayOf(1, 0, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 0, 1),
    ),
    '5' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '6' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 0),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '7' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 1, 0),
    ),
    '8' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
    ),
    '9' to arrayOf(
        intArrayOf(1, 1, 1),
        intArrayOf(1, 0, 1),
        intArrayOf(1, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(1, 1, 1),
    ),
)

@Composable
fun DotMatrixDigit(
    char: Char,
    activeColor: Color = MaterialTheme.colorScheme.onBackground,
    inactiveColor: Color = activeColor.copy(alpha = 0.08f),
    dotSize: Dp = 4.5.dp,
    dotSpacing: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    val matrix = DIGIT_MATRICES[char] ?: return
    val rows = 5
    val cols = 3

    val totalWidth = (dotSize * cols) + (dotSpacing * (cols - 1))
    val totalHeight = (dotSize * rows) + (dotSpacing * (rows - 1))

    Canvas(modifier = modifier.size(width = totalWidth, height = totalHeight)) {
        val dotRadiusPx = (dotSize / 2).toPx()
        val spacingPx = dotSpacing.toPx()
        val diameterPx = dotSize.toPx()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isActive = matrix[r][c] == 1
                val cx = c * (diameterPx + spacingPx) + dotRadiusPx
                val cy = r * (diameterPx + spacingPx) + dotRadiusPx
                drawCircle(
                    color = if (isActive) activeColor else inactiveColor,
                    radius = dotRadiusPx,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}

@Composable
fun DotMatrixColon(
    activeColor: Color = MaterialTheme.colorScheme.onBackground,
    dotSize: Dp = 4.5.dp,
    dotSpacing: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    val rows = 5
    val totalHeight = (dotSize * rows) + (dotSpacing * (rows - 1))

    Canvas(modifier = modifier.size(width = dotSize, height = totalHeight)) {
        val dotRadiusPx = (dotSize / 2).toPx()
        val spacingPx = dotSpacing.toPx()
        val diameterPx = dotSize.toPx()

        val cy1 = 1 * (diameterPx + spacingPx) + dotRadiusPx
        val cy2 = 3 * (diameterPx + spacingPx) + dotRadiusPx
        drawCircle(color = activeColor, radius = dotRadiusPx, center = Offset(dotRadiusPx, cy1))
        drawCircle(color = activeColor, radius = dotRadiusPx, center = Offset(dotRadiusPx, cy2))
    }
}

@Composable
fun DotMatrixClockDisplay(
    timeString: String,
    activeColor: Color = MaterialTheme.colorScheme.onBackground,
    dotSize: Dp = 4.dp,
    dotSpacing: Dp = 2.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        timeString.forEach { ch ->
            when (ch) {
                ':' -> DotMatrixColon(activeColor = activeColor, dotSize = dotSize, dotSpacing = dotSpacing)
                in '0'..'9' -> DotMatrixDigit(char = ch, activeColor = activeColor, dotSize = dotSize, dotSpacing = dotSpacing)
            }
        }
    }
}

@Composable
fun NothingBatteryPill(
    batteryPct: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val isLow = batteryPct <= 20
    val activeColor = when {
        isCharging -> Color(0xFF4CAF50)
        isLow -> Color(0xFFD71920) // Nothing Glyph Red
        else -> Color.White
    }
    val inactiveColor = Color.White.copy(alpha = 0.15f)
    val totalDots = 10
    val filledDots = (batteryPct / 10).coerceIn(0, totalDots)

    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF141414),
        border = BorderStroke(1.dp, Color(0xFF2E2E2E)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Segmented dot bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(totalDots) { idx ->
                    val isFilled = idx < filledDots
                    Box(
                        modifier = Modifier
                            .size(3.5.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) activeColor else inactiveColor),
                    )
                }
            }

            // Text percentage
            Text(
                text = "$batteryPct%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                ),
                color = activeColor,
            )

            if (isCharging) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD71920)),
                )
            }
        }
    }
}
