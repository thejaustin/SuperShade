package com.supershade.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.ShadeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class QsTileTapAction {
    TOGGLE_ACTIVE,
    OPEN_SHADE,
    SHOW_MENU,
}

enum class TileShape(val id: String, val label: String, val cornerRadiusDp: Int) {
    SQUIRCLE("squircle", "Squircle", 22),
    ROUNDED("rounded", "Rounded", 16),
    CIRCLE("circle", "Circle", 50),
    PILL("pill", "Stadium Pill", 28),
    SOFT("soft", "Soft Minimal", 12),
    LEAF("leaf", "Asymmetric Leaf", 24),
    SHARP("sharp", "Sharp Modern", 6);
}

enum class CardBorderWidth(val id: String, val label: String, val widthDp: Float) {
    NONE("none", "Borderless (0dp)", 0f),
    THIN("thin", "Subtle (1dp)", 1f),
    DISTINCT("distinct", "Distinct (1.5dp)", 1.5f),
    BOLD("bold", "Bold (2dp)", 2f);
}

enum class TileSize(
    val id: String,
    val label: String,
    val subtitle: String,
    val heightDp: Int,
    val iconSizeDp: Int,
) {
    COMPACT("compact", "Compact (58dp)", "Space-saving height; fits more notifications and media", 58, 20),
    STANDARD("standard", "Standard (72dp)", "Balanced One UI 8 height with clear icon, label & status", 72, 22),
    COMFORTABLE("comfortable", "Comfortable (84dp)", "Spacious height with large iconography for easy reach", 84, 26);
}

enum class TileGridColumns(val id: String, val label: String, val count: Int) {
    COMFORTABLE("comfortable", "Comfortable (3)", 3),
    STANDARD("standard", "Standard (4)", 4),
    COMPACT("compact", "Compact (5)", 5);
}

enum class AccentColor(val label: String, val hex: Long) {
    GALAXY_BLUE("Galaxy Blue", 0xFF2575FC),
    EMERALD("Emerald", 0xFF10B981),
    VIOLET("Violet", 0xFF8B5CF6),
    AMBER("Amber", 0xFFF59E0B),
    CORAL("Coral", 0xFFF43F5E),
    MONET("Dynamic", 0L);
}

