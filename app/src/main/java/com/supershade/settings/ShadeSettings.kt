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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "supershade_prefs")

class ShadeSettings(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
        private val IS_ACTIVE_KEY = booleanPreferencesKey("is_active")
        private val ENABLED_TILES_KEY = stringPreferencesKey("enabled_tiles")
        private val LAST_SEEN_VERSION_KEY = stringPreferencesKey("last_seen_version")
        private val LAST_UPDATE_CHECK_KEY = longPreferencesKey("last_update_check_ms")
    }

    val theme: Flow<ShadeTheme> = context.dataStore.data.map { prefs ->
        if (prefs[THEME_KEY] == "pixel") ShadeTheme.Pixel else ShadeTheme.OneUI
    }

    val darkThemeMode: Flow<com.supershade.ui.theme.DarkThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[DARK_MODE_KEY]) {
            "dark" -> com.supershade.ui.theme.DarkThemeMode.DARK
            "light" -> com.supershade.ui.theme.DarkThemeMode.LIGHT
            "amoled" -> com.supershade.ui.theme.DarkThemeMode.AMOLED
            else -> com.supershade.ui.theme.DarkThemeMode.SYSTEM
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

    suspend fun setTheme(theme: ShadeTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = if (theme is ShadeTheme.Pixel) "pixel" else "onui"
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

    suspend fun setActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ACTIVE_KEY] = active
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
}
