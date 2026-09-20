package com.supershade.viewmodel

import com.supershade.domain.media.MediaState
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.domain.notification.model.ShadeNotification
import com.supershade.domain.tile.TileDefinition
import com.supershade.ui.theme.ShadeTheme

data class StatusBarState(
    val time: String = "",
    val batteryPct: Int = 100,
    val isCharging: Boolean = false
)

enum class TileDetailType {
    FLASHLIGHT,
    WIFI,
    BLUETOOTH,
}

data class TileDetailState(
    val type: TileDetailType,
    val title: String,
    val subtitle: String? = null,
    val isActive: Boolean = false,
    val torchLevel: Int = 1,
    val maxTorchLevel: Int = 1,
    val wifiSsid: String? = null,
    val wifiBand: String? = null,
    val wifiIp: String? = null,
    val wifiLinkSpeed: String? = null,
    val wifiRssi: Int = 0,
    val btDeviceName: String? = null,
    val btAudioConnected: Boolean = false,
    val settingsAction: String? = null,
)

enum class ShadePanel {
    NOTIFICATIONS,
    QUICK_SETTINGS,
}

data class ShadeState(
    val isOpen: Boolean = false,
    val isQsExpanded: Boolean = false,
    val selectedCategory: ShadeCategory = ShadeCategory.All,
    val allNotifications: List<ShadeNotification> = emptyList(),
    val visibleNotifications: List<ShadeNotification> = emptyList(),
    val tiles: List<TileDefinition> = emptyList(),
    val media: MediaState? = null,
    val theme: ShadeTheme = ShadeTheme.OneUI,
    val statusBar: StatusBarState = StatusBarState(),
    val isShizukuConnected: Boolean = false,
    val brightness: Int = 128,
    val darkThemeMode: com.supershade.ui.theme.DarkThemeMode = com.supershade.ui.theme.DarkThemeMode.SYSTEM,
    val accentColor: com.supershade.settings.AccentColor = com.supershade.settings.AccentColor.GALAXY_BLUE,
    val tileShape: com.supershade.settings.TileShape = com.supershade.settings.TileShape.SQUIRCLE,
    val tileSize: com.supershade.settings.TileSize = com.supershade.settings.TileSize.STANDARD,
    val tileColumns: com.supershade.settings.TileGridColumns = com.supershade.settings.TileGridColumns.STANDARD,
    val showWideCards: Boolean = true,
    val activeTileDetail: TileDetailState? = null,
    val cardBorderWidth: com.supershade.settings.CardBorderWidth = com.supershade.settings.CardBorderWidth.THIN,
    val isQuickControlsTucked: Boolean = false,
    val activePanel: ShadePanel = ShadePanel.NOTIFICATIONS,
    val splitGestureMode: com.supershade.settings.SplitGestureMode = com.supershade.settings.SplitGestureMode.SEPARATE_70_30,
    val showPanelSwitcherPill: Boolean = false,
    val backdropTheme: com.supershade.ui.theme.BackdropTheme = com.supershade.ui.theme.BackdropTheme.FROSTED_GLASS,
    val backdropOpacity: Float = 0.78f,
)
