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

// Nothing OS Signature Dot-Matrix & Monochrome Palette
private val NothingColors = darkColorScheme(
    background             = Color(0xF5050505),
    surface                = Color(0xF80A0A0A),
    surfaceVariant         = Color(0xFF1C1C1E),
    surfaceContainerLowest = Color(0xFF040404),
    surfaceContainerLow    = Color(0xFF0F0F0F),
    surfaceContainer       = Color(0xFF171717),
    surfaceContainerHigh   = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    primary                = Color(0xFFD71920), // Signature Nothing Glyph Red
    primaryContainer       = Color(0xFF400B0E),
    onPrimary              = Color.White,
    onPrimaryContainer     = Color(0xFFFFD7D9),
    secondary              = Color(0xFFE4E4E4),
    secondaryContainer     = Color(0xFF333333),
    onSecondary            = Color.Black,
    onSecondaryContainer   = Color.White,
    onBackground           = Color(0xFFEDEDED),
    onSurface              = Color(0xFFEDEDED),
    onSurfaceVariant       = Color(0xFFA0A0A0),
    outline                = Color(0xFF3C3C3C),
    outlineVariant         = Color(0xFF252525),
    error                  = Color(0xFFFF453A),
    onError                = Color.White,
)

private val NothingAmoledColors = NothingColors.copy(
    background             = Color.Black,
    surface                = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow    = Color(0xFF080808),
    surfaceContainer       = Color(0xFF101010),
    surfaceContainerHigh   = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
    surfaceVariant         = Color(0xFF141414),
    outline                = Color(0xFF2C2C2C),
    outlineVariant         = Color(0xFF1E1E1E),
)

private val NothingLightColors = lightColorScheme(
    background             = Color(0xF5F6F6F8),
    surface                = Color(0xFAFBFBFD),
    surfaceVariant         = Color(0xFFE4E4E6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow    = Color(0xFFF2F2F4),
    surfaceContainer       = Color(0xFFEAEAEA),
    surfaceContainerHigh   = Color(0xFFE0E0E2),
    surfaceContainerHighest = Color(0xFFD6D6D8),
    primary                = Color(0xFFD71920),
    primaryContainer       = Color(0xFFFFE0E2),
    onPrimary              = Color.White,
    onPrimaryContainer     = Color(0xFF4A0004),
    secondary              = Color(0xFF222222),
    secondaryContainer     = Color(0xFFDDDDDD),
    onSecondary            = Color.White,
    onSecondaryContainer   = Color(0xFF111111),
    onBackground           = Color(0xFF111111),
    onSurface              = Color(0xFF111111),
    onSurfaceVariant       = Color(0xFF555555),
    outline                = Color(0xFF888888),
    outlineVariant         = Color(0xFFCCCCCC),
    error                  = Color(0xFFD71920),
    onError                = Color.White,
)

// Nothing OS Typography: Bold geometric hierarchy with monospace / wider tracking accents
private val NothingTypography = Typography(
    displayMedium = TextStyle(
        fontWeight    = FontWeight.Normal,
        fontSize      = 50.sp,
        lineHeight    = 54.sp,
        letterSpacing = 1.2.sp,
        fontFamily    = FontFamily.SansSerif,
    ),
    headlineSmall = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 20.sp,
        lineHeight    = 26.sp,
        letterSpacing = 0.6.sp,
    ),
    titleMedium = TextStyle(
        fontWeight    = FontWeight.Bold,
        fontSize      = 15.sp,
        lineHeight    = 20.sp,
        letterSpacing = 0.4.sp,
    ),
    titleSmall = TextStyle(
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 13.sp,
        lineHeight    = 18.sp,
        letterSpacing = 0.3.sp,
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
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 11.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.6.sp,
    ),
)

private val NothingShapes = Shapes(
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun NothingShadeTheme(
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
        isAmoled -> NothingAmoledColors
        isDark -> NothingColors
        else -> NothingLightColors
    }

    // By default Nothing uses iconic glyph red. If user picked custom accent, adapt gracefully
    val colorScheme = if (accentColor != com.supershade.settings.AccentColor.MONET) {
        val c = Color(accentColor.hex)
        base.copy(
            primary = c,
            primaryContainer = c.copy(alpha = if (isDark) 0.30f else 0.18f),
            onPrimary = Color.White,
            onPrimaryContainer = if (isDark) Color.White else c,
        )
    } else {
        monetScheme(base, isDark, if (isAmoled) 0f else 0.35f, monetBlendStrength)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NothingTypography,
        shapes      = NothingShapes,
        content     = content,
    )
}
