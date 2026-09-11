package com.supershade.domain.brightness

import android.content.Context
import android.provider.Settings
import com.supershade.shizuku.StatusBarGovernor

class BrightnessRepository(
    private val context: Context,
    private val governor: StatusBarGovernor,
) {

    fun getCurrent(): Int = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (_: Settings.SettingNotFoundException) {
        128
    }

    suspend fun set(value: Int): Boolean {
        val clamped = value.coerceIn(1, 255)
        setAutoOff()
        if (Settings.System.canWrite(context)) {
            return try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    clamped,
                )
                true
            } catch (_: SecurityException) { false }
        }
        if (governor.isCommanderConnected.value) {
            return governor.runShell("settings", "put", "system", "screen_brightness", clamped.toString())
        }
        return false
    }

    fun isAuto(): Boolean = try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    } catch (_: Settings.SettingNotFoundException) {
        false
    }

    suspend fun setAutoOn() {
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                )
            } catch (_: SecurityException) {}
        } else if (governor.isCommanderConnected.value) {
            governor.runShell("settings", "put", "system", "screen_brightness_mode", "1")
        }
    }

    private suspend fun setAutoOff() {
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                )
            } catch (_: SecurityException) {}
        } else if (governor.isCommanderConnected.value) {
            governor.runShell("settings", "put", "system", "screen_brightness_mode", "0")
        }
    }
}
