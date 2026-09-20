package com.supershade.ui.theme

import androidx.compose.runtime.compositionLocalOf

enum class BackdropTheme(
    val id: String,
    val label: String,
    val subtitle: String,
) {
    FROSTED_GLASS(
        id = "frosted",
        label = "Frosted Glass",
        subtitle = "Refined acrylic glass with dynamic backdrop blur and luminous accents",
    ),
    LIQUID_GLASS(
        id = "liquid",
        label = "Liquid Glass",
        subtitle = "Luminous specular reflections, iridescent light refraction & vivid fluid depth",
    ),
    BLURRY(
        id = "blurry",
        label = "Blurry",
        subtitle = "Deep Gaussian blur with enriched contrast for effortless reading",
    ),
    OPAQUE(
        id = "opaque",
        label = "Opaque",
        subtitle = "100% solid surface with zero bleed-through for maximum legibility",
    ),
    TRANSPARENT(
        id = "transparent",
        label = "Transparent",
        subtitle = "Clean translucent glass letting your wallpaper and content shine through",
    );

    companion object {
        fun fromId(id: String?): BackdropTheme =
            entries.firstOrNull { it.id == id } ?: FROSTED_GLASS
    }
}

val LocalBackdropTheme = compositionLocalOf { BackdropTheme.FROSTED_GLASS }
