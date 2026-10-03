package com.supershade.domain.tile

import android.provider.Settings

enum class TileCapability { FULL_TOGGLE, SETTINGS_INTENT, READ_ONLY }

data class TileDefinition(
    val id: String,
    val label: String,
    val isActive: Boolean,
    val capability: TileCapability,
    val settingsAction: String? = null,
    // Bug 3 fix: full component name required by "cmd statusbar click-tile"
    val componentName: String? = null,
    // Optional secondary line shown below the tile label (SSID, mode name, etc.)
    val subtitle: String? = null,
)

val TILE_SETTINGS_ACTIONS: Map<String, String> = mapOf(
    "internet"     to Settings.ACTION_WIFI_SETTINGS,
    "wifi"         to Settings.ACTION_WIFI_SETTINGS,
    "bt"           to Settings.ACTION_BLUETOOTH_SETTINGS,
    "nfc"          to Settings.ACTION_NFC_SETTINGS,
    "hotspot"      to Settings.ACTION_WIRELESS_SETTINGS,
    "airplane"     to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
    "cell"         to Settings.ACTION_DATA_ROAMING_SETTINGS,
    "vpn"          to Settings.ACTION_VPN_SETTINGS,
    "dark"         to Settings.ACTION_DISPLAY_SETTINGS,
    "night"        to Settings.ACTION_DISPLAY_SETTINGS,
    "rotation"     to Settings.ACTION_DISPLAY_SETTINGS,
    "cast"         to Settings.ACTION_CAST_SETTINGS,
    "screenrecord" to Settings.ACTION_DISPLAY_SETTINGS,
    "dnd"          to Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
    "mute"         to Settings.ACTION_SOUND_SETTINGS,
    "volume"       to Settings.ACTION_SOUND_SETTINGS,
    "battery"      to Settings.ACTION_BATTERY_SAVER_SETTINGS,
    "location"     to Settings.ACTION_LOCATION_SOURCE_SETTINGS,
    "alarm"        to Settings.ACTION_DATE_SETTINGS,
    "sync"         to Settings.ACTION_SYNC_SETTINGS,
    "datasaver"    to Settings.ACTION_DATA_ROAMING_SETTINGS,
    "onehanded"    to Settings.ACTION_ACCESSIBILITY_SETTINGS,
)

val DEFAULT_TILES: List<String> = listOf(
    "wifi",
    "bt",
    "flashlight",
    "rotation",
    "dnd",
    "mute",
    "battery",
    "location",
    "dark",
    "airplane",
    "nfc",
    "hotspot",
)

/**
 * Curated canonical tiles catalogue without vendor duplicate aliases.
 * Used for the 'Add Available' tiles sheet and editor drawers.
 */
val CANONICAL_TILES: List<String> = listOf(
    "wifi",
    "bt",
    "cell",
    "airplane",
    "hotspot",
    "nfc",
    "flashlight",
    "rotation",
    "dnd",
    "mute",
    "battery",
    "dark",
    "night",
    "location",
    "screenrecord",
    "extra_dim",
    "powershare",
    "qr_code_scanner",
    "wallet",
    "controls",
    "notes",
    "cameratoggle",
    "mictoggle",
    "sensors_off",
    "quick_share",
    "smart_view",
    "dolby",
    "aod",
    "modes",
    "secure_folder",
    "protect_battery",
    "kids_mode",
    "link_to_windows",
    "dex",
    "music_share",
    "voicenote",
    "smartthings",
    "cast",
    "sync",
    "datasaver",
    "onehanded",
    "work",
    "alarm",
)

/**
 * Normalizes any vendor alias, SystemUI token, or component name to its canonical tile ID.
 * Prevents duplicate tiles (e.g. 'AirplaneMode' vs 'airplane', 'SoundMode' vs 'mute') from appearing.
 */
