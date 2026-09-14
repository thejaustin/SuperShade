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

// Samsung OneUI 8 dark shade palette
private val OneUiColors = darkColorScheme(
    background        = Color(0xE6121316),   // 90% opaque dark
    surface           = Color(0xFF1E2024),
    surfaceVariant    = Color(0xFF2B2E34),
    surfaceContainerLowest = Color(0xFF101114),
    surfaceContainerLow = Color(0xFF181A1D),
    surfaceContainer  = Color(0xFF222429),
    surfaceContainerHigh = Color(0xFF2C2F35),
    surfaceContainerHighest = Color(0xFF373A42),
    primary           = Color(0xFF2575FC),   // Samsung Galaxy Blue
    primaryContainer  = Color(0xFF104BBF),
    onPrimary         = Color.White,
    onPrimaryContainer = Color.White,
    secondary         = Color(0xFF8E95A5),
    secondaryContainer = Color(0xFF383D48),
    onSecondary       = Color.White,
    onSecondaryContainer = Color.White,
    onBackground      = Color(0xFFF2F4F8),
    onSurface         = Color(0xFFF2F4F8),
    onSurfaceVariant  = Color(0xFFA5ACB8),
    outline           = Color(0xFF3F444E),
    outlineVariant    = Color(0xFF2A2D34),
    error             = Color(0xFFFF5252),
    onError           = Color.White,
)

private val OneUiAmoledColors = OneUiColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0B0D),
    surfaceContainer = Color(0xFF141518),
    surfaceContainerHigh = Color(0xFF1C1E22),
    surfaceContainerHighest = Color(0xFF26282E),
    surfaceVariant = Color(0xFF1A1B20),
    outline = Color(0xFF2A2C33),
)

// Typography tuned to Samsung's visual weight hierarchy
private val OneUiTypography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Light,
        fontSize   = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 15.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
    ),
)

// Samsung One UI squircle curve standard (22dp rounded cards)
private val OneUiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun OneUiShadeTheme(
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isAmoled) OneUiAmoledColors else OneUiColors,
        typography  = OneUiTypography,
        shapes      = OneUiShapes,
        content     = content,
    )
}
