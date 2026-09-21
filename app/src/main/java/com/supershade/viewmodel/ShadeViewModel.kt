package com.supershade.viewmodel

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supershade.domain.brightness.BrightnessRepository
import com.supershade.domain.media.MediaRepository
import com.supershade.domain.media.MediaState
import com.supershade.domain.notification.NotificationRepository
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.domain.tile.DEFAULT_TILES
import com.supershade.domain.tile.KNOWN_TILES
import com.supershade.domain.tile.TileCapability
import com.supershade.domain.tile.TILE_COMPONENTS
import com.supershade.domain.tile.TILE_SETTINGS_ACTIONS
import com.supershade.domain.tile.TileDefinition
import com.supershade.domain.tile.TileRepository
import com.supershade.domain.tile.TileToggler
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.StatusBarGovernor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ShadeViewModel(
    private val context: Context,
    private val notificationRepo: NotificationRepository,
    private val tileRepo: TileRepository,
    private val tileToggler: TileToggler,
    private val mediaRepo: MediaRepository,
    private val brightnessRepo: BrightnessRepository,
    private val settings: ShadeSettings,
    private val governor: StatusBarGovernor,
) : ViewModel() {

    private val _state = MutableStateFlow(ShadeState())
    val state: StateFlow<ShadeState> = _state.asStateFlow()

    private var positionTickerJob: Job? = null
    private var tileRefreshJob: Job? = null

    init {
        notificationRepo.notifications
            .onEach { notifications ->
                _state.update { current ->
                    current.copy(
                        allNotifications = notifications,
                        visibleNotifications = filterFor(notifications, current.selectedCategory)
                    )
                }
            }
            .launchIn(viewModelScope)

        tileRepo.tiles
            .onEach { tiles -> _state.update { it.copy(tiles = tiles) } }
            .launchIn(viewModelScope)

        mediaRepo.media
            .onEach { media ->
                _state.update { it.copy(media = media) }
                updatePositionTicker(media)
            }
            .launchIn(viewModelScope)

        settings.theme
            .onEach { t -> _state.update { it.copy(theme = t) } }
            .launchIn(viewModelScope)

        settings.darkThemeMode
            .onEach { m -> _state.update { it.copy(darkThemeMode = m) } }
            .launchIn(viewModelScope)

        settings.accentColor
            .onEach { a -> _state.update { it.copy(accentColor = a) } }
            .launchIn(viewModelScope)

        settings.tileShape
            .onEach { shape -> _state.update { it.copy(tileShape = shape) } }
            .launchIn(viewModelScope)

        settings.tileSize
            .onEach { size -> _state.update { it.copy(tileSize = size) } }
            .launchIn(viewModelScope)

        settings.tileColumns
            .onEach { cols -> _state.update { it.copy(tileColumns = cols) } }
            .launchIn(viewModelScope)

        settings.showWideCards
            .onEach { show -> _state.update { it.copy(showWideCards = show) } }
            .launchIn(viewModelScope)

        settings.cardBorderWidth
            .onEach { width -> _state.update { it.copy(cardBorderWidth = width) } }
            .launchIn(viewModelScope)

        settings.splitGestureMode
            .onEach { mode -> _state.update { it.copy(splitGestureMode = mode) } }
            .launchIn(viewModelScope)

        settings.showPanelSwitcherPill
            .onEach { show -> _state.update { it.copy(showPanelSwitcherPill = show) } }
            .launchIn(viewModelScope)

        settings.backdropTheme
            .onEach { backdrop -> _state.update { it.copy(backdropTheme = backdrop) } }
            .launchIn(viewModelScope)

        settings.backdropOpacity
            .onEach { opacity -> _state.update { it.copy(backdropOpacity = opacity) } }
            .launchIn(viewModelScope)

        governor.isCommanderConnected
            .onEach { connected -> _state.update { it.copy(isShizukuConnected = connected) } }
            .launchIn(viewModelScope)
    }

    private fun updatePositionTicker(media: MediaState?) {
        if (media == null || !media.isPlaying || media.duration <= 0) {
            positionTickerJob?.cancel()
            positionTickerJob = null
            return
        }
        // Always cancel and restart to re-seed position from the latest MediaState
        positionTickerJob?.cancel()
        positionTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _state.update { s ->
                    val current = s.media ?: return@update s
                    if (!current.isPlaying) return@update s
                    val newPos = (current.position + 1000L).coerceAtMost(current.duration)
                    s.copy(media = current.copy(position = newPos))
                }
            }
        }
    }

    fun open(expandQs: Boolean = false) {
        _state.update {
            it.copy(
                isOpen = true,
                isQsExpanded = expandQs,
                activePanel = if (expandQs) ShadePanel.QUICK_SETTINGS else ShadePanel.NOTIFICATIONS,
                isQuickControlsTucked = false,
                brightness = brightnessRepo.getCurrent(),
            )
        }
        mediaRepo.refresh()
        tileRepo.reload()
        notificationRepo.refresh()
        tileRefreshJob?.cancel()
        tileRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                tileRepo.reload()
            }
        }
    }

    fun setQsExpanded(expanded: Boolean) {
        _state.update {
            it.copy(
                isQsExpanded = expanded,
                activePanel = if (expanded) ShadePanel.QUICK_SETTINGS else ShadePanel.NOTIFICATIONS,
            )
        }
    }

    fun setActivePanel(panel: ShadePanel) {
        _state.update {
            it.copy(
                activePanel = panel,
                isQsExpanded = (panel == ShadePanel.QUICK_SETTINGS),
            )
        }
    }

    fun setQuickControlsTucked(tucked: Boolean) {
        _state.update { it.copy(isQuickControlsTucked = tucked) }
    }

    fun close() {
        _state.update { it.copy(isOpen = false, isQsExpanded = false, isQuickControlsTucked = false) }
        tileRefreshJob?.cancel()
        tileRefreshJob = null
    }

    fun selectCategory(category: ShadeCategory) {
        _state.update { current ->
            current.copy(
                selectedCategory = category,
                visibleNotifications = filterFor(current.allNotifications, category)
            )
        }
    }

    fun toggleTile(tile: TileDefinition) {
        viewModelScope.launch { tileToggler.toggle(tile) }
    }

    fun moveTile(fromIndex: Int, toIndex: Int) {
        val currentTiles = _state.value.tiles.toMutableList()
        if (fromIndex in currentTiles.indices && toIndex in currentTiles.indices && fromIndex != toIndex) {
            val moved = currentTiles.removeAt(fromIndex)
            currentTiles.add(toIndex, moved)
            _state.update { it.copy(tiles = currentTiles) }
            viewModelScope.launch {
                settings.setEnabledTiles(currentTiles.map { it.id })
            }
        }
    }

    fun removeTile(tileId: String) {
        val currentTiles = _state.value.tiles.toMutableList()
        val index = currentTiles.indexOfFirst { it.id == tileId }
        if (index != -1) {
            currentTiles.removeAt(index)
            _state.update { it.copy(tiles = currentTiles) }
            viewModelScope.launch {
                settings.setEnabledTiles(currentTiles.map { it.id })
            }
        }
    }

    fun addTile(tileId: String) {
        if (_state.value.tiles.any { it.id == tileId }) return
        val (label, capability) = KNOWN_TILES[tileId] ?: (tileId.replaceFirstChar { it.uppercase() } to TileCapability.FULL_TOGGLE)
        val newTile = TileDefinition(
            id = tileId,
            label = label,
            isActive = false,
            capability = capability,
            componentName = TILE_COMPONENTS[tileId],
            settingsAction = TILE_SETTINGS_ACTIONS[tileId],
            subtitle = null,
        )
        val updated = _state.value.tiles + newTile
        _state.update { it.copy(tiles = updated) }
        viewModelScope.launch {
            settings.setEnabledTiles(updated.map { it.id })
        }
    }

    fun resetTiles() {
        viewModelScope.launch {
            settings.setEnabledTiles(DEFAULT_TILES)
        }
    }

    fun openTileDetail(tile: TileDefinition) {
        val id = tile.id.lowercase()
        when {
            id.contains("flashlight") -> {
                val max = tileToggler.getTorchMaxStrength()
                val current = if (_state.value.activeTileDetail?.type == TileDetailType.FLASHLIGHT) {
                    _state.value.activeTileDetail?.torchLevel ?: (if (tile.isActive) max else 1)
                } else {
                    if (tile.isActive) max else 1
                }
                _state.update {
                    it.copy(
                        activeTileDetail = TileDetailState(
                            type = TileDetailType.FLASHLIGHT,
                            title = "Flashlight",
                            subtitle = if (tile.isActive) "On • Level $current" else "Off",
                            isActive = tile.isActive,
                            torchLevel = current,
                            maxTorchLevel = max,
                            settingsAction = tile.settingsAction,
                        )
                    )
                }
            }
            id.contains("wifi") || id.contains("internet") -> {
                val wm = context.getSystemService(WifiManager::class.java)
                val cm = context.getSystemService(ConnectivityManager::class.java)
                val info = try { wm?.connectionInfo } catch (_: Exception) { null }
                val rawSsid = info?.ssid?.trim('"')
                val ssid = if (rawSsid == "<unknown ssid>" || rawSsid.isNullOrBlank()) {
                    if (tile.isActive) "Connected Wi-Fi" else "Wi-Fi Disconnected"
                } else rawSsid

                val freq = info?.frequency ?: 0
                val band = when {
                    freq > 5925 -> "6 GHz (Wi-Fi 6E/7)"
                    freq > 4900 -> "5 GHz"
                    freq > 2400 -> "2.4 GHz"
                    else -> if (tile.isActive) "Wi-Fi" else "Disconnected"
                }
                val speed = if (info != null && info.linkSpeed > 0) "${info.linkSpeed} Mbps" else null
                val rssi = info?.rssi ?: -100
                val ip = try {
                    val activeNet = cm?.activeNetwork
                    val linkProps = cm?.getLinkProperties(activeNet)
                    linkProps?.linkAddresses?.firstOrNull { it.address is java.net.Inet4Address }?.address?.hostAddress
                } catch (_: Exception) { null }

                _state.update {
                    it.copy(
                        activeTileDetail = TileDetailState(
                            type = TileDetailType.WIFI,
                            title = "Wi-Fi",
                            subtitle = if (tile.isActive) ssid else "Off",
                            isActive = tile.isActive,
                            wifiSsid = ssid,
                            wifiBand = band,
                            wifiIp = ip,
                            wifiLinkSpeed = speed,
                            wifiRssi = rssi,
                            settingsAction = tile.settingsAction ?: Settings.ACTION_WIFI_SETTINGS,
                        )
                    )
                }
            }
            id.contains("bt") || id.contains("bluetooth") -> {
                val am = context.getSystemService(AudioManager::class.java)
                val audioDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                        }
                    } catch (_: Exception) { null }
                } else null

                val deviceName = audioDevice?.productName?.toString()
                    ?: if (tile.isActive) "Bluetooth Active" else "Bluetooth Off"

                _state.update {
                    it.copy(
                        activeTileDetail = TileDetailState(
                            type = TileDetailType.BLUETOOTH,
                            title = "Bluetooth",
                            subtitle = if (tile.isActive) deviceName else "Off",
                            isActive = tile.isActive,
                            btDeviceName = deviceName,
                            btAudioConnected = audioDevice != null,
                            settingsAction = tile.settingsAction ?: Settings.ACTION_BLUETOOTH_SETTINGS,
                        )
                    )
                }
            }
            else -> {
                tile.settingsAction?.let { action ->
                    try {
                        context.startActivity(Intent(action).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun closeTileDetail() {
        _state.update { it.copy(activeTileDetail = null) }
    }

    fun setTorchStrength(level: Int) {
        tileToggler.setTorchStrength(level)
        _state.update { current ->
            val detail = current.activeTileDetail ?: return@update current
            if (detail.type == TileDetailType.FLASHLIGHT) {
                current.copy(
                    activeTileDetail = detail.copy(
                        torchLevel = level,
                        subtitle = if (detail.isActive) "On • Level $level" else "Off"
                    )
                )
            } else current
        }
    }

    fun toggleTorchInDetail() {
        val currentDetail = _state.value.activeTileDetail ?: return
        val newActive = !currentDetail.isActive
        val flashlightTile = _state.value.tiles.firstOrNull { it.id.lowercase().contains("flashlight") }
        if (flashlightTile != null) {
            toggleTile(flashlightTile)
        }
        if (newActive) {
            tileToggler.setTorchStrength(currentDetail.torchLevel)
        }
        _state.update { current ->
            val detail = current.activeTileDetail ?: return@update current
            current.copy(
                activeTileDetail = detail.copy(
                    isActive = newActive,
                    subtitle = if (newActive) "On • Level ${detail.torchLevel}" else "Off"
                )
            )
        }
    }

    fun dismissNotification(key: String) {
        notificationRepo.cancelAndRemove(key)
    }

    fun clearAllNotifications() {
        notificationRepo.cancelAll()
    }

    fun snoozeNotification(key: String, delayMs: Long) {
        notificationRepo.snooze(key, delayMs)
        viewModelScope.launch {
            delay(delayMs)
            notificationRepo.refresh()
        }
    }

    fun launchNotification(notification: com.supershade.domain.notification.model.ShadeNotification) {
        try {
            notification.contentIntent?.send()
        } catch (_: Exception) {}
        close()
    }

    // --- Media transport controls ---

    fun mediaPlayPause() {
        if (state.value.media?.isPlaying == true) mediaRepo.pause() else mediaRepo.play()
    }

    fun mediaSkipNext() { mediaRepo.skipNext() }

    fun mediaSkipPrevious() { mediaRepo.skipPrevious() }

    fun mediaSeek(positionMs: Long) {
        mediaRepo.seekTo(positionMs)
        // Immediately reflect the seek in the UI so the thumb snaps to the
        // new position without waiting for the next MediaController callback.
        _state.update { s ->
            s.copy(media = s.media?.copy(position = positionMs))
        }
    }

    // --- Brightness ---

    fun setBrightness(value: Int) {
        viewModelScope.launch {
            brightnessRepo.set(value)
            val actual = brightnessRepo.getCurrent()
            _state.update { it.copy(brightness = actual) }
        }
    }

    // --- Power & Security actions ---

    fun lockScreen() {
        viewModelScope.launch {
            close()
            val handled = com.supershade.service.SuperShadeAccessibilityService.instance?.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
            ) == true
            if (!handled) {
                governor.runShell("input", "keyevent", "26")
            }
        }
    }

    fun restartDevice() {
        viewModelScope.launch {
            close()
            val handled = governor.runShell("svc", "power", "reboot")
            if (!handled) {
                governor.runShell("reboot")
            }
        }
    }

    fun powerOffDevice() {
        viewModelScope.launch {
            close()
            val handled = governor.runShell("svc", "power", "shutdown")
            if (!handled) {
                governor.runShell("reboot", "-p")
            }
        }
    }

    fun openSystemPowerDialog() {
        viewModelScope.launch {
            close()
            val handled = com.supershade.service.SuperShadeAccessibilityService.instance?.performGlobalAction(
                android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG
            ) == true
            if (!handled) {
                governor.runShell("input", "keyevent", "--longpress", "26")
            }
        }
    }

    // --- Lifecycle ---

    override fun onCleared() {
        super.onCleared()
        positionTickerJob?.cancel()
        mediaRepo.dispose()
    }

    // ---------------------------------------------------------------------------

    private fun filterFor(
        notifications: List<com.supershade.domain.notification.model.ShadeNotification>,
        category: ShadeCategory
    ) = if (category == ShadeCategory.All) notifications
        else notifications.filter { it.category == category }
}