fun canonicalTileId(raw: String): String {
    val clean = raw.trim().lowercase()
    return when {
        clean == "internet" || clean == "wifi" || clean.contains("wifitile") || clean.contains("internettile") -> "wifi"
        clean == "bt" || clean == "bluetooth" || clean.contains("bluetoothtile") -> "bt"
        clean == "cell" || clean == "cellular" || clean == "mobiledata" || clean == "mobile_data" || clean.contains("cellulartile") -> "cell"
        clean == "airplane" || clean == "airplanemode" || clean.contains("airplanemodetile") -> "airplane"
        clean == "hotspot" || clean.contains("tether") || clean.contains("hotspottile") -> "hotspot"
        clean == "nfc" || clean.contains("nfctile") -> "nfc"
        clean == "vpn" -> "vpn"
        clean == "rotation" || clean == "rotationlock" || clean == "autorotate" || clean.contains("rotationlocktile") || clean.contains("autorotatetile") -> "rotation"
        clean == "dark" || clean == "darkmode" || clean == "uimodenight" || clean.contains("uimodenighttile") -> "dark"
        clean == "night" || clean == "nightlight" || clean == "bluelightfilter" || clean.contains("nightdisplaytile") || clean.contains("bluelightfiltertile") -> "night"
        clean == "screenrecord" || clean == "screen_record" || clean.contains("screenrecordtile") -> "screenrecord"
        clean == "cast" || clean.contains("casttile") -> "cast"
        clean == "reducebrightcolors" || clean == "extra_dim" || clean == "extradim" || clean.contains("reducebrightcolorstile") -> "extra_dim"
        clean == "mute" || clean == "sound" || clean == "soundmode" || clean.contains("mutemodetile") || clean.contains("soundmodetile") -> "mute"
        clean == "dnd" || clean == "donotdisturb" || clean.contains("dndtile") -> "dnd"
        clean == "flashlight" || clean == "torch" || clean.contains("flashlighttile") -> "flashlight"
        clean == "battery" || clean == "batterymode" || clean == "batterysaver" || clean.contains("batterysavertile") -> "battery"
        clean == "powershare" || clean == "wirelesspowersharing" || clean.contains("powersharetile") -> "powershare"
        clean == "location" || clean == "gps" || clean.contains("locationtile") -> "location"
        clean == "cameratoggle" || clean == "camera_toggle" || clean == "cameraaccess" || clean.contains("cameratoggletile") -> "cameratoggle"
        clean == "mictoggle" || clean == "mic_toggle" || clean == "micaccess" || clean.contains("mictoggletile") -> "mictoggle"
        clean == "sensorprivacy" || clean == "sensors_off" || clean == "sensorsoff" || clean.contains("sensorprivacytile") -> "sensors_off"
        clean == "qr_code_scanner" || clean == "qrcodescanner" || clean == "qrcode" || clean.contains("qrcodescannertile") -> "qr_code_scanner"
        clean == "wallet" || clean == "quickaccesswallet" || clean.contains("quickaccesswallettile") -> "wallet"
        clean == "controls" || clean == "devicecontrols" || clean.contains("devicecontrolstile") -> "controls"
        clean == "notes" || clean == "quicknote" || clean.contains("notestile") -> "notes"
        clean == "sync" || clean.contains("synctile") -> "sync"
        clean == "datasaver" || clean.contains("datasavertile") -> "datasaver"
        clean == "onehanded" || clean.contains("onehandedmodetile") -> "onehanded"
        clean == "work" || clean == "workprofile" || clean.contains("workmodetile") -> "work"
        clean == "quick_share" || clean == "quickshare" || clean == "nearby_share" || clean == "nearbyshare" || clean.contains("quicksharetile") -> "quick_share"
        clean == "smart_view" || clean == "smartview" || clean == "smartmirroring" || clean.contains("smartviewtile") -> "smart_view"
        clean == "dolby" || clean == "dolby_atmos" || clean == "dolbyatmos" || clean.contains("dolbytile") -> "dolby"
        clean == "aod" || clean == "alwaysondisplay" || clean == "always_on_display" || clean.contains("aodtile") -> "aod"
        clean == "modes" || clean == "routines" || clean.contains("modestile") -> "modes"
        clean == "secure_folder" || clean == "securefolder" || clean.contains("securefoldertile") -> "secure_folder"
        clean == "protect_battery" || clean == "protectbattery" || clean.contains("protectbatterytile") -> "protect_battery"
        clean == "kids_mode" || clean == "kids" || clean == "kidsmode" || clean.contains("kidsmodetile") -> "kids_mode"
        clean == "link_to_windows" || clean == "linktowindows" || clean.contains("linktowindowstile") -> "link_to_windows"
        clean == "dex" || clean == "desktopmode" || clean.contains("dextile") -> "dex"
        clean == "music_share" || clean == "musicshare" || clean.contains("musicsharetile") -> "music_share"
        clean == "voicenote" || clean == "voicerecorder" || clean.contains("voicenotetile") -> "voicenote"
        clean == "smartthings" || clean.contains("smartthingstile") -> "smartthings"
        clean == "hearing_devices" || clean == "hearingdevices" || clean.contains("hearingdevicestile") -> "hearing_devices"
        clean == "font_scaling" || clean == "fontscaling" || clean.contains("fontscalingtile") -> "font_scaling"
        else -> raw
    }
}

