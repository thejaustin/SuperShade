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
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.ui.settings.SettingsScreen
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.SuperShadeAppTheme
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
            val monetAccentStrength by settings.monetAccentStrength.collectAsState(initial = 1.0f)
            val backdropTheme by settings.backdropTheme.collectAsState(initial = BackdropTheme.FROSTED_GLASS)
            val backdropOpacity by settings.backdropOpacity.collectAsState(initial = 0.78f)
            val notificationDensity by settings.notificationDensity.collectAsState(initial = com.supershade.settings.NotificationDensity.BALANCED)
            val isActive by settings.isActive.collectAsState(initial = false)
            val blockSystemShade by settings.blockSystemShade.collectAsState(initial = true)
            val tileShape by settings.tileShape.collectAsState(initial = com.supershade.settings.TileShape.SQUIRCLE)
            val tileSize by settings.tileSize.collectAsState(initial = com.supershade.settings.TileSize.STANDARD)
            val tileColumns by settings.tileColumns.collectAsState(initial = com.supershade.settings.TileGridColumns.STANDARD)
            val showWideCards by settings.showWideCards.collectAsState(initial = true)
            val splitGestureMode by settings.splitGestureMode.collectAsState(initial = com.supershade.settings.SplitGestureMode.SEPARATE_70_30)
            val showPanelSwitcherPill by settings.showPanelSwitcherPill.collectAsState(initial = false)
            val cardBorderWidth by settings.cardBorderWidth.collectAsState(initial = com.supershade.settings.CardBorderWidth.THIN)
            val hiddenChannels by settings.hiddenChannels.collectAsState(initial = emptySet())
            val hideOngoingNotifications by settings.hideOngoingNotifications.collectAsState(initial = false)
            val ambientMediaWidgetEnabled by settings.ambientMediaWidgetEnabled.collectAsState(initial = true)
            val deviceControlMode by settings.deviceControlMode.collectAsState(initial = com.supershade.settings.DeviceControlMode.SHOW_WHEN_EXPANDED)
            val availableUpdate by updateRepo.availableUpdate.collectAsState()
            val isCheckingUpdate by updateRepo.isChecking.collectAsState()
            val showWhatsNew by updateRepo.showWhatsNew.collectAsState()
            val previousVersion by updateRepo.previousVersion.collectAsState()

            SuperShadeAppTheme(mode = darkThemeMode, accentColor = accentColor) {
                // Re-checked on every resume and reactively via Shizuku listener so user sees instant feedback
                val shizukuPermGrantedFlow by connector.hasPermissionFlow.collectAsState()
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
                            connector.updatePermissionState()
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
                        shizukuPermGranted = shizukuPermGranted || shizukuPermGrantedFlow,
                        notificationAccessGranted = notifAccessGranted,
                        overlayGranted = overlayGranted,
                        writeSettingsGranted = writeSettingsGranted,
                        accessibilityGranted = accessibilityGranted,
                        shadeActive = isActive,
                        blockSystemShade = blockSystemShade,
                        selectedTheme = theme,
                        selectedAccentColor = accentColor,
                        backdropTheme = backdropTheme,
                        backdropOpacity = backdropOpacity,
                        notificationDensity = notificationDensity,
                        onNotificationDensityChange = { newDensity ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setNotificationDensity(newDensity) }
                        },
                        hiddenChannels = hiddenChannels,
                        onUnhideChannel = { channelKey ->
                            superHaptics.lightTap()
                            scope.launch { settings.unhideChannel(channelKey) }
                        },
                        hideOngoingNotifications = hideOngoingNotifications,
                        onHideOngoingNotificationsChange = { hide ->
                            if (hide) superHaptics.tileToggleOn() else superHaptics.tileToggleOff()
                            scope.launch { settings.setHideOngoingNotifications(hide) }
                        },
                        ambientMediaWidgetEnabled = ambientMediaWidgetEnabled,
                        onAmbientMediaWidgetEnabledChange = { enabled ->
                            if (enabled) superHaptics.tileToggleOn() else superHaptics.tileToggleOff()
                            scope.launch { settings.setAmbientMediaWidgetEnabled(enabled) }
                        },
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
                        monetAccentStrength = monetAccentStrength,
                        onMonetAccentStrengthChange = { newStrength ->
                            scope.launch { settings.setMonetAccentStrength(newStrength) }
                        },
                        onDarkModeChange = { newMode ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setDarkThemeMode(newMode) }
                        },
                        onBackdropThemeChange = { newBackdrop ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setBackdropTheme(newBackdrop) }
                        },
                        onBackdropOpacityChange = { newOpacity ->
                            scope.launch { settings.setBackdropOpacity(newOpacity) }
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
                            } catch (e: Exception) {}
                        },
                        onGrantAccessibility = {
                            superHaptics.lightTap()
                            try {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (e: Exception) {}
                        },
                        onRequestShizukuPermission = {
                            superHaptics.lightTap()
                            connector.requestPermission()
                        },
                        onOpenShizukuManager = {
                            superHaptics.lightTap()
                            try {
                                val launchIntent = connector.getManagerLaunchIntent()
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                } else {
                                    val explicitIntent = Intent(Intent.ACTION_MAIN)
                                        .setComponent(android.content.ComponentName("af.shizuku.plus.api", "af.shizuku.manager.LauncherAlias"))
                                        .addCategory(Intent.CATEGORY_LAUNCHER)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(explicitIntent)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to launch Shizuku manager app", e)
                                try {
                                    startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {}
                            }
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
                        tileShape = tileShape,
                        onTileShapeChange = { shape ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setTileShape(shape) }
                        },
                        tileSize = tileSize,
                        onTileSizeChange = { size ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setTileSize(size) }
                        },
                        tileColumns = tileColumns,
                        onTileColumnsChange = { cols ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setTileColumns(cols) }
                        },
                        showWideCards = showWideCards,
                        onShowWideCardsChange = { show ->
                            if (show) superHaptics.tileToggleOn() else superHaptics.tileToggleOff()
                            scope.launch { settings.setShowWideCards(show) }
                        },
                        splitGestureMode = splitGestureMode,
                        onSplitGestureModeChange = { mode ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setSplitGestureMode(mode) }
                        },
                        showPanelSwitcherPill = showPanelSwitcherPill,
                        onShowPanelSwitcherPillChange = { show ->
                            if (show) superHaptics.tileToggleOn() else superHaptics.tileToggleOff()
                            scope.launch { settings.setShowPanelSwitcherPill(show) }
                        },
                        cardBorderWidth = cardBorderWidth,
                        onCardBorderWidthChange = { width ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setCardBorderWidth(width) }
                        },
                        deviceControlMode = deviceControlMode,
                        onDeviceControlModeChange = { mode ->
                            superHaptics.sliderTick()
                            scope.launch { settings.setDeviceControlMode(mode) }
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
                        previousVersion = previousVersion,
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
