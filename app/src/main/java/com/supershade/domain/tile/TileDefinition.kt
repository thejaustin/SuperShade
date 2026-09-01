package com.supershade.domain.tile

enum class TileCapability { FULL_TOGGLE, SETTINGS_INTENT, READ_ONLY }

data class TileDefinition(
    val id: String,
    val label: String,
    val isActive: Boolean,
    val capability: TileCapability,
    val settingsAction: String? = null,
    // Bug 3 fix: full component name required by "cmd statusbar click-tile"
    val componentName: String? = null,
)

val KNOWN_TILES: Map<String, Pair<String, TileCapability>> = mapOf(
    // Connectivity
    "internet"     to ("Internet"       to TileCapability.FULL_TOGGLE),
    "wifi"         to ("Wi-Fi"          to TileCapability.FULL_TOGGLE),
    "bt"           to ("Bluetooth"      to TileCapability.FULL_TOGGLE),
    "nfc"          to ("NFC"            to TileCapability.FULL_TOGGLE),
    "hotspot"      to ("Hotspot"        to TileCapability.FULL_TOGGLE),
    "airplane"     to ("Airplane"       to TileCapability.FULL_TOGGLE),
    "cell"         to ("Mobile Data"    to TileCapability.FULL_TOGGLE),
    "vpn"          to ("VPN"            to TileCapability.SETTINGS_INTENT),
    // Display
    "dark"         to ("Dark Mode"      to TileCapability.FULL_TOGGLE),
    "night"        to ("Night Light"    to TileCapability.FULL_TOGGLE),
    "rotation"     to ("Auto Rotate"    to TileCapability.FULL_TOGGLE),
    "cast"         to ("Cast"           to TileCapability.SETTINGS_INTENT),
    "screenrecord" to ("Screen Record"  to TileCapability.FULL_TOGGLE),
    // Sound & Utilities
    "dnd"          to ("Do Not Disturb" to TileCapability.FULL_TOGGLE),
    "flashlight"   to ("Flashlight"     to TileCapability.FULL_TOGGLE),
    "mute"         to ("Mute"           to TileCapability.FULL_TOGGLE),
    "volume"       to ("Volume"         to TileCapability.SETTINGS_INTENT),
    // Power & Device
    "battery"      to ("Battery Saver"  to TileCapability.SETTINGS_INTENT),
    "powershare"   to ("Wireless Share" to TileCapability.FULL_TOGGLE),
    "location"     to ("Location"       to TileCapability.SETTINGS_INTENT),
    "alarm"        to ("Alarm"          to TileCapability.SETTINGS_INTENT),
    // Sync & Accessibility
    "sync"         to ("Sync"           to TileCapability.FULL_TOGGLE),
    "datasaver"    to ("Data Saver"     to TileCapability.SETTINGS_INTENT),
    "work"         to ("Work Profile"   to TileCapability.FULL_TOGGLE),
    "onehanded"    to ("One-Handed"     to TileCapability.FULL_TOGGLE),
)

// Bug 3 fix: map short tile IDs to the fully-qualified component names that
// "cmd statusbar click-tile" requires.
val TILE_COMPONENTS: Map<String, String> = mapOf(
    "internet"     to "com.android.systemui/.qs.tiles.InternetTile",
    "wifi"         to "com.android.systemui/.qs.tiles.WifiTile",
    "bt"           to "com.android.systemui/.qs.tiles.BluetoothTile",
    "nfc"          to "com.android.systemui/.qs.tiles.NfcTile",
    "hotspot"      to "com.android.systemui/.qs.tiles.HotspotTile",
    "airplane"     to "com.android.systemui/.qs.tiles.AirplaneModeTile",
    "cell"         to "com.android.systemui/.qs.tiles.CellularTile",
    "dark"         to "com.android.systemui/.qs.tiles.UiModeNightTile",
    "night"        to "com.android.systemui/.qs.tiles.NightDisplayTile",
    "rotation"     to "com.android.systemui/.qs.tiles.RotationLockTile",
    "cast"         to "com.android.systemui/.qs.tiles.CastTile",
    "screenrecord" to "com.android.systemui/.qs.tiles.ScreenRecordTile",
    "dnd"          to "com.android.systemui/.qs.tiles.DndTile",
    "flashlight"   to "com.android.systemui/.qs.tiles.FlashlightTile",
    "mute"         to "com.android.systemui/.qs.tiles.MuteModeTile",
    "battery"      to "com.android.systemui/.qs.tiles.BatterySaverTile",
    "location"     to "com.android.systemui/.qs.tiles.LocationTile",
    "alarm"        to "com.android.systemui/.qs.tiles.AlarmTile",
    "sync"         to "com.android.systemui/.qs.tiles.SyncTile",
    "datasaver"    to "com.android.systemui/.qs.tiles.DataSaverTile",
    "work"         to "com.android.systemui/.qs.tiles.WorkModeTile",
    "onehanded"    to "com.android.systemui/.qs.tiles.OneHandedModeTile",
)
