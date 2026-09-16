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
    background        = Color(0xF5101114),   // 96% deep frosted
    surface           = Color(0xFA121418),   // 98% deep crisp frosted surface
    surfaceVariant    = Color(0xFF252830),
    surfaceContainerLowest = Color(0xFF0C0D0F),
    surfaceContainerLow = Color(0xFF15171B),
    surfaceContainer  = Color(0xFF1D2026),   // Island container cards
    surfaceContainerHigh = Color(0xFF272A32),
    surfaceContainerHighest = Color(0xFF323640),
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
    background = Color(0xF8000000),
    surface = Color(0xFC000000),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF08090C),
    surfaceContainer = Color(0xFF111317),   // Deep graphite island cards
    surfaceContainerHigh = Color(0xFF1A1C22),
    surfaceContainerHighest = Color(0xFF242730),
    surfaceVariant = Color(0xFF181A20),
    outline = Color(0xFF2E313B),
    outlineVariant = Color(0xFF22242C),
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

// Samsung One UI squircle curve standard (24dp rounded cards, 28dp extra large)
private val OneUiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun OneUiShadeTheme(
    isAmoled: Boolean = false,
    accentColor: com.supershade.settings.AccentColor = com.supershade.settings.AccentColor.GALAXY_BLUE,
    content: @Composable () -> Unit,
) {
    val base = if (isAmoled) OneUiAmoledColors else OneUiColors
    val colorScheme = if (accentColor != com.supershade.settings.AccentColor.MONET) {
        val c = Color(accentColor.hex)
        base.copy(
            primary = c,
            primaryContainer = c.copy(alpha = 0.35f),
            onPrimary = Color.White,
            onPrimaryContainer = Color.White,
        )
    } else {
        base
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = OneUiTypography,
        shapes      = OneUiShapes,
        content     = content,
    )
}
