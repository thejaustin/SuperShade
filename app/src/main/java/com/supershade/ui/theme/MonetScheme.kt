package com.supershade.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/**
 * Overlays the wallpaper-derived (Monet / Material You) palette onto an OS-specific [base]
 * scheme so One UI, Pixel, Nothing and Cyberpunk themes follow the wallpaper while keeping
 * their own structure. Accent roles are always replaced; neutral surfaces are blended by
 * [surfaceBlend] (0 keeps the OS surfaces untouched, 1 uses full dynamic surfaces).
 */
@Composable
fun monetScheme(
    base: ColorScheme,
    isDark: Boolean,
    surfaceBlend: Float,
    accentStrength: Float = 1.0f,
): ColorScheme {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return base
    val clampedStrength = accentStrength.coerceIn(0f, 1f)
    if (clampedStrength <= 0f) return base

    val context = LocalContext.current
    val dyn = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val effectiveSurfaceBlend = (surfaceBlend * clampedStrength).coerceIn(0f, 1f)

    fun a(own: Color, dynamic: Color) = if (clampedStrength >= 1f) dynamic else lerp(own, dynamic, clampedStrength)
    fun s(own: Color, dynamic: Color) = if (effectiveSurfaceBlend <= 0f) own else lerp(own, dynamic, effectiveSurfaceBlend)

    return base.copy(
        primary = a(base.primary, dyn.primary),
        onPrimary = a(base.onPrimary, dyn.onPrimary),
        primaryContainer = a(base.primaryContainer, dyn.primaryContainer),
        onPrimaryContainer = a(base.onPrimaryContainer, dyn.onPrimaryContainer),
        secondary = a(base.secondary, dyn.secondary),
        onSecondary = a(base.onSecondary, dyn.onSecondary),
        secondaryContainer = a(base.secondaryContainer, dyn.secondaryContainer),
        onSecondaryContainer = a(base.onSecondaryContainer, dyn.onSecondaryContainer),
        tertiary = a(base.tertiary, dyn.tertiary),
        onTertiary = a(base.onTertiary, dyn.onTertiary),
        tertiaryContainer = a(base.tertiaryContainer, dyn.tertiaryContainer),
        onTertiaryContainer = a(base.onTertiaryContainer, dyn.onTertiaryContainer),
        surfaceTint = a(base.primary, dyn.primary),
        surfaceContainer = s(base.surfaceContainer, dyn.surfaceContainer),
        surfaceContainerHigh = s(base.surfaceContainerHigh, dyn.surfaceContainerHigh),
        surfaceContainerHighest = s(base.surfaceContainerHighest, dyn.surfaceContainerHighest),
        surfaceContainerLow = s(base.surfaceContainerLow, dyn.surfaceContainerLow),
        surfaceVariant = s(base.surfaceVariant, dyn.surfaceVariant),
        onSurfaceVariant = s(base.onSurfaceVariant, dyn.onSurfaceVariant),
        outlineVariant = s(base.outlineVariant, dyn.outlineVariant),
    )
}
