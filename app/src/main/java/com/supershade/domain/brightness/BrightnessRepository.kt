package com.supershade.domain.brightness

import android.content.Context
import android.provider.Settings

class BrightnessRepository(private val context: Context) {

    fun getCurrent(): Int = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (_: Settings.SettingNotFoundException) {
        128
    }

    fun set(value: Int) {
        try {
            setAutoOff()
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value.coerceIn(1, 255),
            )
        } catch (_: SecurityException) {}
    }

    fun isAuto(): Boolean = try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    } catch (_: Settings.SettingNotFoundException) {
        false
    }

    private fun setAutoOff() {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
    }
}
