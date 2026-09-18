package com.supershade

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.haptics.SuperHaptics
import com.supershade.domain.update.UpdateRepository
import com.supershade.service.NotificationCollector
import com.supershade.service.ShadeService
import com.supershade.service.SuperShadeTileService
import com.supershade.settings.QsTileTapAction
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.ui.settings.SettingsScreen
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.SuperShadeAppTheme
import com.supershade.ui.tile.TilePreferencesActivity
import com.supershade.ui.update.UpdateDialog
import com.supershade.ui.update.WhatsNewSheet
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val connector: ShizukuPlusConnector by inject()
    private val settings: ShadeSettings by inject()
    private val updateRepo: UpdateRepository by inject()
    private val shadeViewModel: ShadeViewModel by inject()
    private val shadeWindowManager: com.supershade.overlay.ShadeWindowManager by inject()
    private val governor: com.supershade.shizuku.StatusBarGovernor by inject()
    private val superHaptics: SuperHaptics by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalSuperHaptics provides superHaptics) {
                val scope = rememberCoroutineScope()
            val lifecycleOwner = LocalLifecycleOwner.current

            val shizukuConnected by connector.isConnected.collectAsState()
            val theme by settings.theme.collectAsState(initial = ShadeTheme.OneUI)
            val darkThemeMode by settings.darkThemeMode.collectAsState(initial = com.supershade.ui.theme.DarkThemeMode.SYSTEM)
            val accentColor by settings.accentColor.collectAsState(initial = com.supershade.settings.AccentColor.GALAXY_BLUE)
            val isActive by settings.isActive.collectAsState(initial = false)
            val blockSystemShade by settings.blockSystemShade.collectAsState(initial = true)
            val qsTileTapAction by settings.qsTileTapAction.collectAsState(initial = QsTileTapAction.TOGGLE_ACTIVE)
            val availableUpdate by updateRepo.availableUpdate.collectAsState()
            val isCheckingUpdate by updateRepo.isChecking.collectAsState()
            val showWhatsNew by updateRepo.showWhatsNew.collectAsState()

            SuperShadeAppTheme(mode = darkThemeMode, accentColor = accentColor) {
                // Re-checked on every resume so user sees instant feedback after
                // granting access in system Settings.
                var notifAccessGranted by remember { mutableStateOf(isNotificationAccessGranted()) }
                var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
                var shizukuPermGranted by remember { mutableStateOf(connector.hasPermission()) }
                var writeSettingsGranted by remember { mutableStateOf(Settings.System.canWrite(this)) }
                var accessibilityGranted by remember { mutableStateOf(isAccessibilityServiceEnabled()) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            notifAccessGranted = isNotificationAccessGranted()
                            overlayGranted = Settings.canDrawOverlays(this@MainActivity)
                            shizukuPermGranted = connector.hasPermission()
                            writeSettingsGranted = Settings.System.canWrite(this@MainActivity)
                            accessibilityGranted = isAccessibilityServiceEnabled()
                            if (isActive && notifAccessGranted && overlayGranted) {
                                toggleShadeService(true)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Ensure service is running if active in settings and permissions are ready
                LaunchedEffect(isActive, notifAccessGranted, overlayGranted) {
                    if (isActive && notifAccessGranted && overlayGranted) {
                        toggleShadeService(true)
                    }
                }

                // Request Shizuku permission as soon as it connects but isn't granted.
                LaunchedEffect(shizukuConnected) {
                    if (shizukuConnected && !connector.hasPermission()) {
                        connector.requestPermission(0)
                    }
                }

                // Detect fresh install / version upgrade → show What's New sheet
                LaunchedEffect(Unit) {
                    updateRepo.initSession()
                }

                // Throttled update check — once per 24 h
                LaunchedEffect(Unit) {
                    val lastCheck = settings.lastUpdateCheckMs.first()
                    val now = System.currentTimeMillis()
                    if (now - lastCheck > 24 * 60 * 60 * 1000L) {
                        settings.setLastUpdateCheckMs(now)
                        updateRepo.checkForUpdate()
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                ) { padding ->
                    SettingsScreen(
                        shizukuConnected = shizukuConnected,
                        shizukuPermGranted = shizukuPermGranted,
                        notificationAccessGranted = notifAccessGranted,
                        overlayGranted = overlayGranted,
                        writeSettingsGranted = writeSettingsGranted,
                        accessibilityGranted = accessibilityGranted,
                        shadeActive = isActive,
                        blockSystemShade = blockSystemShade,
                        selectedTheme = theme,
                        selectedAccentColor = accentColor,
                        darkThemeMode = darkThemeMode,
                        appVersion = BuildConfig.VERSION_NAME,
                        onToggleShade = { enabled ->
                            if (enabled) superHaptics.tileToggleOn() else superHaptics.tileToggleOff()
                            toggleShadeService(enabled)
                            scope.launch {
                                settings.setActive(enabled)
                                if (!enabled) {
                                    governor.enableExpansion()
                                } else if (blockSystemShade) {
                                    governor.disableExpansion()
                                }
                                SuperShadeTileService.requestUpdate(this@MainActivity)
                            }
                        },
                        onBlockSystemShadeChange = { block ->
                            if (block) superHaptics.tileToggleOn() else superHaptics.tileToggleOff()
                            scope.launch {
                                settings.setBlockSystemShade(block)
                                if (isActive) {
                                    if (block) governor.disableExpansion() else governor.enableExpansion()
                                }
                            }
                        },
                        onThemeChange = { newTheme ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setTheme(newTheme) }
                        },
                        onAccentColorChange = { newAccent ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setAccentColor(newAccent) }
                        },
                        onDarkModeChange = { newMode ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setDarkThemeMode(newMode) }
                        },
                        onGrantOverlay = {
                            superHaptics.lightTap()
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                )
                            )
                        },
                        onGrantWriteSettings = {
                            superHaptics.lightTap()
                            try {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                        Uri.parse("package:$packageName"),
                                    )
                                )
                            } catch (_: Exception) {}
                        },
                        onGrantAccessibility = {
                            superHaptics.lightTap()
                            try {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (_: Exception) {}
                        },
                        onCheckUpdate = {
                            superHaptics.lightTap()
                            scope.launch {
                                when (val result = updateRepo.checkForUpdate()) {
                                    is com.supershade.domain.update.UpdateCheckResult.UpToDate -> {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "SuperShade is up to date (v${result.currentVersion})",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    is com.supershade.domain.update.UpdateCheckResult.Error -> {
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "Update check: ${result.message}",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    is com.supershade.domain.update.UpdateCheckResult.UpdateAvailable -> {
                                        // UpdateDialog will appear automatically
                                    }
                                }
                            }
                        },
                        isCheckingUpdate = isCheckingUpdate,
                        onShowWhatsNew = {
                            superHaptics.lightTap()
                            updateRepo.showWhatsNewManual()
                        },
                        onPreviewShade = {
                            superHaptics.sheetDetent()
                            toggleShadeService(true)
                            shadeViewModel.open()
                            shadeWindowManager.show()
                        },
                        qsTileTapAction = qsTileTapAction,
                        onQsTileTapActionChange = { action ->
                            superHaptics.sliderTick()
                            scope.launch {
                                settings.setQsTileTapAction(action)
                                SuperShadeTileService.requestUpdate(this@MainActivity)
                            }
                        },
                        onOpenTilePreferences = {
                            superHaptics.sheetDetent()
                            startActivity(Intent(this@MainActivity, TilePreferencesActivity::class.java))
                        },
                        modifier = Modifier.padding(padding),
                    )
                }

                // Update available dialog
                availableUpdate?.let { update ->
                    UpdateDialog(
                        update = update,
                        onDismiss = { updateRepo.dismissUpdate() },
                    )
                }

                // What's New sheet — auto after upgrade, manual via "What's new" button
                if (showWhatsNew) {
                    WhatsNewSheet(
                        releaseNotes = availableUpdate?.releaseNotes ?: "",
                        onDismiss = { updateRepo.dismissWhatsNew() },
                    )
                }
            }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val am = getSystemService(android.view.accessibility.AccessibilityManager::class.java) ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val expected = ComponentName(this, com.supershade.service.SuperShadeAccessibilityService::class.java)
            enabledServices.any {
                it.resolveInfo.serviceInfo.packageName == expected.packageName &&
                it.resolveInfo.serviceInfo.name == expected.className
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val component = ComponentName(this, NotificationCollector::class.java)
        return flat.split(":").any {
            runCatching { ComponentName.unflattenFromString(it) == component }.getOrDefault(false)
        }
    }

    private fun toggleShadeService(enable: Boolean) {
        val intent = Intent(this, ShadeService::class.java)
        try {
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } else {
                stopService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "toggleShadeService failed: enable=$enable", e)
        }
    }
}
