package com.supershade.ui.settings

import android.content.Intent
import android.provider.Settings
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
import com.supershade.ui.theme.ShadeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    shizukuConnected: Boolean,
    notificationAccessGranted: Boolean,
    shadeActive: Boolean,
    selectedTheme: ShadeTheme,
    appVersion: String,
    onToggleShade: (Boolean) -> Unit,
    onThemeChange: (ShadeTheme) -> Unit,
    onCheckUpdate: () -> Unit,
    onShowWhatsNew: () -> Unit,
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
        StatusCard(
            icon = Icons.Default.Smartphone,
            label = "Shizuku",
            ok = shizukuConnected,
            okText = "Connected",
            failText = "Not connected — open Shizuku",
            action = if (!shizukuConnected) {
                { context.startActivity(
                    context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        ?: Intent(Settings.ACTION_SETTINGS)
                ) }
            } else null,
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

        HorizontalDivider()

        // ---- Shade section ------------------------------------------------
        SectionLabel("Shade")
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
                        enabled = shizukuConnected && notificationAccessGranted,
                    )
                }
            }
        }

        // ---- Theme section ------------------------------------------------
        SectionLabel("Theme")
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 12.dp),
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
) {
    Card(
        onClick = { action?.invoke() },
        enabled = action != null,
        colors = CardDefaults.cardColors(
            containerColor = if (ok)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
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
                tint = if (ok)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (ok) okText else failText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ok)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                )
            }
            Icon(
                imageVector = if (ok) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (ok)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
