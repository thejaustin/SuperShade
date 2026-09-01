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

data class ShadeState(
    val isOpen: Boolean = false,
    val selectedCategory: ShadeCategory = ShadeCategory.All,
    val allNotifications: List<ShadeNotification> = emptyList(),
    val visibleNotifications: List<ShadeNotification> = emptyList(),
    val tiles: List<TileDefinition> = emptyList(),
    val media: MediaState? = null,
    val theme: ShadeTheme = ShadeTheme.OneUI,
    val statusBar: StatusBarState = StatusBarState(),
    val isShizukuConnected: Boolean = false,
    val brightness: Int = 128,
)
