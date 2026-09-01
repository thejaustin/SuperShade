package com.supershade.ui.theme

sealed class ShadeTheme {
    data object OneUI : ShadeTheme()
    data object Pixel : ShadeTheme()
}
