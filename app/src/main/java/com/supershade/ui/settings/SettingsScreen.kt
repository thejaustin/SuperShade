package com.supershade.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.R
import com.supershade.settings.AccentColor
import com.supershade.settings.QsTileTapAction
import com.supershade.ui.theme.DarkThemeMode
import com.supershade.ui.theme.ShadeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    shizukuConnected: Boolean,
    shizukuPermGranted: Boolean,
    notificationAccessGranted: Boolean,
    overlayGranted: Boolean,
    writeSettingsGranted: Boolean = false,
    accessibilityGranted: Boolean = false,
    shadeActive: Boolean,
    blockSystemShade: Boolean = true,
    selectedTheme: ShadeTheme,
    selectedAccentColor: AccentColor = AccentColor.GALAXY_BLUE,
    appVersion: String,
    darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    onToggleShade: (Boolean) -> Unit,
    onBlockSystemShadeChange: (Boolean) -> Unit = {},
    onThemeChange: (ShadeTheme) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit = {},
    onDarkModeChange: (DarkThemeMode) -> Unit = {},
    onGrantOverlay: () -> Unit,
    onGrantWriteSettings: () -> Unit = {},
    onGrantAccessibility: () -> Unit = {},
    onCheckUpdate: () -> Unit,
    isCheckingUpdate: Boolean = false,
    onShowWhatsNew: () -> Unit,
    onPreviewShade: () -> Unit = {},
    qsTileTapAction: QsTileTapAction = QsTileTapAction.TOGGLE_ACTIVE,
    onQsTileTapActionChange: (QsTileTapAction) -> Unit = {},
    onOpenTilePreferences: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allEssentialGranted = notificationAccessGranted && overlayGranted
    val shizukuOk = shizukuConnected && shizukuPermGranted

    val activeServiceCount = listOf(
        notificationAccessGranted,
        overlayGranted,
        accessibilityGranted,
        writeSettingsGranted,
        shizukuOk,
    ).count { it }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // =======================================================================
        // 1. HERO HEADER CARD
        // =======================================================================
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification_shade),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "SuperShade",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = "v$appVersion",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Intelligent System Shade & Control Center",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Live Active Badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        !allEssentialGranted -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        shadeActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            !allEssentialGranted -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            shadeActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !allEssentialGranted -> MaterialTheme.colorScheme.error
                                        shadeActive -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                        )
                        Text(
                            text = when {
                                !allEssentialGranted -> "Setup required — permissions needed to activate"
                                shadeActive -> "Active • Swipe down from status bar to open"
                                else -> "Disabled • Native system shade currently active"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = when {
                                !allEssentialGranted -> MaterialTheme.colorScheme.error
                                shadeActive -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Master Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable SuperShade",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = if (shadeActive) "Running foreground service & gesture interceptor" else "Turn on to replace system shade",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = shadeActive && allEssentialGranted,
                        onCheckedChange = onToggleShade,
                        enabled = allEssentialGranted,
                    )
                }
            }
        }

        // =======================================================================
        // 2. QUICK ACTIONS HUB
        // =======================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onPreviewShade,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Open Shade",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            FilledTonalButton(
                onClick = onOpenTilePreferences,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Customize Tiles",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }

        // =======================================================================
        // 3. SYSTEM INTEGRATION & PERMISSIONS HUB
        // =======================================================================
        SectionHeader(
            title = "System Integration",
            badge = "$activeServiceCount of 5 ready",
            badgeColor = if (allEssentialGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Notification Access (Required)
                PermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Access",
                    subtitle = "Reads & categorizes notifications with bidirectional swipe dismiss",
                    isGranted = notificationAccessGranted,
                    isRequired = true,
                    actionText = "Grant",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // 2. Display Over Other Apps (Required)
                PermissionRow(
                    icon = Icons.Default.Layers,
                    title = "Display Over Other Apps",
                    subtitle = "Enables drawing the full-screen shade and Quick Settings window",
                    isGranted = overlayGranted,
                    isRequired = true,
                    actionText = "Grant",
                    onClick = onGrantOverlay,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // 3. Accessibility Service (Zero-ADB - Recommended)
                PermissionRow(
                    icon = Icons.Default.TouchApp,
                    title = "Accessibility Service",
                    subtitle = "Zero-ADB pull interception — catches status bar pulls seamlessly",
                    isGranted = accessibilityGranted,
                    isRequired = false,
                    isRecommended = true,
                    actionText = "Enable",
                    onClick = onGrantAccessibility,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // 4. Modify System Settings (Recommended)
                PermissionRow(
                    icon = Icons.Default.BrightnessMedium,
                    title = "Modify System Settings",
                    subtitle = "Direct screen brightness and auto-rotation control without Shizuku",
                    isGranted = writeSettingsGranted,
                    isRequired = false,
                    isRecommended = true,
                    actionText = "Grant",
                    onClick = onGrantWriteSettings,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // 5. Shizuku Privileged API (Advanced)
                PermissionRow(
                    icon = Icons.Default.Smartphone,
                    title = "Shizuku Privileged API",
                    subtitle = when {
                        shizukuOk -> "Connected — privileged hardware controls & system panel suppression"
                        shizukuConnected && !shizukuPermGranted -> "Connected — tap to authorize permission"
                        else -> "Optional — run wireless ADB or Shizuku for rootless toggles"
                    },
                    isGranted = shizukuOk,
                    isRequired = false,
                    actionText = when {
                        shizukuConnected && !shizukuPermGranted -> "Authorize"
                        !shizukuConnected -> "Open"
                        else -> "Active"
                    },
                    onClick = when {
                        shizukuConnected && !shizukuPermGranted -> null
                        !shizukuConnected -> {
                            {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                context.startActivity(launchIntent ?: Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                        else -> null
                    },
                )
            }
        }

        // =======================================================================
        // 4. GESTURES & CONTROLS
        // =======================================================================
        SectionHeader(title = "Gestures & Controls")

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Split status bar visual guide
                Text(
                    text = "Status Bar Pull Split",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    // Left 72% segment: Notifications + Quick Settings
                    Box(
                        modifier = Modifier
                            .weight(0.72f)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwipeDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Notifications + QS (Left 72%)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    )

                    // Right 28% segment: Quick Settings
                    Box(
                        modifier = Modifier
                            .weight(0.28f)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "QS Only (Right)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                Text(
                    text = "Swipe down from the left or center of your status bar to open the standard shade with notifications. Swipe down from the top right edge to directly expand full Quick Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Haptic feedback info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tactile Haptic Feedback",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Mechanical clock tick feedback on crossing pull threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Block system shade toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Block native system shade",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = when {
                                !shizukuOk -> "Requires Shizuku connection to disable system panel"
                                blockSystemShade -> "Native panel blocked — SuperShade handles all pulls"
                                else -> "Native system panel allowed to co-exist"
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
            }
        }

        // =======================================================================
        // 5. APPEARANCE & THEMING
        // =======================================================================
        SectionHeader(title = "Appearance")

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Shade Style
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Shade Style",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    val themes = listOf(ShadeTheme.OneUI, ShadeTheme.Pixel, ShadeTheme.PureMaterial)
                    val labels = listOf("One UI 8", "Pixel", "Pure Material")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themes.forEachIndexed { index, theme ->
                            SegmentedButton(
                                selected = selectedTheme == theme,
                                onClick = { onThemeChange(theme) },
                                shape = SegmentedButtonDefaults.itemShape(index, themes.size),
                                icon = {},
                            ) {
                                Text(
                                    text = labels[index],
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Dark Theme Mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Accent Color Palette",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccentColor.entries.forEach { accent ->
                            ColorPaletteSwatch(
                                accent = accent,
                                isSelected = selectedAccentColor == accent,
                                onClick = { onAccentColorChange(accent) },
                            )
                        }
                    }
                }
            }
        }

        // =======================================================================
        // 6. SYSTEM QUICK SETTINGS TILE
        // =======================================================================
        SectionHeader(title = "System Quick Settings Tile")

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Settings System Tile",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Add a SuperShade tile to your device's native Quick Settings panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification_shade),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val sbm = context.getSystemService(android.app.StatusBarManager::class.java)
                                val component = android.content.ComponentName(
                                    context,
                                    com.supershade.service.SuperShadeTileService::class.java,
                                )
                                sbm?.requestAddTileService(
                                    component,
                                    "SuperShade",
                                    android.graphics.drawable.Icon.createWithResource(
                                        context,
                                        R.drawable.ic_notification_shade,
                                    ),
                                    context.mainExecutor,
                                ) { _ -> }
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Tile to Device Quick Settings")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Single-Tap Tile Action",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    val tileActions = listOf(
                        QsTileTapAction.TOGGLE_ACTIVE to "Toggle Active",
                        QsTileTapAction.OPEN_SHADE to "Open Shade",
                        QsTileTapAction.SHOW_MENU to "Show Quick Menu",
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        tileActions.forEachIndexed { index, (action, label) ->
                            SegmentedButton(
                                selected = qsTileTapAction == action,
                                onClick = { onQsTileTapActionChange(action) },
                                shape = SegmentedButtonDefaults.itemShape(index, tileActions.size),
                                icon = {},
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "💡 Tip: Long-press the SuperShade system tile anytime to instantly display the tile quick control menu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // =======================================================================
        // 7. ABOUT & UPDATES
        // =======================================================================
        SectionHeader(title = "About")

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "SuperShade",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Version $appVersion • Modern One UI 8 / Android 16",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onShowWhatsNew,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("What's new", style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedButton(
                    onClick = onCheckUpdate,
                    enabled = !isCheckingUpdate,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Checking for updates...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Check for updates")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ===========================================================================
// HELPER COMPOSABLES
// ===========================================================================

@Composable
private fun SectionHeader(
    title: String,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = badgeColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    isRequired: Boolean = false,
    isRecommended: Boolean = false,
    actionText: String = "Grant",
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                if (isRequired && !isGranted) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                } else if (isRecommended && !isGranted) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Granted",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        } else if (onClick != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isRequired) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ColorPaletteSwatch(
    accent: AccentColor,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val displayColor = if (accent == AccentColor.MONET) {
        MaterialTheme.colorScheme.primary
    } else {
        androidx.compose.ui.graphics.Color(accent.hex)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.5.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                    } else Modifier
                )
                .padding(3.5.dp)
                .clip(CircleShape)
                .background(displayColor),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(18.dp),
                )
            } else if (accent == AccentColor.MONET) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (accent) {
                AccentColor.GALAXY_BLUE -> "Galaxy"
                AccentColor.EMERALD -> "Emerald"
                AccentColor.VIOLET -> "Violet"
                AccentColor.AMBER -> "Amber"
                AccentColor.CORAL -> "Coral"
                AccentColor.MONET -> "Monet"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
