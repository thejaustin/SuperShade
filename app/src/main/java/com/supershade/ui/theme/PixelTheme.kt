package com.supershade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PixelColors = darkColorScheme(
    background = Color(0xE0000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    primary = Color(0xFF78B3FF),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF3A3A3C)
)

@Composable
fun PixelShadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PixelColors,
        content = content
    )
}
