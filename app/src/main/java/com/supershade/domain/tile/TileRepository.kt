package com.supershade.domain.tile

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.res.Configuration
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.PowerManager
import android.provider.Settings
import android.app.NotificationManager
import com.supershade.shizuku.StatusBarGovernor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TileRepository(
    private val context: Context,
    private val governor: StatusBarGovernor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _tiles = MutableStateFlow<List<TileDefinition>>(emptyList())
    val tiles: StateFlow<List<TileDefinition>> = _tiles.asStateFlow()

    private val componentToId: Map<String, String> =
        TILE_COMPONENTS.entries.associate { (id, comp) -> comp to id }

    init {
        scope.launch { loadTiles() }

        governor.isCommanderConnected
            .onEach { connected -> if (connected) loadTiles() }
            .launchIn(scope)
    }

    /** Public reload entry-point — callers can trigger a fresh fetch. */
    fun reload() {
        scope.launch { loadTiles() }
    }

    private suspend fun loadTiles() {
        val raw = governor.getCurrentTiles()
        val tokens = if (raw.isBlank()) KNOWN_TILES.keys.take(8).toList()
                     else raw.split(",").map { it.trim() }

        _tiles.value = tokens.map { token ->
            val id = componentToId[token] ?: token
            val (label, capability) = KNOWN_TILES[id] ?: (id to TileCapability.SETTINGS_INTENT)
            val componentName = TILE_COMPONENTS[id]
            val isActive = queryTileActiveState(id)
            TileDefinition(
                id = id,
                label = label,
                isActive = isActive,
                capability = capability,
                componentName = componentName,
            )
        }
    }

    fun setTileActive(id: String, active: Boolean) {
        _tiles.value = _tiles.value.map {
            if (it.id == id) it.copy(isActive = active) else it
        }
    }

    private fun queryTileActiveState(id: String): Boolean {
        val key = id.lowercase()
        return try {
            when {
                key.contains("wifi") || key.contains("internet") -> {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    wm?.isWifiEnabled == true
                }
                key.contains("bt") || key.contains("bluetooth") -> {
                    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    bm?.adapter?.isEnabled == true
                }
                key.contains("dark") || key.contains("uimodenight") -> {
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                }
                key.contains("rotation") || key.contains("rotationlock") -> {
                    Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                }
                key.contains("airplane") -> {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
                }
                key.contains("location") -> {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    lm?.isLocationEnabled == true
                }
                key.contains("nfc") -> {
                    val nfc = NfcAdapter.getDefaultAdapter(context)
                    nfc?.isEnabled == true
                }
                key.contains("dnd") || key.contains("donotdisturb") -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    (nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL) != NotificationManager.INTERRUPTION_FILTER_ALL
                }
                key.contains("battery") || key.contains("batterymode") -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    pm?.isPowerSaveMode == true
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