val KNOWN_TILES: Map<String, Pair<String, TileCapability>> = mapOf(
    // Connectivity
    "internet"           to ("Internet"          to TileCapability.FULL_TOGGLE),
    "wifi"               to ("Wi-Fi"             to TileCapability.FULL_TOGGLE),
    "Wifi"               to ("Wi-Fi"             to TileCapability.FULL_TOGGLE),
    "bt"                 to ("Bluetooth"         to TileCapability.FULL_TOGGLE),
    "Bluetooth"          to ("Bluetooth"         to TileCapability.FULL_TOGGLE),
    "nfc"                to ("NFC"               to TileCapability.FULL_TOGGLE),
    "Nfc"                to ("NFC"               to TileCapability.FULL_TOGGLE),
    "hotspot"            to ("Hotspot"           to TileCapability.FULL_TOGGLE),
    "Hotspot"            to ("Hotspot"           to TileCapability.FULL_TOGGLE),
    "airplane"           to ("Airplane"          to TileCapability.FULL_TOGGLE),
    "AirplaneMode"       to ("Airplane"          to TileCapability.FULL_TOGGLE),
    "cell"               to ("Mobile Data"       to TileCapability.FULL_TOGGLE),
    "Cellular"           to ("Mobile Data"       to TileCapability.FULL_TOGGLE),
    "vpn"                to ("VPN"               to TileCapability.SETTINGS_INTENT),
    // Display
    "dark"               to ("Dark Mode"         to TileCapability.FULL_TOGGLE),
    "UiModeNight"        to ("Dark Mode"         to TileCapability.FULL_TOGGLE),
    "night"              to ("Night Light"       to TileCapability.FULL_TOGGLE),
    "BlueLightFilter"    to ("Eye Comfort"       to TileCapability.FULL_TOGGLE),
    "rotation"           to ("Auto Rotate"       to TileCapability.FULL_TOGGLE),
    "RotationLock"       to ("Auto Rotate"       to TileCapability.FULL_TOGGLE),
    "cast"               to ("Cast"              to TileCapability.SETTINGS_INTENT),
    "screenrecord"       to ("Screen Record"     to TileCapability.FULL_TOGGLE),
    "ScreenPrivacy"      to ("Screen Privacy"    to TileCapability.FULL_TOGGLE),
    "ReduceBrightColors" to ("Extra Dim"         to TileCapability.FULL_TOGGLE),
    // Sound & Utilities
    "dnd"                to ("Do Not Disturb"    to TileCapability.FULL_TOGGLE),
    "Dnd"                to ("Do Not Disturb"    to TileCapability.FULL_TOGGLE),
    "flashlight"         to ("Flashlight"        to TileCapability.FULL_TOGGLE),
    "Flashlight"         to ("Flashlight"        to TileCapability.FULL_TOGGLE),
    "mute"               to ("Sound"             to TileCapability.FULL_TOGGLE),
    "SoundMode"          to ("Sound"             to TileCapability.FULL_TOGGLE),
    "volume"             to ("Volume"            to TileCapability.SETTINGS_INTENT),
    // Power & Device
    "battery"            to ("Battery Saver"     to TileCapability.FULL_TOGGLE),
    "BatteryMode"        to ("Battery Saver"     to TileCapability.FULL_TOGGLE),
    "powershare"         to ("Wireless Share"    to TileCapability.FULL_TOGGLE),
    "PowerShare"         to ("Wireless Share"    to TileCapability.FULL_TOGGLE),
    "location"           to ("Location"          to TileCapability.FULL_TOGGLE),
    "Location"           to ("Location"          to TileCapability.FULL_TOGGLE),
    "alarm"              to ("Alarm"             to TileCapability.SETTINGS_INTENT),
    // Sync & Accessibility
    "sync"               to ("Sync"              to TileCapability.FULL_TOGGLE),
    "datasaver"          to ("Data Saver"        to TileCapability.SETTINGS_INTENT),
    "work"               to ("Work Profile"      to TileCapability.FULL_TOGGLE),
    "onehanded"          to ("One-Handed"        to TileCapability.FULL_TOGGLE),

    // Privacy & Security
    "cameratoggle"       to ("Camera Access"     to TileCapability.FULL_TOGGLE),
    "CameraToggle"       to ("Camera Access"     to TileCapability.FULL_TOGGLE),
    "camera_toggle"      to ("Camera Access"     to TileCapability.FULL_TOGGLE),
    "mictoggle"          to ("Mic Access"        to TileCapability.FULL_TOGGLE),
    "MicToggle"          to ("Mic Access"        to TileCapability.FULL_TOGGLE),
    "mic_toggle"         to ("Mic Access"        to TileCapability.FULL_TOGGLE),
    "SensorPrivacy"      to ("Sensors Off"       to TileCapability.FULL_TOGGLE),
    "sensors_off"        to ("Sensors Off"       to TileCapability.FULL_TOGGLE),

    // Utilities & Tools
    "qr_code_scanner"    to ("Scan QR"           to TileCapability.SETTINGS_INTENT),
    "QRCodeScanner"      to ("Scan QR"           to TileCapability.SETTINGS_INTENT),
    "qrcode"             to ("Scan QR"           to TileCapability.SETTINGS_INTENT),
    "font_scaling"       to ("Font Size"         to TileCapability.SETTINGS_INTENT),
    "FontScaling"        to ("Font Size"         to TileCapability.SETTINGS_INTENT),
    "hearing_devices"    to ("Hearing Devices"   to TileCapability.SETTINGS_INTENT),
    "HearingDevices"     to ("Hearing Devices"   to TileCapability.SETTINGS_INTENT),
    "wallet"             to ("Wallet"            to TileCapability.SETTINGS_INTENT),
    "Wallet"             to ("Wallet"            to TileCapability.SETTINGS_INTENT),
    "QuickAccessWallet"  to ("Wallet"            to TileCapability.SETTINGS_INTENT),
    "controls"           to ("Device Controls"   to TileCapability.SETTINGS_INTENT),
    "Controls"           to ("Device Controls"   to TileCapability.SETTINGS_INTENT),
    "DeviceControls"     to ("Device Controls"   to TileCapability.SETTINGS_INTENT),
    "notes"              to ("Quick Note"        to TileCapability.SETTINGS_INTENT),
    "Notes"              to ("Quick Note"        to TileCapability.SETTINGS_INTENT),
    "screen_record"      to ("Screen Record"     to TileCapability.FULL_TOGGLE),
    "ScreenRecord"       to ("Screen Record"     to TileCapability.FULL_TOGGLE),

    // Samsung One UI Specific
    "dolby"              to ("Dolby Atmos"       to TileCapability.FULL_TOGGLE),
    "Dolby"              to ("Dolby Atmos"       to TileCapability.FULL_TOGGLE),
    "dolby_atmos"        to ("Dolby Atmos"       to TileCapability.FULL_TOGGLE),
    "DolbyAtmos"         to ("Dolby Atmos"       to TileCapability.FULL_TOGGLE),
    "smart_view"         to ("Smart View"        to TileCapability.FULL_TOGGLE),
    "SmartView"          to ("Smart View"        to TileCapability.FULL_TOGGLE),
    "smartview"          to ("Smart View"        to TileCapability.FULL_TOGGLE),
    "SmartMirroring"     to ("Smart View"        to TileCapability.FULL_TOGGLE),
    "nearby_share"       to ("Quick Share"       to TileCapability.FULL_TOGGLE),
    "NearbyShare"        to ("Quick Share"       to TileCapability.FULL_TOGGLE),
    "quick_share"        to ("Quick Share"       to TileCapability.FULL_TOGGLE),
    "QuickShare"         to ("Quick Share"       to TileCapability.FULL_TOGGLE),
    "modes"              to ("Modes & Routines"  to TileCapability.SETTINGS_INTENT),
    "Modes"              to ("Modes & Routines"  to TileCapability.SETTINGS_INTENT),
    "Routines"           to ("Modes & Routines"  to TileCapability.SETTINGS_INTENT),
    "always_on_display"  to ("Always On Display" to TileCapability.FULL_TOGGLE),
    "Aod"                to ("Always On Display" to TileCapability.FULL_TOGGLE),
    "aod"                to ("Always On Display" to TileCapability.FULL_TOGGLE),
    "AlwaysOnDisplay"    to ("Always On Display" to TileCapability.FULL_TOGGLE),
    "secure_folder"      to ("Secure Folder"     to TileCapability.FULL_TOGGLE),
    "SecureFolder"       to ("Secure Folder"     to TileCapability.FULL_TOGGLE),
    "kids_mode"          to ("Kids"              to TileCapability.FULL_TOGGLE),
    "KidsMode"           to ("Kids"              to TileCapability.FULL_TOGGLE),
    "protect_battery"    to ("Protect Battery"   to TileCapability.FULL_TOGGLE),
    "ProtectBattery"     to ("Protect Battery"   to TileCapability.FULL_TOGGLE),
    "music_share"        to ("Music Share"       to TileCapability.FULL_TOGGLE),
    "MusicShare"         to ("Music Share"       to TileCapability.FULL_TOGGLE),
    "link_to_windows"    to ("Link to Windows"   to TileCapability.FULL_TOGGLE),
    "LinkToWindows"      to ("Link to Windows"   to TileCapability.FULL_TOGGLE),
    "dex"                to ("Samsung DeX"       to TileCapability.FULL_TOGGLE),
    "DeX"                to ("Samsung DeX"       to TileCapability.FULL_TOGGLE),
    "DesktopMode"        to ("Samsung DeX"       to TileCapability.FULL_TOGGLE),
    "voicenote"          to ("Voice Recorder"    to TileCapability.FULL_TOGGLE),
    "VoiceNote"          to ("Voice Recorder"    to TileCapability.FULL_TOGGLE),
    "voicerecorder"      to ("Voice Recorder"    to TileCapability.FULL_TOGGLE),
    "VoiceRecorder"      to ("Voice Recorder"    to TileCapability.FULL_TOGGLE),
    "smartthings"        to ("SmartThings"       to TileCapability.SETTINGS_INTENT),
    "SmartThings"        to ("SmartThings"       to TileCapability.SETTINGS_INTENT),
    "quickconnect"       to ("Quick Connect"     to TileCapability.SETTINGS_INTENT),
    "QuickConnect"       to ("Quick Connect"     to TileCapability.SETTINGS_INTENT),
    "bedtime"            to ("Bedtime Mode"      to TileCapability.FULL_TOGGLE),
    "Bedtime"            to ("Bedtime Mode"      to TileCapability.FULL_TOGGLE),
    "focus"              to ("Focus Mode"        to TileCapability.FULL_TOGGLE),
    "Focus"              to ("Focus Mode"        to TileCapability.FULL_TOGGLE),
    "livecaption"        to ("Live Caption"      to TileCapability.FULL_TOGGLE),
    "LiveCaption"        to ("Live Caption"      to TileCapability.FULL_TOGGLE),
    "cameramic"          to ("Camera & Mic"      to TileCapability.FULL_TOGGLE),
    "private_share"      to ("Private Share"     to TileCapability.SETTINGS_INTENT),
    "PrivateShare"       to ("Private Share"     to TileCapability.SETTINGS_INTENT),
)

