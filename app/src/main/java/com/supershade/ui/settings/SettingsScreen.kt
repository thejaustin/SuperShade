package com.supershade.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.supershade.ui.theme.DarkThemeMode
import com.supershade.ui.theme.ShadeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    shizukuConnected: Boolean,
    shizukuPermGranted: Boolean,
    notificationAccessGranted: Boolean,
    overlayGranted: Boolean,
    shadeActive: Boolean,
    blockSystemShade: Boolean = true,
    selectedTheme: ShadeTheme,
    appVersion: String,
    darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    onToggleShade: (Boolean) -> Unit,
    onBlockSystemShadeChange: (Boolean) -> Unit = {},
    onThemeChange: (ShadeTheme) -> Unit,
    onDarkModeChange: (DarkThemeMode) -> Unit = {},
    onGrantOverlay: () -> Unit,
    onCheckUpdate: () -> Unit,
    onShowWhatsNew: () -> Unit,
    onPreviewShade: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // ---- Status section -----------------------------------------------
        SectionLabel("Status")
        val shizukuOk = shizukuConnected && shizukuPermGranted
        StatusCard(
            icon = Icons.Default.Smartphone,
            label = "Shizuku (Optional)",
            ok = shizukuOk,
            okText = "Connected — QS tile toggling enabled",
            failText = when {
                shizukuConnected && !shizukuPermGranted -> "Connected — tap to grant permission"
                else -> "Not connected — tiles will open Settings instead"
            },
            action = when {
                shizukuConnected && !shizukuPermGranted -> null
                !shizukuConnected -> {
                    { context.startActivity(
                        context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                            ?: Intent(Settings.ACTION_SETTINGS)
                    ) }
                }
                else -> null
            },
            isOptional = true,
        )
        StatusCard(
            icon = Icons.Default.Notifications,
            label = "Notification Access",
            ok = notificationAccessGranted,
            okText = "Granted",
            failText = "Tap to grant",
            action = if (!notificationAccessGranted) {
                {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            } else null,
        )
        StatusCard(
            icon = Icons.Default.Layers,
            label = "Display Over Other Apps",
            ok = overlayGranted,
            okText = "Granted",
            failText = "Tap to grant",
            action = if (!overlayGranted) onGrantOverlay else null,
        )

        HorizontalDivider()

        // ---- Shade section ------------------------------------------------
        SectionLabel("Shade")
        val allGranted = notificationAccessGranted && overlayGranted
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable SuperShade", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (shadeActive) "Swipe down to open" else "System shade active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = shadeActive,
                        onCheckedChange = onToggleShade,
                        enabled = allGranted,
                    )
                }
                AnimatedVisibility(visible = !allGranted) {
                    val missing = buildList {
                        if (!notificationAccessGranted) add("Notification access")
                        if (!overlayGranted) add("Display over other apps")
                    }
                    Text(
                        text = "Requires: ${missing.joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                AnimatedVisibility(visible = shadeActive && allGranted) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Block system shade", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = when {
                                        !shizukuOk -> "Requires Shizuku"
                                        blockSystemShade -> "System panel disabled"
                                        else -> "System panel allowed"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = blockSystemShade,
                                onCheckedChange = onBlockSystemShadeChange,
                                enabled = shizukuOk,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = onPreviewShade,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open Shade Preview")
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ---- Theme section ------------------------------------------------
        SectionLabel("Theme")
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Shade Style",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                val themes = listOf(ShadeTheme.OneUI, ShadeTheme.Pixel)
                val labels = listOf("One UI", "Pixel")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themes.forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = selectedTheme == theme,
                            onClick = { onThemeChange(theme) },
                            shape = SegmentedButtonDefaults.itemShape(index, themes.size),
                            icon = {},
                        ) {
                            Text(labels[index])
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "App Theme Mode",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                val darkModes = listOf(
                    DarkThemeMode.SYSTEM to "System",
                    DarkThemeMode.DARK to "Dark",
                    DarkThemeMode.LIGHT to "Light",
                    DarkThemeMode.AMOLED to "AMOLED",
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    darkModes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = darkThemeMode == mode,
                            onClick = { onDarkModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, darkModes.size),
                            icon = {},
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ---- About section ------------------------------------------------
        SectionLabel("About")
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("SuperShade", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Version $appVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onShowWhatsNew) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("What's new")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCheckUpdate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Check for updates")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    label: String,
    ok: Boolean,
    okText: String,
    failText: String,
    action: (() -> Unit)?,
    isOptional: Boolean = false,
) {
    val isWarning = !ok && isOptional
    Card(
        onClick = { action?.invoke() },
        enabled = action != null,
        colors = CardDefaults.cardColors(
            containerColor = when {
                ok -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                isWarning -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    ok -> MaterialTheme.colorScheme.primary
                    isWarning -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (ok) okText else failText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        ok -> MaterialTheme.colorScheme.primary
                        isWarning -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.error
                    },
                )
            }
            Icon(
                imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = when {
                    ok -> MaterialTheme.colorScheme.primary
                    isWarning -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
