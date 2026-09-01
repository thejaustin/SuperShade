package com.supershade.domain.tile

import android.content.Context
import com.supershade.shizuku.StatusBarGovernor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    // Bug 4b fix: reverse-lookup map so OEM component names returned by
    // "settings get secure sysui_qs_tiles" are normalised to short IDs.
    private val componentToId: Map<String, String> =
        TILE_COMPONENTS.entries.associate { (id, comp) -> comp to id }

    init {
        // Attempt immediately — may return "" if Shizuku isn't connected yet.
        scope.launch { loadTiles() }

        // Bug 4a fix: retry once after 3 s to catch the common case where
        // Shizuku binds shortly after app start.  Only retries when the first
        // attempt produced no usable data (empty list or all SETTINGS_INTENT
        // fallbacks, which signals that getCurrentTiles() returned "").
        scope.launch {
            delay(3_000L)
            if (_tiles.value.isEmpty() ||
                _tiles.value.all { it.capability == TileCapability.SETTINGS_INTENT }) {
                loadTiles()
            }
        }
    }

    /** Public reload entry-point — callers (e.g. StatusBarGovernor) can trigger
     *  a fresh fetch once they know the commander is ready. */
    fun reload() {
        scope.launch { loadTiles() }
    }

    private suspend fun loadTiles() {
        val raw = governor.getCurrentTiles()
        val tokens = if (raw.isBlank()) KNOWN_TILES.keys.take(8).toList()
                     else raw.split(",").map { it.trim() }

        _tiles.value = tokens.map { token ->
            // Bug 4b fix: if the token is a full component name (OEM devices),
            // map it back to the short id before looking up KNOWN_TILES.
            val id = componentToId[token] ?: token
            val (label, capability) = KNOWN_TILES[id] ?: (id to TileCapability.SETTINGS_INTENT)
            val componentName = TILE_COMPONENTS[id]
            TileDefinition(
                id = id,
                label = label,
                isActive = false,
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
}
