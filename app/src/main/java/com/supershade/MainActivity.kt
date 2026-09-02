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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.supershade.domain.update.UpdateRepository
import com.supershade.service.NotificationCollector
import com.supershade.service.ShadeService
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.ui.settings.SettingsScreen
import com.supershade.ui.theme.ShadeTheme
import com.supershade.ui.theme.SuperShadeAppTheme
import com.supershade.ui.update.UpdateDialog
import com.supershade.ui.update.WhatsNewSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val connector: ShizukuPlusConnector by inject()
    private val settings: ShadeSettings by inject()
    private val updateRepo: UpdateRepository by inject()
    private val shadeViewModel: ShadeViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SuperShadeAppTheme {
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                val shizukuConnected by connector.isConnected.collectAsState()
                val theme by settings.theme.collectAsState(initial = ShadeTheme.OneUI)
                val isActive by settings.isActive.collectAsState(initial = false)
                val availableUpdate by updateRepo.availableUpdate.collectAsState()
                val showWhatsNew by updateRepo.showWhatsNew.collectAsState()

                // Re-checked on every resume so user sees instant feedback after
                // granting access in system Settings.
                var notifAccessGranted by remember { mutableStateOf(isNotificationAccessGranted()) }
                var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(this)) }
                var shizukuPermGranted by remember { mutableStateOf(connector.hasPermission()) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            notifAccessGranted = isNotificationAccessGranted()
                            overlayGranted = Settings.canDrawOverlays(this@MainActivity)
                            shizukuPermGranted = connector.hasPermission()
                            if (isActive && notifAccessGranted && overlayGranted && shizukuPermGranted) {
                                toggleShadeService(true)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Ensure service is running if active in settings and permissions are ready
                LaunchedEffect(isActive, notifAccessGranted, overlayGranted, shizukuPermGranted) {
                    if (isActive && notifAccessGranted && overlayGranted && shizukuPermGranted) {
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
                    topBar = { TopAppBar(title = { Text("SuperShade") }) },
                ) { padding ->
                    SettingsScreen(
                        shizukuConnected = shizukuConnected,
                        shizukuPermGranted = shizukuPermGranted,
                        notificationAccessGranted = notifAccessGranted,
                        overlayGranted = overlayGranted,
                        shadeActive = isActive,
                        selectedTheme = theme,
                        appVersion = BuildConfig.VERSION_NAME,
                        onToggleShade = { enabled ->
                            toggleShadeService(enabled)
                            scope.launch { settings.setActive(enabled) }
                        },
                        onThemeChange = { newTheme ->
                            scope.launch { settings.setTheme(newTheme) }
                        },
                        onGrantOverlay = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                )
                            )
                        },
                        onCheckUpdate = {
                            scope.launch { updateRepo.checkForUpdate() }
                        },
                        onShowWhatsNew = { updateRepo.showWhatsNewManual() },
                        onPreviewShade = {
                            toggleShadeService(true)
                            shadeViewModel.open()
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
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }
}
