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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import com.supershade.domain.tile.DEFAULT_TILES
import com.supershade.domain.tile.KNOWN_TILES
import com.supershade.settings.TileGridColumns
import com.supershade.settings.TileShape
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
    tileShape: TileShape = TileShape.SQUIRCLE,
    onTileShapeChange: (TileShape) -> Unit = {},
    tileColumns: TileGridColumns = TileGridColumns.STANDARD,
    onTileColumnsChange: (TileGridColumns) -> Unit = {},
    showWideCards: Boolean = true,
    onShowWideCardsChange: (Boolean) -> Unit = {},
    enabledTiles: List<String> = emptyList(),
    onEnabledTilesChange: (List<String>) -> Unit = {},
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
                    // Left 70% segment: Notifications + Quick Settings
                    Box(
                        modifier = Modifier
                            .weight(0.70f)
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
                                text = "Notifications + QS (Left 70%)",
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

                    // Right 30% segment: Quick Settings
                    Box(
                        modifier = Modifier
                            .weight(0.30f)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "QS Only (Right 30%)",
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
        // 6. QUICK TILES & GRID CUSTOMIZATION
        // =======================================================================
        var showTileEditor by remember { mutableStateOf(false) }

        SectionHeader(
            title = "Quick Tiles & Grid",
            badge = "${tileShape.label} • ${tileColumns.count} cols",
        )

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
                // Tile Shape Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tile Shape & Rounding",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "Select your preferred quick tile contour and corner radius",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TileShape.entries.forEach { shape ->
                            val isSelected = tileShape == shape
                            val previewCorner = when (shape) {
                                TileShape.SQUIRCLE -> RoundedCornerShape(22.dp)
                                TileShape.ROUNDED -> RoundedCornerShape(16.dp)
                                TileShape.CIRCLE -> CircleShape
                                TileShape.PILL -> RoundedCornerShape(28.dp)
                                TileShape.SOFT -> RoundedCornerShape(12.dp)
                            }
                            Surface(
                                shape = previewCorner,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                                ),
                                modifier = Modifier
                                    .width(108.dp)
                                    .height(68.dp)
                                    .clickable { onTileShapeChange(shape) },
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary),
                                            )
                                        }
                                    }
                                    Text(
                                        text = shape.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Grid Columns Density
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tile Grid Density",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = when (tileColumns) {
                            TileGridColumns.COMFORTABLE -> "3 Columns: Extra-large cards for comfortable thumb reach & readability"
                            TileGridColumns.COMPACT -> "5 Columns: Dense layout showing maximum toggles per row"
                            else -> "4 Columns: Standard balanced One UI 8 grid density"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val colsList = listOf(
                        TileGridColumns.COMFORTABLE to "3 Large",
                        TileGridColumns.STANDARD to "4 Standard",
                        TileGridColumns.COMPACT to "5 Dense",
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        colsList.forEachIndexed { index, (colOption, label) ->
                            SegmentedButton(
                                selected = tileColumns == colOption,
                                onClick = { onTileColumnsChange(colOption) },
                                shape = SegmentedButtonDefaults.itemShape(index, colsList.size),
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

                // Dual Connectivity Pills Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prominent Connectivity Pills",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Display dedicated top Wi-Fi & Bluetooth island cards in expanded Quick Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = showWideCards,
                        onCheckedChange = onShowWideCardsChange,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Active Tiles Manager Button
                FilledTonalButton(
                    onClick = { showTileEditor = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.DashboardCustomize,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Customize & Reorder Active Tiles (${if (enabledTiles.isNotEmpty()) enabledTiles.size else DEFAULT_TILES.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }

        if (showTileEditor) {
            TileEditorSheet(
                currentTiles = if (enabledTiles.isNotEmpty()) enabledTiles else DEFAULT_TILES,
                onDismiss = { showTileEditor = false },
                onSave = { updated ->
                    onEnabledTilesChange(updated)
                    showTileEditor = false
                },
            )
        }

        // =======================================================================
        // 7. SYSTEM QUICK SETTINGS TILE
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TileEditorSheet(
    currentTiles: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTiles by remember(currentTiles) { mutableStateOf(currentTiles.toList()) }

    // All available tile IDs from KNOWN_TILES
    val allTileIds = remember {
        val list = mutableListOf<String>()
        DEFAULT_TILES.forEach { if (!list.contains(it)) list.add(it) }
        KNOWN_TILES.keys.forEach { id ->
            val cleanId = id.lowercase()
            if (!list.contains(id) && !list.contains(cleanId)) list.add(id)
        }
        list.distinct()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Customize Quick Tiles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "${selectedTiles.size} active tiles in Quick Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { selectedTiles = DEFAULT_TILES.toList() },
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Scrollable list of tiles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                allTileIds.forEach { tileId ->
                    val isEnabled = selectedTiles.contains(tileId)
                    val label = KNOWN_TILES[tileId]?.first ?: tileId.replaceFirstChar { it.uppercase() }
                    val currentIndex = selectedTiles.indexOf(tileId)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isEnabled) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Checkbox(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        selectedTiles = if (checked) {
                                            selectedTiles + tileId
                                        } else {
                                            selectedTiles - tileId
                                        }
                                    },
                                )
                                Column {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isEnabled) {
                                        Text(
                                            text = "Position #${currentIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }

                            if (isEnabled) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (currentIndex > 0) {
                                                val mutable = selectedTiles.toMutableList()
                                                val item = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex - 1, item)
                                                selectedTiles = mutable
                                            }
                                        },
                                        enabled = currentIndex > 0,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move up",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (currentIndex < selectedTiles.size - 1) {
                                                val mutable = selectedTiles.toMutableList()
                                                val item = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex + 1, item)
                                                selectedTiles = mutable
                                            }
                                        },
                                        enabled = currentIndex < selectedTiles.size - 1,
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move down",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onSave(selectedTiles) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Text("Save Tiles")
                }
            }
        }
    }
}
