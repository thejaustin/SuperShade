package com.supershade.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = Color(0xFF1C6FEE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0F4BA8),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF78B3FF),
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFC7C7CC),
    outline = Color(0xFF38383A),
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

private val AmoledColors = darkColorScheme(
    primary = Color(0xFF1C6FEE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0F4BA8),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF78B3FF),
    onSecondary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF0D0D0D),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF242424),
    error = Color(0xFFCF6679),
    onError = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1C6FEE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF00639B),
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

@Composable
fun SuperShadeAppTheme(
    mode: DarkThemeMode = DarkThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (mode) {
        DarkThemeMode.SYSTEM -> systemInDark
        DarkThemeMode.DARK, DarkThemeMode.AMOLED -> true
        DarkThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        mode == DarkThemeMode.AMOLED -> AmoledColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
