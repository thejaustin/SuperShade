package com.supershade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Authentic Android 15/16 Material 3 Expressive light palette
private val PixelLightColors = lightColorScheme(
    background = Color(0xF2F7F9FC),
    surface = Color(0xFAF8FAFC),
    surfaceVariant = Color(0xFFDEE3EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F9),
    surfaceContainer = Color(0xFFE9EDF4),
    surfaceContainerHigh = Color(0xFFE2E7EE),
    surfaceContainerHighest = Color(0xFFDCE1E9),
    primary = Color(0xFF0061A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251432),
    onBackground = Color(0xFF181C20),
    onSurface = Color(0xFF181C20),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7D0),
)

// Authentic Google Pixel Android 15/16 Material 3 Expressive dark shade palette
private val PixelColors = darkColorScheme(
    background = Color(0xE6111318),
    surface = Color(0xEB13151B),
    surfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFF0B0D12),
    surfaceContainerLow = Color(0xFF13151B),
    surfaceContainer = Color(0xFF191C22),
    surfaceContainerHigh = Color(0xFF23262D),
    surfaceContainerHighest = Color(0xFF2E3138),
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF063259),
    primaryContainer = Color(0xFF08427B),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBCC7D8),
    onSecondary = Color(0xFF273140),
    secondaryContainer = Color(0xFF3D4757),
    onSecondaryContainer = Color(0xFFD8E3F5),
    tertiary = Color(0xFFD8BDE6),
    onTertiary = Color(0xFF3C294A),
    tertiaryContainer = Color(0xFF533F60),
    onTertiaryContainer = Color(0xFFF2DAFF),
    onBackground = Color(0xFFE2E2E8),
    onSurface = Color(0xFFE2E2E8),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
)

private val PixelAmoledColors = PixelColors.copy(
    background = Color(0xF5000000),
    surface = Color(0xF8000000),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF08090C),
    surfaceContainer = Color(0xFF111317),
    surfaceContainerHigh = Color(0xFF191B20),
    surfaceContainerHighest = Color(0xFF22252C),
    surfaceVariant = Color(0xFF1B1D22),
    outline = Color(0xFF2E3136),
    outlineVariant = Color(0xFF222428),
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
    darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    accentColor: com.supershade.settings.AccentColor = com.supershade.settings.AccentColor.GALAXY_BLUE,
    content: @Composable () -> Unit,
) {
    val isSystemInDark = isSystemInDarkTheme()
    val isDark = when (darkThemeMode) {
        DarkThemeMode.SYSTEM -> isSystemInDark
        DarkThemeMode.DARK, DarkThemeMode.AMOLED -> true
        DarkThemeMode.LIGHT -> false
    }

    val base = when {
        isAmoled -> PixelAmoledColors
        isDark -> PixelColors
        else -> PixelLightColors
    }

    val colorScheme = if (accentColor != com.supershade.settings.AccentColor.MONET) {
        val c = Color(accentColor.hex)
        base.copy(
            primary = c,
            primaryContainer = c.copy(alpha = if (isDark) 0.35f else 0.20f),
            onPrimary = Color.White,
            onPrimaryContainer = if (isDark) Color.White else c,
        )
    } else {
        monetScheme(base, isDark, if (isAmoled) 0f else 0.85f)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content,
    )
}