val PACKAGE_FRIENDLY_NAMES: Map<String, String> = mapOf(
    "com.samsung.android.oneconnect" to "SmartThings",
    "com.sec.android.app.voicenote" to "Voice Recorder",
    "com.samsung.android.voicenote" to "Voice Recorder",
    "com.samsung.android.app.voicerecorder" to "Voice Recorder",
    "com.samsung.android.app.routines" to "Modes & Routines",
    "com.samsung.android.app.soundpicker" to "Sound Picker",
    "com.samsung.android.smartmirroring" to "Smart View",
    "com.samsung.android.mdecservice" to "Call & Text",
    "com.google.android.gms.nearby" to "Quick Share",
    "com.google.android.apps.recorder" to "Recorder",
    "com.google.android.apps.wellbeing" to "Digital Wellbeing",
    "com.google.android.projection.gearhead" to "Android Auto",
    "com.samsung.android.honeyboard" to "Samsung Keyboard",
    "com.samsung.android.lool" to "Device Care",
    "com.samsung.android.bixby.agent" to "Bixby",
    "com.sec.android.quickconnect" to "Quick Connect",
)

/**
 * Humanizes any raw tile token, custom(pkg/cls), or technical name into a clean,
 * user-facing title. Fixes broken tile names for third-party and vendor tiles.
 */
