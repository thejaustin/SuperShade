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
            val canonical = canonicalTileId(id)
            val (label, capability) = KNOWN_TILES[canonical]
                ?: KNOWN_TILES[id]
                ?: (humanizeTileLabel(canonical) to TileCapability.SETTINGS_INTENT)
            TileDefinition(
                id = canonical,
                label = label,
                isActive = false,
                capability = capability,
                componentName = TILE_COMPONENTS[canonical] ?: TILE_COMPONENTS[id],
                settingsAction = TILE_SETTINGS_ACTIONS[canonical] ?: TILE_SETTINGS_ACTIONS[id],
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
        } catch (e: Exception) {}

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
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: android.content.Intent?) {
                    scope.launch { updateActiveStates() }
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {}

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
        } catch (e: Exception) {}

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
            val canonical = canonicalTileId(id)
            val known = KNOWN_TILES[id] ?: KNOWN_TILES[canonical]
            val componentName = when {
                TILE_COMPONENTS.containsKey(id) -> TILE_COMPONENTS[id]
                TILE_COMPONENTS.containsKey(canonical) -> TILE_COMPONENTS[canonical]
                token.startsWith("custom(") && token.endsWith(")") -> token.substring(7, token.length - 1).trim()
                token.contains("/") -> token
                else -> null
            }

            // Resolve friendly label: KNOWN_TILES -> PackageManager ServiceInfo -> humanizeTileLabel
            val label = when {
                known != null -> known.first
                componentName != null -> {
                    try {
                        val cn = android.content.ComponentName.unflattenFromString(componentName)
                        if (cn != null) {
                            val serviceInfo = context.packageManager.getServiceInfo(cn, 0)
                            val loaded = serviceInfo.loadLabel(context.packageManager).toString()
                            if (loaded.isNotBlank() && !loaded.contains(".") && !loaded.endsWith("TileService") && !loaded.endsWith("Service")) {
                                loaded
                            } else {
                                humanizeTileLabel(if (loaded.isNotBlank()) loaded else componentName)
                            }
                        } else humanizeTileLabel(id)
                    } catch (_: Exception) {
                        humanizeTileLabel(id)
                    }
                }
                else -> humanizeTileLabel(id)
            }

            val capability = known?.second ?: TileCapability.FULL_TOGGLE
            val isActive = queryTileActiveState(id)
            TileDefinition(
                id = canonical,
                label = label,
                isActive = isActive,
                capability = capability,
                componentName = componentName,
                settingsAction = TILE_SETTINGS_ACTIONS[canonical] ?: TILE_SETTINGS_ACTIONS[id],
                subtitle = queryTileSubtitle(canonical),
            )
        }
    }

    fun setTileActive(id: String, active: Boolean) {
        val target = canonicalTileId(id)
        _tiles.value = _tiles.value.map {
            if (canonicalTileId(it.id) == target) it.copy(isActive = active) else it
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
                key.contains("reducebright") || key.contains("extradim") -> {
                    Settings.Secure.getInt(context.contentResolver, "reduce_bright_colors_activated", 0) == 1
                }
                key.contains("bluelight") || key.contains("eyecomfort") -> {
                    Settings.System.getInt(context.contentResolver, "blue_light_filter", 0) == 1 ||
                    Settings.Secure.getInt(context.contentResolver, "night_display_activated", 0) == 1
                }
                key.contains("aod") || key.contains("alwayson") -> {
                    Settings.Secure.getInt(context.contentResolver, "aod_mode", 0) == 1 ||
                    Settings.Secure.getInt(context.contentResolver, "doze_always_on", 0) == 1
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}
