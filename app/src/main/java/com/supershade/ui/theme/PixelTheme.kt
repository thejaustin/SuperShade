package com.supershade.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Authentic Google Pixel Material You dark shade palette
private val PixelColors = darkColorScheme(
    background = Color(0xF2191C1E),
    surface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFF41474D),
    surfaceContainerLowest = Color(0xFF0C0E10),
    surfaceContainerLow = Color(0xFF171A1C),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2E),
    surfaceContainerHighest = Color(0xFF333539),
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF41474D),
)

private val PixelAmoledColors = PixelColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF08090A),
    surfaceContainer = Color(0xFF101214),
    surfaceContainerHigh = Color(0xFF181A1D),
    surfaceContainerHighest = Color(0xFF222428),
    surfaceVariant = Color(0xFF1E2024),
    outline = Color(0xFF2E3136),
)

private val PixelTypography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    ),
)

private val PixelShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PixelShadeTheme(
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isAmoled) PixelAmoledColors else PixelColors,
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content,
    )
}
