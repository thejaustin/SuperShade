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
            TileCapability.FULL_TOGGLE -> governor.clickTile(tile.id)
            TileCapability.SETTINGS_INTENT -> openSettings(tile)
            TileCapability.READ_ONLY -> Unit
        }
    }

    private fun openSettings(tile: TileDefinition) {
        val action = tile.settingsAction ?: when (tile.id) {
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "battery"  -> Intent.ACTION_POWER_USAGE_SUMMARY
            else       -> Settings.ACTION_SETTINGS
        }
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
