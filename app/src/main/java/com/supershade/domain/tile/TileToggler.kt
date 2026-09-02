package com.supershade.domain.tile

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.supershade.shizuku.StatusBarGovernor

class TileToggler(
    private val context: Context,
    private val governor: StatusBarGovernor
) {
    suspend fun toggle(tile: TileDefinition) {
        when (tile.capability) {
            TileCapability.FULL_TOGGLE -> togglePrivileged(tile)
            TileCapability.SETTINGS_INTENT -> openSettings(tile)
            TileCapability.READ_ONLY -> Unit
        }
    }

    private suspend fun togglePrivileged(tile: TileDefinition) {
        val id = tile.id.lowercase()
        val newState = !tile.isActive

        when {
            id.contains("wifi") || id.contains("internet") -> {
                governor.runShell("svc", "wifi", if (newState) "enable" else "disable")
            }
            id.contains("bt") || id.contains("bluetooth") -> {
                governor.runShell("cmd", "bluetooth", if (newState) "enable" else "disable")
            }
            id.contains("dark") || id.contains("uimodenight") || id.contains("night") -> {
                governor.runShell("cmd", "uimode", "night", if (newState) "yes" else "no")
            }
            id.contains("rotation") || id.contains("rotationlock") -> {
                governor.runShell("settings", "put", "system", "accelerometer_rotation", if (newState) "1" else "0")
            }
            id.contains("airplane") -> {
                governor.runShell("cmd", "connectivity", "airplane-mode", if (newState) "enable" else "disable")
            }
            id.contains("location") -> {
                governor.runShell("cmd", "location", "set-location-enabled", if (newState) "true" else "false")
            }
            id.contains("nfc") -> {
                governor.runShell("svc", "nfc", if (newState) "enable" else "disable")
            }
            id.contains("cell") || id.contains("cellular") || id.contains("data") -> {
                governor.runShell("svc", "data", if (newState) "enable" else "disable")
            }
            id.contains("dnd") || id.contains("donotdisturb") -> {
                governor.runShell("cmd", "notification", "set_dnd", if (newState) "on" else "off")
            }
            id.contains("battery") || id.contains("batterymode") -> {
                governor.runShell("cmd", "power", "set-mode", if (newState) "1" else "0")
            }
            else -> {
                val component = tile.componentName ?: tile.id
                governor.clickTile(component)
            }
        }
    }

    private fun openSettings(tile: TileDefinition) {
        val action = tile.settingsAction ?: when (tile.id.lowercase()) {
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "battery", "batterymode" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "vpn" -> Settings.ACTION_VPN_SETTINGS
            "cast" -> Settings.ACTION_CAST_SETTINGS
            "volume" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}
