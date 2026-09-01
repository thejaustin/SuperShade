package com.supershade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OneUiColors = darkColorScheme(
    background = Color(0xCC1A1A1A),
    surface = Color(0xFF2A2A2A),
    surfaceVariant = Color(0xFF333333),
    primary = Color(0xFF4DA6FF),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF444444)
)

@Composable
fun OneUiShadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OneUiColors,
        content = content
    )
}
