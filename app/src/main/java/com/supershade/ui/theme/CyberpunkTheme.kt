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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cyberpunk / Neon Synthwave high-contrast electric palette
private val CyberpunkDarkColors = darkColorScheme(
    background             = Color(0xF5080A10),
    surface                = Color(0xF80E101B),
    surfaceVariant         = Color(0xFF1B1E30),
    surfaceContainerLowest = Color(0xFF040509),
    surfaceContainerLow    = Color(0xFF0B0D16),
    surfaceContainer       = Color(0xFF131624),
    surfaceContainerHigh   = Color(0xFF1D2136),
    surfaceContainerHighest = Color(0xFF282E49),
    primary                = Color(0xFF00F0FF), // Electric Neon Cyan
    primaryContainer       = Color(0xFF003845),
    onPrimary              = Color(0xFF001A20),
    onPrimaryContainer     = Color(0xFFBEF7FF),
    secondary              = Color(0xFFFF0055), // Hot Neon Pink
    secondaryContainer     = Color(0xFF4A0018),
    onSecondary            = Color.White,
    onSecondaryContainer   = Color(0xFFFFD9E2),
    tertiary               = Color(0xFFFFE600), // Cyber Yellow
    onTertiary             = Color(0xFF2E2A00),
    onBackground           = Color(0xFFF0F4FC),
    onSurface              = Color(0xFFF0F4FC),
    onSurfaceVariant       = Color(0xFFA5B2D1),
    outline                = Color(0xFF00F0FF),
    outlineVariant         = Color(0xFF2B3252),
    error                  = Color(0xFFFF003C),
    onError                = Color.White,
)

private val CyberpunkAmoledColors = CyberpunkDarkColors.copy(
    background             = Color.Black,
    surface                = Color(0xFF030408),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow    = Color(0xFF06070E),
    surfaceContainer       = Color(0xFF0B0E18),
    surfaceContainerHigh   = Color(0xFF141828),
    surfaceContainerHighest = Color(0xFF1E233A),
    surfaceVariant         = Color(0xFF101322),
    outline                = Color(0xFF00F0FF),
    outlineVariant         = Color(0xFF1C223C),
)

private val CyberpunkLightColors = lightColorScheme(
    background             = Color(0xF5F0F4F8),
    surface                = Color(0xFAF8FAFD),
    surfaceVariant         = Color(0xFFDDE3EC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFEFF3F8),
    surfaceContainer       = Color(0xFFE3E9F2),
    surfaceContainerHigh   = Color(0xFFD7DFEB),
    surfaceContainerHighest = Color(0xFFCBD5E4),
    primary                = Color(0xFF007A87),
    primaryContainer       = Color(0xFFBCEEF5),
    onPrimary              = Color.White,
    onPrimaryContainer     = Color(0xFF002024),
    secondary              = Color(0xFFD80046),
    secondaryContainer     = Color(0xFFFFD9E2),
    onSecondary            = Color.White,
    onSecondaryContainer   = Color(0xFF3F0012),
    onBackground           = Color(0xFF0C1017),
    onSurface              = Color(0xFF0C1017),
    onSurfaceVariant       = Color(0xFF454E5E),
    outline                = Color(0xFF007A87),
    outlineVariant         = Color(0xFFBDC7D6),
    error                  = Color(0xFFBA1A1A),
    onError                = Color.White,
)

private val CyberpunkTypography = Typography(
    displayMedium = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 48.sp,
        lineHeight    = 52.sp,
        letterSpacing = 0.5.sp,
        fontFamily    = FontFamily.SansSerif,
    ),
    headlineSmall = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.3.sp,
    ),
    titleMedium = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 15.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    titleSmall = TextStyle(
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
    ),
    bodySmall = TextStyle(
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
    ),
    labelMedium = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 11.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

// Sharp futuristic corners
private val CyberpunkShapes = Shapes(
    small      = RoundedCornerShape(6.dp),
    medium     = RoundedCornerShape(10.dp),
    large      = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp),
)

@Composable
fun CyberpunkShadeTheme(
    isAmoled: Boolean = false,
    darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    accentColor: com.supershade.settings.AccentColor = com.supershade.settings.AccentColor.MONET,
    monetBlendStrength: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val isSystemInDark = isSystemInDarkTheme()
    val isDark = when (darkThemeMode) {
        DarkThemeMode.SYSTEM -> isSystemInDark
        DarkThemeMode.DARK, DarkThemeMode.AMOLED -> true
        DarkThemeMode.LIGHT -> false
    }

    val base = when {
        isAmoled -> CyberpunkAmoledColors
        isDark -> CyberpunkDarkColors
        else -> CyberpunkLightColors
    }

    val colorScheme = if (accentColor != com.supershade.settings.AccentColor.MONET) {
        val c = Color(accentColor.hex)
        base.copy(
            primary = c,
            primaryContainer = c.copy(alpha = if (isDark) 0.35f else 0.22f),
            onPrimary = Color.Black,
            onPrimaryContainer = if (isDark) Color.White else c,
        )
    } else {
        monetScheme(base, isDark, if (isAmoled) 0f else 0.25f, monetBlendStrength)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CyberpunkTypography,
        shapes      = CyberpunkShapes,
        content     = content,
    )
}