enum class SplitGestureMode(
    val id: String,
    val label: String,
    val subtitle: String,
    val qsThreshold: Float,
    val isLeftQs: Boolean = false,
) {
    SEPARATE_70_30(
        "split_70_30",
        "Right 30% (Standard)",
        "Pull right 30% for Quick Settings, left 70% for Notifications",
        0.70f,
    ),
    SEPARATE_50_50(
        "split_50_50",
        "Half & Half (50/50)",
        "One UI 8 / iOS style: pull right half for Quick Settings, left half for Notifications",
        0.50f,
    ),
    SEPARATE_30_70(
        "split_30_70",
        "Left-Handed (30/70)",
        "Pull left 30% for Quick Settings, right 70% for Notifications",
        0.30f,
        isLeftQs = true,
    ),
    ALWAYS_NOTIFICATIONS(
        "always_notifs",
        "Notifications Only",
        "Pulling anywhere along the status bar always opens Notifications first",
        1.01f,
    ),
    ALWAYS_QUICK_SETTINGS(
        "always_qs",
        "Quick Settings Only",
        "Pulling anywhere along the status bar directly expands Quick Settings",
        -0.01f,
    );
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
        private val TILE_SHAPE_KEY = stringPreferencesKey("tile_shape")
        private val TILE_SIZE_KEY = stringPreferencesKey("tile_size")
        private val TILE_COLUMNS_KEY = stringPreferencesKey("tile_columns")
        private val SHOW_WIDE_CARDS_KEY = booleanPreferencesKey("show_wide_cards")
        private val SPLIT_GESTURE_MODE_KEY = stringPreferencesKey("split_gesture_mode")
        private val TORCH_STRENGTH_LEVEL_KEY = intPreferencesKey("torch_strength_level")
        private val CARD_BORDER_WIDTH_KEY = stringPreferencesKey("card_border_width")
        private val SHOW_PANEL_SWITCHER_PILL_KEY = booleanPreferencesKey("show_panel_switcher_pill")
        private val BACKDROP_THEME_KEY = stringPreferencesKey("backdrop_theme")
    }

    val backdropTheme: Flow<BackdropTheme> = context.dataStore.data.map { prefs ->
        BackdropTheme.fromId(prefs[BACKDROP_THEME_KEY])
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

    val tileShape: Flow<TileShape> = context.dataStore.data.map { prefs ->
        when (prefs[TILE_SHAPE_KEY]) {
            "rounded" -> TileShape.ROUNDED
            "circle" -> TileShape.CIRCLE
            "pill" -> TileShape.PILL
            "soft" -> TileShape.SOFT
            "leaf" -> TileShape.LEAF
            "sharp" -> TileShape.SHARP
            else -> TileShape.SQUIRCLE
        }
    }

    val tileSize: Flow<TileSize> = context.dataStore.data.map { prefs ->
        when (prefs[TILE_SIZE_KEY]) {
            "compact" -> TileSize.COMPACT
            "comfortable" -> TileSize.COMFORTABLE
            else -> TileSize.STANDARD
        }
    }

    val torchStrengthLevel: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[TORCH_STRENGTH_LEVEL_KEY] ?: 3
    }

    val tileColumns: Flow<TileGridColumns> = context.dataStore.data.map { prefs ->
        when (prefs[TILE_COLUMNS_KEY]) {
            "comfortable" -> TileGridColumns.COMFORTABLE
            "compact" -> TileGridColumns.COMPACT
            else -> TileGridColumns.STANDARD
        }
    }

    val showWideCards: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_WIDE_CARDS_KEY] ?: true
    }

    val splitGestureMode: Flow<SplitGestureMode> = context.dataStore.data.map { prefs ->
        when (prefs[SPLIT_GESTURE_MODE_KEY]) {
            "split_50_50" -> SplitGestureMode.SEPARATE_50_50
            "split_30_70" -> SplitGestureMode.SEPARATE_30_70
            "always_notifs" -> SplitGestureMode.ALWAYS_NOTIFICATIONS
            "always_qs" -> SplitGestureMode.ALWAYS_QUICK_SETTINGS
            else -> SplitGestureMode.SEPARATE_70_30
        }
    }

    val cardBorderWidth: Flow<CardBorderWidth> = context.dataStore.data.map { prefs ->
        when (prefs[CARD_BORDER_WIDTH_KEY]) {
            "none" -> CardBorderWidth.NONE
            "distinct" -> CardBorderWidth.DISTINCT
            "bold" -> CardBorderWidth.BOLD
            else -> CardBorderWidth.THIN
        }
    }

    val showPanelSwitcherPill: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHOW_PANEL_SWITCHER_PILL_KEY] ?: false
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

    suspend fun setTileShape(shape: TileShape) {
        context.dataStore.edit { prefs ->
            prefs[TILE_SHAPE_KEY] = shape.id
        }
    }

    suspend fun setTileSize(size: TileSize) {
        context.dataStore.edit { prefs ->
            prefs[TILE_SIZE_KEY] = size.id
        }
    }

    suspend fun setTorchStrengthLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[TORCH_STRENGTH_LEVEL_KEY] = level
        }
    }

    suspend fun setTileColumns(columns: TileGridColumns) {
        context.dataStore.edit { prefs ->
            prefs[TILE_COLUMNS_KEY] = columns.id
        }
    }

    suspend fun setShowWideCards(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_WIDE_CARDS_KEY] = show
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

    suspend fun setSplitGestureMode(mode: SplitGestureMode) {
        context.dataStore.edit { prefs ->
            prefs[SPLIT_GESTURE_MODE_KEY] = mode.id
        }
    }

    suspend fun setCardBorderWidth(width: CardBorderWidth) {
        context.dataStore.edit { prefs ->
            prefs[CARD_BORDER_WIDTH_KEY] = width.id
        }
    }

    suspend fun setShowPanelSwitcherPill(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_PANEL_SWITCHER_PILL_KEY] = show
        }
    }

    suspend fun setBackdropTheme(theme: BackdropTheme) {
        context.dataStore.edit { prefs ->
            prefs[BACKDROP_THEME_KEY] = theme.id
        }
    }
}
