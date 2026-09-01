package com.supershade.domain.tile

enum class TileCapability { FULL_TOGGLE, SETTINGS_INTENT, READ_ONLY }

data class TileDefinition(
    val id: String,
    val label: String,
    val isActive: Boolean,
    val capability: TileCapability,
    val settingsAction: String? = null
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
