package com.supershade.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supershade.domain.brightness.BrightnessRepository
import com.supershade.domain.media.MediaRepository
import com.supershade.domain.notification.NotificationRepository
import com.supershade.domain.notification.model.ShadeCategory
import com.supershade.domain.tile.TileDefinition
import com.supershade.domain.tile.TileRepository
import com.supershade.domain.tile.TileToggler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShadeViewModel(
    private val notificationRepo: NotificationRepository,
    private val tileRepo: TileRepository,
    private val tileToggler: TileToggler,
    private val mediaRepo: MediaRepository,
    private val brightnessRepo: BrightnessRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ShadeState())
    val state: StateFlow<ShadeState> = _state.asStateFlow()

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
            .onEach { media -> _state.update { it.copy(media = media) } }
            .launchIn(viewModelScope)
    }

    fun open() {
        _state.update { it.copy(isOpen = true, brightness = brightnessRepo.getCurrent()) }
        mediaRepo.refresh()
    }

    fun close() {
        _state.update { it.copy(isOpen = false) }
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

    fun dismissNotification(key: String) {
        notificationRepo.onNotificationRemoved(key)
    }

    // --- Media transport controls ---

    fun mediaPlayPause() {
        if (state.value.media?.isPlaying == true) mediaRepo.pause() else mediaRepo.play()
    }

    fun mediaSkipNext() { mediaRepo.skipNext() }

    fun mediaSkipPrevious() { mediaRepo.skipPrevious() }

    // --- Brightness ---

    fun setBrightness(value: Int) {
        brightnessRepo.set(value)
        _state.update { it.copy(brightness = value) }
    }

    // --- Lifecycle ---

    override fun onCleared() {
        super.onCleared()
        mediaRepo.dispose()
    }

    // ---------------------------------------------------------------------------

    private fun filterFor(
        notifications: List<com.supershade.domain.notification.model.ShadeNotification>,
        category: ShadeCategory
    ) = if (category == ShadeCategory.All) notifications
        else notifications.filter { it.category == category }
}
