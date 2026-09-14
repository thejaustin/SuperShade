package com.supershade.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.supershade.R
import com.supershade.settings.QsTileTapAction
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.ui.tile.TilePreferencesActivity
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * System Quick Settings Tile for SuperShade.
 *
 * Supports:
 * - Direct single-tap toggle (Enable / Disable) or instant pull-down expansion or quick menu.
 * - Customization via [ShadeSettings.qsTileTapAction].
 * - Long-press tile preferences via [TilePreferencesActivity].
 */
class SuperShadeTileService : TileService() {

    companion object {
        fun requestUpdate(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, SuperShadeTileService::class.java))
            } catch (_: Exception) {}
        }
    }

    private val settings: ShadeSettings by inject()
    private val governor: StatusBarGovernor by inject()
    private val shadeViewModel: ShadeViewModel by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val tapAction = settings.qsTileTapAction.first()
            val isActive = settings.isActive.first()

            when (tapAction) {
                QsTileTapAction.TOGGLE_ACTIVE -> {
                    val newState = !isActive
                    settings.setActive(newState)
                    toggleShadeService(newState)
                    if (!newState) {
                        governor.enableExpansion()
                    } else if (settings.blockSystemShade.first()) {
                        governor.disableExpansion()
                    }
                    updateTileState()
                }
                QsTileTapAction.OPEN_SHADE -> {
                    if (!isActive) {
                        settings.setActive(true)
                        toggleShadeService(true)
                    }
                    collapseSystemShadeAndOpenSuperShade()
                }
                QsTileTapAction.SHOW_MENU -> {
                    openChoicesMenu()
                }
            }
        }
    }

    private fun collapseSystemShadeAndOpenSuperShade() {
        scope.launch {
            governor.collapse()
        }
        val intent = Intent(this, ShadeService::class.java).apply {
            action = ShadeService.ACTION_OPEN_SHADE
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {
            try { startService(intent) } catch (_: Exception) {}
        }
        shadeViewModel.open()
    }

    private fun openChoicesMenu() {
        val intent = Intent(this, TilePreferencesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        scope.launch {
            val active = settings.isActive.first()
            val tapAction = settings.qsTileTapAction.first()

            tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "SuperShade"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = when {
                    !active -> "Disabled"
                    tapAction == QsTileTapAction.OPEN_SHADE -> "Tap to Open"
                    tapAction == QsTileTapAction.SHOW_MENU -> "Tap for Menu"
                    else -> "Active"
                }
            }
            tile.icon = Icon.createWithResource(this@SuperShadeTileService, R.drawable.ic_notification_shade)
            tile.updateTile()
        }
    }

    private fun toggleShadeService(enable: Boolean) {
        val intent = Intent(this, ShadeService::class.java)
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
