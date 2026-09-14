package com.supershade.ui.theme

sealed class ShadeTheme {
    data object OneUI : ShadeTheme()
    data object Pixel : ShadeTheme()
    data object PureMaterial : ShadeTheme()
}

enum class DarkThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED
}
