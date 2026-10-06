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
    SOUND_MODE,
    DND,
    HOTSPOT,
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
    val ringerMode: Int = 2, // AudioManager.RINGER_MODE_NORMAL
    val mediaVol: Int = 0,
    val mediaMaxVol: Int = 15,
    val ringVol: Int = 0,
    val ringMaxVol: Int = 15,
    val notifVol: Int = 0,
    val notifMaxVol: Int = 15,
    val sysVol: Int = 0,
    val sysMaxVol: Int = 15,
    val alarmVol: Int = 0,
    val alarmMaxVol: Int = 15,
    val dndDurationMinutes: Int = 0, // 0 = until turned off
    val hotspotSsid: String? = null,
    val hotspotBand: String? = null,
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
    val activePanel: ShadePanel = ShadePanel.NOTIFICATIONS,
    val splitGestureMode: com.supershade.settings.SplitGestureMode = com.supershade.settings.SplitGestureMode.SEPARATE_70_30,
    val showPanelSwitcherPill: Boolean = false,
    val backdropTheme: com.supershade.ui.theme.BackdropTheme = com.supershade.ui.theme.BackdropTheme.FROSTED_GLASS,
    val backdropOpacity: Float = 0.78f,
    val notificationDensity: com.supershade.settings.NotificationDensity = com.supershade.settings.NotificationDensity.BALANCED,
    val hiddenChannels: Set<String> = emptySet(),
    val hideOngoingNotifications: Boolean = false,
    val deviceControlMode: com.supershade.settings.DeviceControlMode = com.supershade.settings.DeviceControlMode.SHOW_WHEN_EXPANDED,
    val monetAccentStrength: Float = 1.0f,
    val isHistorySheetOpen: Boolean = false,
    val dismissedHistory: List<com.supershade.domain.notification.DismissedNotificationRecord> = emptyList(),
    val pinnedKeys: Set<String> = emptySet(),
    val lastDismissedNotification: ShadeNotification? = null,
    val snoozedRecords: List<com.supershade.domain.notification.SnoozeRecord> = emptyList(),
    val classificationMode: com.supershade.settings.ClassificationMode = com.supershade.settings.ClassificationMode.ONE_UI,
    val showCategoryBar: Boolean = true,
    val appCategoryOverrides: Map<String, ShadeCategory> = emptyMap(),
) {
    val isNotificationCompact: Boolean get() = notificationDensity == com.supershade.settings.NotificationDensity.COMPACT || tileSize == com.supershade.settings.TileSize.COMPACT
}
