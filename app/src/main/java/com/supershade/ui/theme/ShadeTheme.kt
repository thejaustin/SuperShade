package com.supershade.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

sealed class ShadeTheme {
    data object OneUI : ShadeTheme()
    data object Pixel : ShadeTheme()
    data object PureMaterial : ShadeTheme()
    data object Nothing : ShadeTheme()
    data object Cyberpunk : ShadeTheme()
}

enum class DarkThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED
}

val LocalShadeTheme = staticCompositionLocalOf<ShadeTheme> { ShadeTheme.OneUI }
