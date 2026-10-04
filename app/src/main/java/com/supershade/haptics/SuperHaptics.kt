package com.supershade.haptics

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.compositionLocalOf
import java.lang.reflect.Method
import java.util.regex.Pattern

/**
 * Enterprise-grade tactile haptics engine for SuperShade.
 *
 * Specially architected for Samsung Galaxy S22 Ultra, S23 Ultra, S24 Ultra, S25 Ultra,
 * and S26 Ultra linear resonant actuator (LRA) X-axis hardware, with high-fidelity
 * Android 11-16 Composition primitives and graceful degradation for all devices.
 */
class SuperHaptics(context: Context) {

    private val vibrator: Vibrator? = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val isSamsung: Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    val isSamsungFlagship: Boolean = isSamsung && run {
        val model = Build.MODEL ?: ""
        // Matches Galaxy S22-S26 series (SM-S90x, S91x, S92x, S93x, S94x) and Fold series (SM-F9xx)
        Pattern.compile("^SM-(S9[0-9]{2}|F9[0-9]{2})[A-Z0-9]*$", Pattern.CASE_INSENSITIVE)
            .matcher(model)
            .matches()
    }

    val supportsComposition: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        vibrator?.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL,
            VibrationEffect.Composition.PRIMITIVE_THUD
        ) == true

    private var lastSliderTickTimestamp = 0L

    // =========================================================================
    // Core Public Haptic Triggers
    // =========================================================================

    /**
     * Satisfying mechanical switch click when toggling a tile ON.
     * Replicates the kinetic snap of a premium mechanical switch.
     */
    fun tileToggleOn() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_SWITCH, scale = 1.0f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.35f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.95f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Subtle mechanical switch release when toggling a tile OFF.
     * Softer, damped kinetic release distinct from toggle ON.
     */
    fun tileToggleOff() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_TICK, scale = 0.65f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.45f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.40f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Ultra-crisp detent tick for sliders (brightness, volume).
     * Includes velocity scaling and adaptive rate limiting to prevent actuator saturation.
     *
     * @param velocity Gesture velocity in px/s (e.g. 0f to 2000f) to dynamically scale intensity.
     * @param isBoundary True when the slider hits min (0%) or max (100%) limits.
     */
    fun sliderTick(velocity: Float = 0f, isBoundary: Boolean = false) {
        if (!hasVibrator()) return

        val now = SystemClock.uptimeMillis()
        val minIntervalMs = if (isBoundary) 60L else 32L
        if (now - lastSliderTickTimestamp < minIntervalMs) return
        lastSliderTickTimestamp = now

        try {
            if (isBoundary) {
                sliderBoundary()
                return
            }

            val scale = (0.25f + (velocity.coerceIn(0f, 1500f) / 1500f) * 0.45f).coerceIn(0.25f, 0.70f)

            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_TICK_PICKER, scale = scale)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(6L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Solid detent when scrubbing sliders hit 0% or 100% boundary.
     */
    fun sliderBoundary() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_BUMP, scale = 0.85f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.70f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(22L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Crisp detent when expanding/collapsing Quick Settings or drawer snaps.
     */
    fun sheetDetent() = sheetSnap(expanded = true)

    /**
     * Tactile detent tick when horizontal swipe crosses the notification dismiss threshold.
     * Uses Samsung's dedicated EFFECT_CLICK_DISMISS on Galaxy devices and Android R+
     * composition primitives for ultra-precise haptic detent.
     */
    fun notificationDismissTick() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_CLICK_DISMISS, scale = 0.85f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.65f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Decisive tactile commit when a notification dismissal action is finalized and launched.
     */
    fun notificationDismissCommit() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_CLICK, scale = 0.70f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.75f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(14L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Sheet snap with directional tactile response.
     */
    fun sheetSnap(expanded: Boolean = true) {
        if (!hasVibrator()) return
        try {
            val samsungIdx = if (expanded) SamsungHapticIndices.EFFECT_HEAVY_CLICK_GESTURE else SamsungHapticIndices.EFFECT_HEAVY_CLICK
            if (isSamsungFlagship && playSamsungEffect(samsungIdx, scale = 0.90f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, if (expanded) 0.40f else 0.25f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, if (expanded) 0.85f else 0.70f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(22L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Elastic rubber-band spring bounce when pulling past limits or releasing overscroll.
     */
    fun sheetSpringBounce() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_END_SPRING, scale = 0.75f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.50f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.35f, 15)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Heavy click feedback for significant actions such as lock screen double-tap or power actions.
     */
    fun heavyClick() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_HEAVY_CLICK_IMPACT, scale = 1.0f)) {
                return
            }
            sheetDetent()
        } catch (_: Exception) {}
    }

    /**
     * Fast double-click feedback for double-tap gestures (e.g. status bar double tap to sleep).
     */
    fun doubleTap() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_DOUBLE_CLICK, scale = 0.85f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.80f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.60f, 65)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 15, 50, 15), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Haptic feedback when lifting/reordering a Quick Settings tile.
     */
    fun tileGrab() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_GRAB, scale = 0.80f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.70f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            lightTap()
        } catch (_: Exception) {}
    }

    /**
     * Haptic feedback when dropping a Quick Settings tile into position.
     */
    fun tileDrop() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_HEAVY_CLICK_IMPACT, scale = 0.90f)) {
                return
            }
            heavyClick()
        } catch (_: Exception) {}
    }

    /**
     * Haptic rejection / error when a permission or capability is blocked.
     */
    fun actionDenied() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_FAIL, scale = 0.90f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.80f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.50f, 75)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 20, 40, 20), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Affirmative pulse for successful operation.
     */
    fun actionSuccess() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_SUCCESS, scale = 0.85f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.70f, 0)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 0.40f, 20)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            tileToggleOn()
        } catch (_: Exception) {}
    }

    /**
     * Ultra-light clock tick for subtle taps and minor touches.
     */
    fun lightTap() {
        if (!hasVibrator()) return
        try {
            if (isSamsungFlagship && playSamsungEffect(SamsungHapticIndices.EFFECT_TAP, scale = 0.50f)) {
                return
            }
            if (supportsComposition && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.35f, 0)
                    .compose()
                vibrator?.vibrate(effect)
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(8L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Performs haptic feedback directly on a View with Samsung passthrough.
     */
    fun performViewFeedback(view: View, constant: Int): Boolean {
        return try {
            view.performHapticFeedback(
                constant,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } catch (_: Exception) {
            false
        }
    }

    private fun hasVibrator(): Boolean = vibrator?.hasVibrator() == true

    // =========================================================================
    // Samsung Reflection Bridge
    // =========================================================================

    private fun playSamsungEffect(type: Int, scale: Float = 1.0f): Boolean {
        val effect = SamsungHapticBridge.createWaveform(type, scale) ?: return false
        return try {
            vibrator?.vibrate(effect)
            true
        } catch (_: Exception) {
            false
        }
    }

    private object SamsungHapticBridge {
        private var semCreateWaveformScaleMethod: Method? = null
        private var semCreateWaveformMethod: Method? = null
        private var initialized = false

        init {
            try {
                // Try scaled overload semCreateWaveform(int, int, float)
                semCreateWaveformScaleMethod = VibrationEffect::class.java.getMethod(
                    "semCreateWaveform",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Float::class.javaPrimitiveType
                )
            } catch (_: Exception) {
                semCreateWaveformScaleMethod = null
            }

            try {
                // Try standard semCreateWaveform(int, int)
                semCreateWaveformMethod = VibrationEffect::class.java.getMethod(
                    "semCreateWaveform",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
            } catch (_: Exception) {
                semCreateWaveformMethod = null
            }

            initialized = true
        }

        fun createWaveform(type: Int, scale: Float): VibrationEffect? {
            if (!initialized) return null
            try {
                if (semCreateWaveformScaleMethod != null) {
                    return semCreateWaveformScaleMethod?.invoke(null, type, -1, scale) as? VibrationEffect
                }
                if (semCreateWaveformMethod != null) {
                    return semCreateWaveformMethod?.invoke(null, type, -1) as? VibrationEffect
                }
            } catch (_: Exception) {}
            return null
        }
    }

    object SamsungHapticIndices {
        const val EFFECT_CLICK = 50025
        const val EFFECT_DOUBLE_CLICK = 50029
        const val EFFECT_HEAVY_CLICK = 50038
        const val EFFECT_LIGHT_CUE = 50039
        const val EFFECT_HEAVY_CLICK_GESTURE = 50046
        const val EFFECT_CLICK_GESTURE = 50047
        const val EFFECT_SWITCH = 50051
        const val EFFECT_END_SPRING = 50052
        const val EFFECT_TICK_PICKER = 50056
        const val EFFECT_TICK = 50065
        const val EFFECT_CLICK_DISMISS = 50067
        const val EFFECT_HEAVY_CLICK_IMPACT = 50090
        const val EFFECT_TAP = 50096
        const val EFFECT_SPRING_SHORT = 50102
        const val EFFECT_BUMP = 50103
        const val EFFECT_GRAB = 50132
        const val EFFECT_SUCCESS = 50150
        const val EFFECT_FAIL = 50151
    }
}

val LocalSuperHaptics = compositionLocalOf<SuperHaptics?> { null }

