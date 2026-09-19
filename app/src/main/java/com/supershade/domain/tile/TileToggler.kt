package com.supershade.domain.tile

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings
import com.supershade.service.NotificationCollector
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.StatusBarGovernor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TileToggler(
    private val context: Context,
    private val governor: StatusBarGovernor,
    private val tileRepo: TileRepository? = null,
    private val settings: ShadeSettings? = null,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var preferredTorchLevel: Int = 3

    init {
        settings?.let { s ->
            coroutineScope.launch {
                s.torchStrengthLevel.collect { preferredTorchLevel = it }
            }
        }
    }
    suspend fun toggle(tile: TileDefinition) {
        val id = tile.id.lowercase()
        val newState = !tile.isActive

        // Optimistically update visual state immediately so touch feels instant
        tileRepo?.setTileActiveOptimistic(tile.id, newState)

        try {
            when {
                // Flashlight: direct CameraManager
                id.contains("flashlight") -> toggleFlashlight(newState)

                // Auto-Rotate: direct Settings.System if WRITE_SETTINGS granted, else Shizuku or Settings
                id.contains("rotation") || id.contains("rotationlock") -> {
                    if (governor.canRunPrivileged) {
                        governor.runShell("settings", "put", "system", "accelerometer_rotation", if (newState) "1" else "0")
                    } else if (Settings.System.canWrite(context)) {
                        try {
                            Settings.System.putInt(
                                context.contentResolver,
                                Settings.System.ACCELEROMETER_ROTATION,
                                if (newState) 1 else 0
                            )
                        } catch (_: Exception) {}
                    } else {
                        launchWriteSettingsOrSettings(tile)
                    }
                    tileRepo?.reload()
                }

                // Do Not Disturb: direct NotificationManager or NotificationCollector listener
                id.contains("dnd") || id.contains("donotdisturb") -> {
                    if (governor.canRunPrivileged) {
                        governor.runShell("cmd", "notification", "set_dnd", if (newState) "on" else "off")
                    } else {
                        val filter = if (newState) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                                     else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                        if (nm?.isNotificationPolicyAccessGranted == true) {
                            try { nm.setInterruptionFilter(filter) } catch (_: Exception) {}
                        } else {
                            try {
                                NotificationCollector.instance?.requestInterruptionFilter(filter)
                            } catch (_: Exception) {
                                openSettings(tile)
                            }
                        }
                    }
                }

                // Sound Mode / Mute: direct AudioManager
                id.contains("mute") || id.contains("sound") -> {
                    val am = context.getSystemService(android.media.AudioManager::class.java)
                    try {
                        when (am.ringerMode) {
                            android.media.AudioManager.RINGER_MODE_NORMAL -> am.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
                            android.media.AudioManager.RINGER_MODE_VIBRATE -> {
                                val nm = context.getSystemService(android.app.NotificationManager::class.java)
                                if (nm.isNotificationPolicyAccessGranted) {
                                    am.ringerMode = android.media.AudioManager.RINGER_MODE_SILENT
                                } else {
                                    am.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                                }
                            }
                            else -> am.ringerMode = android.media.AudioManager.RINGER_MODE_NORMAL
                        }
                        tileRepo?.reload()
                    } catch (_: Exception) {
                        openSettings(tile)
                    }
                }

                // Master Sync: direct ContentResolver
                id.contains("sync") -> {
                    try {
                        android.content.ContentResolver.setMasterSyncAutomatically(newState)
                    } catch (_: Exception) {
                        openSettings(tile)
                    }
                }

                // Internet / Wi-Fi / Mobile Data: Shizuku privileged or native Floating Internet Panel
                id.contains("wifi") || id.contains("internet") || id.contains("cell") || id.contains("data") -> {
                    if (governor.canRunPrivileged) {
                        if (id.contains("cell") || id.contains("data")) {
                            governor.runShell("svc", "data", if (newState) "enable" else "disable")
                        } else {
                            governor.runShell("svc", "wifi", if (newState) "enable" else "disable")
                        }
                    } else {
                        try {
                            val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(panelIntent)
                        } catch (_: Exception) {
                            openSettings(tile)
                        }
                    }
                }

                // NFC: Shizuku privileged or native Floating NFC Panel
                id.contains("nfc") -> {
                    if (governor.canRunPrivileged) {
                        governor.runShell("svc", "nfc", if (newState) "enable" else "disable")
                    } else {
                        try {
                            val panelIntent = Intent(Settings.Panel.ACTION_NFC)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(panelIntent)
                        } catch (_: Exception) {
                            openSettings(tile)
                        }
                    }
                }

                // Volume: native Floating Volume Panel
                id.contains("volume") -> {
                    try {
                        val panelIntent = Intent(Settings.Panel.ACTION_VOLUME)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(panelIntent)
                    } catch (_: Exception) {
                        openSettings(tile)
                    }
                }

                // Bluetooth: Shizuku privileged (svc bluetooth) or Intent request enable
                id.contains("bt") || id.contains("bluetooth") -> {
                    if (governor.canRunPrivileged) {
                        governor.runShell("svc", "bluetooth", if (newState) "enable" else "disable")
                    } else if (newState) {
                        try {
                            val intent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            openSettings(tile)
                        }
                    } else {
                        openSettings(tile)
                    }
                }

                tile.capability == TileCapability.FULL_TOGGLE && governor.canRunPrivileged -> {
                    togglePrivileged(tile)
                }

                else -> openSettings(tile)
            }
        } finally {
            // Confirm actual hardware state after driver applies change
            delay(250L)
            tileRepo?.refreshActiveStates()
        }
    }

    private fun launchWriteSettingsOrSettings(tile: TileDefinition) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openSettings(tile)
        }
    }

    private fun toggleFlashlight(on: Boolean) {
        try {
            val cm = context.getSystemService(CameraManager::class.java)
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val chars = cm.getCameraCharacteristics(cameraId)
                val maxStrength = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                if (maxStrength > 1) {
                    if (on) {
                        val level = preferredTorchLevel.coerceIn(1, maxStrength)
                        cm.turnOnTorchWithStrengthLevel(cameraId, level)
                    } else {
                        cm.setTorchMode(cameraId, false)
                    }
                    return
                }
            }
            cm.setTorchMode(cameraId, on)
        } catch (_: Exception) {}
    }

    fun setTorchStrength(level: Int) {
        preferredTorchLevel = level
        settings?.let { s ->
            coroutineScope.launch { s.setTorchStrengthLevel(level) }
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return
        try {
            val cm = context.getSystemService(CameraManager::class.java)
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            val chars = cm.getCameraCharacteristics(cameraId)
            val maxStrength = chars.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            if (maxStrength > 1) {
                val clamped = level.coerceIn(1, maxStrength)
                cm.turnOnTorchWithStrengthLevel(cameraId, clamped)
            }
        } catch (_: Exception) {}
    }

    fun getTorchMaxStrength(): Int {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return 1
        return try {
            val cm = context.getSystemService(CameraManager::class.java)
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return 1
            cm.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } catch (_: Exception) {
            1
        }
    }

    private suspend fun togglePrivileged(tile: TileDefinition) {
        val id = tile.id.lowercase()
        val newState = !tile.isActive

        when {
            id.contains("wifi") || id.contains("internet") -> {
                governor.runShell("svc", "wifi", if (newState) "enable" else "disable")
            }
            id.contains("bt") || id.contains("bluetooth") -> {
                governor.runShell("svc", "bluetooth", if (newState) "enable" else "disable")
            }
            id.contains("dark") || id.contains("uimodenight") || id.contains("night") -> {
                governor.runShell("cmd", "uimode", "night", if (newState) "yes" else "no")
            }
            id.contains("rotation") || id.contains("rotationlock") -> {
                governor.runShell("settings", "put", "system", "accelerometer_rotation", if (newState) "1" else "0")
            }
            id.contains("airplane") -> {
                governor.runShell("cmd", "connectivity", "airplane-mode", if (newState) "enable" else "disable")
            }
            id.contains("location") -> {
                governor.runShell("cmd", "location", "set-location-enabled", if (newState) "true" else "false")
            }
            id.contains("nfc") -> {
                governor.runShell("svc", "nfc", if (newState) "enable" else "disable")
            }
            id.contains("cell") || id.contains("cellular") || id.contains("data") -> {
                governor.runShell("svc", "data", if (newState) "enable" else "disable")
            }
            id.contains("dnd") || id.contains("donotdisturb") -> {
                governor.runShell("cmd", "notification", "set_dnd", if (newState) "on" else "off")
            }
            id.contains("battery") || id.contains("batterymode") -> {
                governor.runShell("cmd", "power", "set-mode", if (newState) "1" else "0")
            }
            else -> {
                val component = tile.componentName ?: tile.id
                governor.clickTile(component)
            }
        }
    }

    private fun openSettings(tile: TileDefinition) {
        val action = tile.settingsAction ?: when (tile.id.lowercase()) {
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "battery", "batterymode" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "vpn" -> Settings.ACTION_VPN_SETTINGS
            "cast" -> Settings.ACTION_CAST_SETTINGS
            "volume" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}