fun humanizeTileLabel(raw: String): String {
    // 1. Direct match in canonical ID or KNOWN_TILES
    val canonical = canonicalTileId(raw)
    KNOWN_TILES[canonical]?.first?.let { return it }
    KNOWN_TILES[raw]?.first?.let { return it }
    KNOWN_TILES[raw.lowercase()]?.first?.let { return it }

    // 2. Remove custom( ... ) wrapper if present
    var token = raw.trim()
    if (token.startsWith("custom(") && token.endsWith(")")) {
        token = token.substring(7, token.length - 1).trim()
    }

    // 3. Check for vendor package friendly name
    var pkgName: String? = null
    if (token.contains("/")) {
        pkgName = token.substringBefore("/")
        PACKAGE_FRIENDLY_NAMES[pkgName]?.let { return it }
        val cls = token.substringAfter("/")
        token = cls.substringAfterLast(".")
    } else if (token.contains(".")) {
        PACKAGE_FRIENDLY_NAMES[token]?.let { return it }
        token = token.substringAfterLast(".")
    }

    // 4. Strip vendor prefixes if present (sec_, sem_, samsung_, qcom_)
    val unvendor = token
        .removePrefix("sec_")
        .removePrefix("sem_")
        .removePrefix("samsung_")
        .removePrefix("qcom_")
        .removePrefix("Sec")
        .removePrefix("Sem")

    KNOWN_TILES[unvendor]?.first?.let { return it }
    KNOWN_TILES[unvendor.lowercase()]?.first?.let { return it }

    // 5. Remove common technical suffixes
    val stripped = unvendor
        .removeSuffix("TileService")
        .removeSuffix("Service")
        .removeSuffix("Tile")
        .removeSuffix("Setting")
        .removeSuffix("Toggle")
    val candidate = if (stripped.isNotBlank()) stripped else unvendor

    // 6. Check known names for candidate
    KNOWN_TILES[candidate]?.first?.let { return it }
    KNOWN_TILES[candidate.lowercase()]?.first?.let { return it }

    // 7. If candidate is too generic ("Tile", "Quick", "Main", "Default"), fallback to pkg
    if (candidate.equals("Tile", ignoreCase = true) ||
        candidate.equals("Quick", ignoreCase = true) ||
        candidate.equals("Service", ignoreCase = true) ||
        candidate.equals("Main", ignoreCase = true)
    ) {
        if (pkgName != null) {
            val pkgSegment = pkgName.substringAfterLast(".").replace(Regex("[-_]+"), " ")
            return pkgSegment.replaceFirstChar { it.uppercase() }
        }
    }

    // 8. Convert camelCase, PascalCase, snake_case or kebab-case to Title Case words
    val words = candidate
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1 $2")
        .replace(Regex("[-_]+"), " ")
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    return if (words.isNotBlank()) words else raw
}

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
