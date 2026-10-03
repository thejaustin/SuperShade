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
fun monetScheme(base: ColorScheme, isDark: Boolean, surfaceBlend: Float): ColorScheme {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return base
    val context = LocalContext.current
    val dyn = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    fun s(own: Color, dynamic: Color) = if (surfaceBlend <= 0f) own else lerp(own, dynamic, surfaceBlend)
    return base.copy(
        primary = dyn.primary,
        onPrimary = dyn.onPrimary,
        primaryContainer = dyn.primaryContainer,
        onPrimaryContainer = dyn.onPrimaryContainer,
        secondary = dyn.secondary,
        onSecondary = dyn.onSecondary,
        secondaryContainer = dyn.secondaryContainer,
        onSecondaryContainer = dyn.onSecondaryContainer,
        tertiary = dyn.tertiary,
        onTertiary = dyn.onTertiary,
        tertiaryContainer = dyn.tertiaryContainer,
        onTertiaryContainer = dyn.onTertiaryContainer,
        surfaceTint = dyn.primary,
        surfaceContainer = s(base.surfaceContainer, dyn.surfaceContainer),
        surfaceContainerHigh = s(base.surfaceContainerHigh, dyn.surfaceContainerHigh),
        surfaceContainerHighest = s(base.surfaceContainerHighest, dyn.surfaceContainerHighest),
        surfaceContainerLow = s(base.surfaceContainerLow, dyn.surfaceContainerLow),
        surfaceVariant = s(base.surfaceVariant, dyn.surfaceVariant),
        onSurfaceVariant = s(base.onSurfaceVariant, dyn.onSurfaceVariant),
        outlineVariant = s(base.outlineVariant, dyn.outlineVariant),
    )
}
