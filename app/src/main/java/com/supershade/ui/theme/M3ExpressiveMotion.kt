package com.supershade.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Material 3 Expressive (M3E) physics-based motion specifications.
 * Modeled after Android 16 SystemUI motion specifications for reactive tiles,
 * dynamic shape morphing, scale bounce, and fluid spatial transitions.
 */
object M3ExpressiveMotion {
    /**
     * M3E Spatial Default: For corner radius morphing, live status dot expansion,
     * and structural container geometry transitions. Noticeable organic overshoot.
     */
    fun <T> spatialDefault() = spring<T>(
        dampingRatio = 0.60f,
        stiffness = 700f,
    )

    /**
     * M3E Spatial Fast: For tactile touch-press scale bounces, icon pops, and click rebounds.
     */
    fun <T> spatialFast() = spring<T>(
        dampingRatio = 0.60f,
        stiffness = 1400f,
    )

    /**
     * M3E Spatial Slow: For panel expansion, grid reordering, and sheet drawer transitions.
     */
    fun <T> spatialSlow() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = 350f,
    )

    /**
     * M3E Spatial Bouncy: High-energy overshoot spring for state toggles, badge pops, and dot entries.
     */
    fun <T> spatialBouncy() = spring<T>(
        dampingRatio = 0.48f,
        stiffness = 800f,
    )

    /**
     * M3E Effects Default: For background color, icon tint, and alpha transitions.
     * Critically damped to eliminate color flashing or chromatic overshoot.
     */
    fun <T> effectsDefault() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1600f,
    )
}
