package com.supershade.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.supershade.ui.theme.ShadeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class QsTileTapAction {
    TOGGLE_ACTIVE,
    OPEN_SHADE,
    SHOW_MENU,
}

enum class AccentColor(val label: String, val hex: Long) {
    GALAXY_BLUE("Galaxy Blue", 0xFF2575FC),
    EMERALD("Emerald", 0xFF10B981),
    VIOLET("Violet", 0xFF8B5CF6),
    AMBER("Amber", 0xFFF59E0B),
    CORAL("Coral", 0xFFF43F5E),
    MONET("Dynamic", 0L);
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supershade_prefs")

class ShadeSettings(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
        private val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
        private val IS_ACTIVE_KEY = booleanPreferencesKey("is_active")
        private val ENABLED_TILES_KEY = stringPreferencesKey("enabled_tiles")
        private val LAST_SEEN_VERSION_KEY = stringPreferencesKey("last_seen_version")
        private val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check_ms")
        private val BLOCK_SYSTEM_SHADE_KEY = booleanPreferencesKey("block_system_shade")
        private val QS_TILE_TAP_ACTION_KEY = stringPreferencesKey("qs_tile_tap_action")
    }

    val theme: Flow<ShadeTheme> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "pixel" -> ShadeTheme.Pixel
            "material" -> ShadeTheme.PureMaterial
            else -> ShadeTheme.OneUI
        }
    }

    val darkThemeMode: Flow<com.supershade.ui.theme.DarkThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[DARK_MODE_KEY]) {
            "dark" -> com.supershade.ui.theme.DarkThemeMode.DARK
            "light" -> com.supershade.ui.theme.DarkThemeMode.LIGHT
            "amoled" -> com.supershade.ui.theme.DarkThemeMode.AMOLED
            else -> com.supershade.ui.theme.DarkThemeMode.SYSTEM
        }
    }

    val accentColor: Flow<AccentColor> = context.dataStore.data.map { prefs ->
        when (prefs[ACCENT_COLOR_KEY]) {
            "emerald" -> AccentColor.EMERALD
            "violet" -> AccentColor.VIOLET
            "amber" -> AccentColor.AMBER
            "coral" -> AccentColor.CORAL
            "monet" -> AccentColor.MONET
            else -> AccentColor.GALAXY_BLUE
        }
    }

    val isActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_ACTIVE_KEY] ?: false
    }

    val enabledTiles: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[ENABLED_TILES_KEY] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }
    }

    val lastSeenVersion: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_SEEN_VERSION_KEY] ?: ""
    }

    val lastUpdateCheckMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_UPDATE_CHECK_KEY] ?: 0L
    }

    val blockSystemShade: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BLOCK_SYSTEM_SHADE_KEY] ?: true
    }

    val qsTileTapAction: Flow<QsTileTapAction> = context.dataStore.data.map { prefs ->
        when (prefs[QS_TILE_TAP_ACTION_KEY]) {
            "open_shade" -> QsTileTapAction.OPEN_SHADE
            "show_menu" -> QsTileTapAction.SHOW_MENU
            else -> QsTileTapAction.TOGGLE_ACTIVE
        }
    }

    suspend fun setTheme(theme: ShadeTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = when (theme) {
                is ShadeTheme.Pixel -> "pixel"
                is ShadeTheme.PureMaterial -> "material"
                else -> "oneui"
            }
        }
    }

    suspend fun setDarkThemeMode(mode: com.supershade.ui.theme.DarkThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = when (mode) {
                com.supershade.ui.theme.DarkThemeMode.DARK -> "dark"
                com.supershade.ui.theme.DarkThemeMode.LIGHT -> "light"
                com.supershade.ui.theme.DarkThemeMode.AMOLED -> "amoled"
                com.supershade.ui.theme.DarkThemeMode.SYSTEM -> "system"
            }
        }
    }

    suspend fun setAccentColor(accent: AccentColor) {
        context.dataStore.edit { prefs ->
            prefs[ACCENT_COLOR_KEY] = when (accent) {
                AccentColor.EMERALD -> "emerald"
                AccentColor.VIOLET -> "violet"
                AccentColor.AMBER -> "amber"
                AccentColor.CORAL -> "coral"
                AccentColor.MONET -> "monet"
                AccentColor.GALAXY_BLUE -> "galaxy_blue"
            }
        }
    }

    suspend fun setActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ACTIVE_KEY] = active
        }
    }

    suspend fun setQsTileTapAction(action: QsTileTapAction) {
        context.dataStore.edit { prefs ->
            prefs[QS_TILE_TAP_ACTION_KEY] = when (action) {
                QsTileTapAction.OPEN_SHADE -> "open_shade"
                QsTileTapAction.SHOW_MENU -> "show_menu"
                QsTileTapAction.TOGGLE_ACTIVE -> "toggle_active"
            }
        }
    }

    suspend fun setEnabledTiles(tileIds: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[ENABLED_TILES_KEY] = tileIds.joinToString(",")
        }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SEEN_VERSION_KEY] = version
        }
    }

    suspend fun setLastUpdateCheckMs(ms: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_UPDATE_CHECK_KEY] = ms
        }
    }

    suspend fun setBlockSystemShade(block: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BLOCK_SYSTEM_SHADE_KEY] = block
        }
    }
}
