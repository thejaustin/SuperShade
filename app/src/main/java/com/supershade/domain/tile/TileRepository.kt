package com.supershade.domain.tile

import android.content.Context
import com.supershade.shizuku.StatusBarGovernor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TileRepository(
    private val context: Context,
    private val governor: StatusBarGovernor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _tiles = MutableStateFlow<List<TileDefinition>>(emptyList())
    val tiles: StateFlow<List<TileDefinition>> = _tiles.asStateFlow()

    init {
        scope.launch { loadTiles() }
    }

    private suspend fun loadTiles() {
        val raw = governor.getCurrentTiles()
        val ids = if (raw.isBlank()) KNOWN_TILES.keys.take(8).toList()
                  else raw.split(",").map { it.trim() }
        _tiles.value = ids.map { id ->
            val (label, capability) = KNOWN_TILES[id] ?: (id to TileCapability.SETTINGS_INTENT)
            TileDefinition(id = id, label = label, isActive = false, capability = capability)
        }
    }

    fun setTileActive(id: String, active: Boolean) {
        _tiles.value = _tiles.value.map {
            if (it.id == id) it.copy(isActive = active) else it
        }
    }
}
