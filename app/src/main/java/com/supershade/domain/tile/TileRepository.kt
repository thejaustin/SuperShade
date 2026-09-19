package com.supershade.domain.tile

import android.app.AlarmManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.app.NotificationManager
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.StatusBarGovernor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TileRepository(
    private val context: Context,
    private val governor: StatusBarGovernor,
    private val settings: ShadeSettings? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val componentToId: Map<String, String> =
        TILE_COMPONENTS.entries.associate { (id, comp) -> comp to id }

    private val _tiles = MutableStateFlow<List<TileDefinition>>(
        DEFAULT_TILES.map { id ->
            val (label, capability) = KNOWN_TILES[id] ?: (id to TileCapability.SETTINGS_INTENT)
            TileDefinition(
                id = id,
                label = label,
                isActive = false,
                capability = capability,
                componentName = TILE_COMPONENTS[id],
                settingsAction = TILE_SETTINGS_ACTIONS[id],
                subtitle = null,
            )
        }
    )
    val tiles: StateFlow<List<TileDefinition>> = _tiles.asStateFlow()

    @Volatile private var torchEnabled = false

    init {
        // Track torch state without polling so the flashlight tile stays accurate
        try {
            val cm = context.getSystemService(CameraManager::class.java)
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchEnabled = enabled
                    _tiles.value = _tiles.value.map { tile ->
                        if (tile.id.lowercase().contains("flashlight")) tile.copy(isActive = enabled)
                        else tile
                    }
                }
            }, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}

        try {
            val filter = android.content.IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(LocationManager.MODE_CHANGED_ACTION)
                addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
                addAction(android.media.AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }
            context.registerReceiver(object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: android.content.Intent?) {
                    scope.launch { updateActiveStates() }
                }
            }, filter)
        } catch (_: Exception) {}

        try {
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        scope.launch { updateActiveStates() }
                    }
                }
            )
        } catch (_: Exception) {}

        scope.launch { loadTiles() }

        settings?.enabledTiles
            ?.onEach { loadTiles() }
            ?.launchIn(scope)

        governor.isCommanderConnected
            .onEach { connected -> if (connected) loadTiles() }
            .launchIn(scope)
    }

    /** Public reload entry-point — callers can trigger a fresh fetch. */
    fun reload() {
        scope.launch { loadTiles() }
    }

    fun setTileActiveOptimistic(id: String, active: Boolean) {
        _tiles.value = _tiles.value.map { tile ->
            if (tile.id == id) tile.copy(isActive = active) else tile
        }
    }

    fun refreshActiveStates() {
        scope.launch { updateActiveStates() }
    }

    private fun updateActiveStates() {
        _tiles.value = _tiles.value.map { tile ->
            tile.copy(
                isActive = queryTileActiveState(tile.id),
                subtitle = queryTileSubtitle(tile.id),
            )
        }
    }

    private suspend fun loadTiles() {
        val userConfigured = try { settings?.enabledTiles?.first() ?: emptyList() } catch (_: Exception) { emptyList() }
        val tokens = if (userConfigured.isNotEmpty()) {
            userConfigured
        } else {
            val raw = governor.getCurrentTiles()
            if (raw.isNotBlank()) raw.split(",").map { it.trim() }
            else DEFAULT_TILES
        }

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
                settingsAction = TILE_SETTINGS_ACTIONS[id],
                subtitle = queryTileSubtitle(id),
            )
        }
    }

    fun setTileActive(id: String, active: Boolean) {
        _tiles.value = _tiles.value.map {
            if (it.id == id) it.copy(isActive = active) else it
        }
    }

    private fun queryTileSubtitle(id: String): String? {
        val key = id.lowercase()
        return try {
            when {
                key.contains("wifi") || key.contains("internet") -> {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                    if (wm?.isWifiEnabled != true) return null
                    @Suppress("DEPRECATION")
                    val raw = wm.connectionInfo?.ssid?.trim('"')
                    if (!raw.isNullOrBlank() && raw != "<unknown ssid>") raw else "Connected"
                }
                key.contains("bt") || key.contains("bluetooth") -> {
                    val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val adapter = bm?.adapter
                    if (adapter?.isEnabled != true) return null
                    try {
                        @Suppress("MissingPermission")
                        val bonded = adapter.bondedDevices?.filter { dev ->
                            try {
                                val isConnectedMethod = dev.javaClass.getMethod("isConnected")
                                (isConnectedMethod.invoke(dev) as? Boolean) == true
                            } catch (_: Exception) { false }
                        }
                        val name = bonded?.firstOrNull()?.name
                        if (!name.isNullOrBlank()) name else "On"
                    } catch (_: Exception) { "On" }
                }
                key.contains("mute") || key.contains("volume") || key.contains("sound") -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    when (am?.ringerMode) {
                        android.media.AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                        android.media.AudioManager.RINGER_MODE_SILENT  -> "Mute"
                        android.media.AudioManager.RINGER_MODE_NORMAL  -> "Sound"
                        else -> null
                    }
                }
                key.contains("rotation") || key.contains("rotationlock") -> {
                    val auto = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                    if (auto) "Auto rotate" else "Portrait"
                }
                key.contains("battery") || key.contains("batterymode") -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (pm?.isPowerSaveMode == true) "Power saving" else null
                }
                key.contains("flashlight") -> {
                    if (torchEnabled) "On" else null
                }
                key.contains("hotspot") -> {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    val isAp = try {
                        val m = wm?.javaClass?.getDeclaredMethod("isWifiApEnabled")
                        m?.isAccessible = true
                        (m?.invoke(wm) as? Boolean) == true
                    } catch (_: Exception) { false }
                    if (isAp) "Active" else null
                }
                key.contains("dnd") -> {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    when (nm?.currentInterruptionFilter) {
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority only"
                        NotificationManager.INTERRUPTION_FILTER_ALARMS   -> "Alarms only"
                        NotificationManager.INTERRUPTION_FILTER_NONE     -> "Silent"
                        else -> null
                    }
                }
                key.contains("alarm") -> {
                    val am = context.getSystemService(AlarmManager::class.java)
                    val next = am?.nextAlarmClock
                    if (next != null)
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(next.triggerTime))
                    else null
                }
                else -> null
            }
        } catch (_: Exception) { null }
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
                key.contains("flashlight") -> torchEnabled
                key.contains("cell") || key.contains("cellular") || key.contains("data") -> {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                    try { tm?.isDataEnabled == true } catch (_: Exception) { false }
                }
                key.contains("hotspot") -> {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    try {
                        val m = wm?.javaClass?.getDeclaredMethod("isWifiApEnabled")
                        m?.isAccessible = true
                        (m?.invoke(wm) as? Boolean) == true
                    } catch (_: Exception) { false }
                }
                key.contains("mute") || key.contains("sound") -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    am?.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL
                }
                key.contains("sync") -> {
                    android.content.ContentResolver.getMasterSyncAutomatically()
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }
}
