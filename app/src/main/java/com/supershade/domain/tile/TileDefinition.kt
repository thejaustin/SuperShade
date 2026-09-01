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
    "internet"   to ("Internet"   to TileCapability.FULL_TOGGLE),
    "bt"         to ("Bluetooth"  to TileCapability.FULL_TOGGLE),
    "airplane"   to ("Airplane"   to TileCapability.FULL_TOGGLE),
    "dnd"        to ("Do Not Disturb" to TileCapability.FULL_TOGGLE),
    "flashlight" to ("Flashlight" to TileCapability.FULL_TOGGLE),
    "rotation"   to ("Rotation"   to TileCapability.FULL_TOGGLE),
    "nfc"        to ("NFC"        to TileCapability.FULL_TOGGLE),
    "location"   to ("Location"   to TileCapability.SETTINGS_INTENT),
    "battery"    to ("Battery"    to TileCapability.SETTINGS_INTENT),
    "cast"       to ("Cast"       to TileCapability.SETTINGS_INTENT),
    "hotspot"    to ("Hotspot"    to TileCapability.FULL_TOGGLE),
)
