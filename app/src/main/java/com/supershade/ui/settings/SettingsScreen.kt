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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.font.FontFamily
import com.supershade.ui.theme.ChamferedCornerShape
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import com.supershade.settings.CardBorderWidth
import com.supershade.settings.DeviceControlMode
import com.supershade.settings.SplitGestureMode
import com.supershade.settings.TileGridColumns
import com.supershade.settings.TileShape
import com.supershade.settings.TileSize
import com.supershade.ui.theme.LocalBackdropTheme
import com.supershade.ui.theme.LocalCardBorderWidth
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.ShadeShapeScheme
import com.supershade.ui.theme.getCardBorder
import com.supershade.ui.theme.toComposeShape
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.R
import com.supershade.settings.NotificationDensity
import com.supershade.settings.AccentColor
import com.supershade.ui.theme.BackdropTheme
import com.supershade.ui.theme.DarkThemeMode
import com.supershade.ui.theme.ShadeTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    backdropTheme: BackdropTheme = BackdropTheme.FROSTED_GLASS,
    backdropOpacity: Float = 0.78f,
    notificationDensity: NotificationDensity = NotificationDensity.BALANCED,
    onNotificationDensityChange: (NotificationDensity) -> Unit = {},
    hiddenChannels: Set<String> = emptySet(),
    onUnhideChannel: (String) -> Unit = {},
    hideOngoingNotifications: Boolean = false,
    onHideOngoingNotificationsChange: (Boolean) -> Unit = {},
    appVersion: String,
    darkThemeMode: DarkThemeMode = DarkThemeMode.SYSTEM,
    onToggleShade: (Boolean) -> Unit,
    onBlockSystemShadeChange: (Boolean) -> Unit = {},
    onThemeChange: (ShadeTheme) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit = {},
    onDarkModeChange: (DarkThemeMode) -> Unit = {},
    onBackdropThemeChange: (BackdropTheme) -> Unit = {},
    onBackdropOpacityChange: (Float) -> Unit = {},
    onGrantOverlay: () -> Unit,
    onGrantWriteSettings: () -> Unit = {},
    onGrantAccessibility: () -> Unit = {},
    onRequestShizukuPermission: () -> Unit = {},
    onOpenShizukuManager: () -> Unit = {},
    onCheckUpdate: () -> Unit,
    isCheckingUpdate: Boolean = false,
    onShowWhatsNew: () -> Unit,
    onPreviewShade: () -> Unit = {},
    tileShape: TileShape = TileShape.SQUIRCLE,
    onTileShapeChange: (TileShape) -> Unit = {},
    tileSize: TileSize = TileSize.STANDARD,
    onTileSizeChange: (TileSize) -> Unit = {},
    tileColumns: TileGridColumns = TileGridColumns.STANDARD,
    onTileColumnsChange: (TileGridColumns) -> Unit = {},
    showWideCards: Boolean = true,
    onShowWideCardsChange: (Boolean) -> Unit = {},
    splitGestureMode: SplitGestureMode = SplitGestureMode.SEPARATE_70_30,
    onSplitGestureModeChange: (SplitGestureMode) -> Unit = {},
    showPanelSwitcherPill: Boolean = false,
    onShowPanelSwitcherPillChange: (Boolean) -> Unit = {},
    cardBorderWidth: CardBorderWidth = CardBorderWidth.THIN,
    onCardBorderWidthChange: (CardBorderWidth) -> Unit = {},
    deviceControlMode: DeviceControlMode = DeviceControlMode.SHOW_WHEN_EXPANDED,
    onDeviceControlModeChange: (DeviceControlMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = com.supershade.haptics.LocalSuperHaptics.current
    val allEssentialGranted = notificationAccessGranted && overlayGranted
    val shizukuOk = shizukuConnected && shizukuPermGranted

    val activeServiceCount = listOf(
        notificationAccessGranted,
        overlayGranted,
        accessibilityGranted,
        writeSettingsGranted,
        shizukuOk,
    ).count { it }

    val shapeScheme = remember(tileShape) { ShadeShapeScheme.fromTileShape(tileShape) }

    CompositionLocalProvider(
        LocalBackdropTheme provides backdropTheme,
        LocalCardBorderWidth provides cardBorderWidth,
        LocalShadeShapeScheme provides shapeScheme,
    ) {
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
        Button(
            onClick = onPreviewShade,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
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

        // =======================================================================
        // 3. SYSTEM INTEGRATION & PERMISSIONS HUB
        // =======================================================================
        var permissionsExpanded by remember { mutableStateOf(activeServiceCount < 5) }

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
                if (activeServiceCount == 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { permissionsExpanded = !permissionsExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Column {
                                Text(
                                    text = "All System Permissions Active",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Notifications, Overlay, Accessibility, Settings, & Shizuku ready",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { permissionsExpanded = !permissionsExpanded }) {
                            Icon(
                                imageVector = if (permissionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (permissionsExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (permissionsExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))
                    }
                }

                AnimatedVisibility(visible = permissionsExpanded) {
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
                                else -> "Tap to open Shizuku/ShizukuPlus to start service & set up rootless toggles"
                            },
                            isGranted = shizukuOk,
                            isRequired = false,
                            actionText = when {
                                shizukuConnected && !shizukuPermGranted -> "Authorize"
                                !shizukuConnected -> "Set up"
                                else -> "Active"
                            },
                            onClick = when {
                                shizukuConnected && !shizukuPermGranted -> onRequestShizukuPermission
                                !shizukuOk -> onOpenShizukuManager
                                else -> onOpenShizukuManager
                            },
                        )
                    }
                }
            }
        }

        // =======================================================================
        // 4. GESTURES & CONTROLS
        // =======================================================================
        SectionHeader(
            title = "Gestures & Controls",
            badge = splitGestureMode.label,
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
                // Primary Layout Mode Header & 2-Card Mode Selector (Together vs Separate)
                val isTogether = splitGestureMode.isTogether

                Text(
                    text = "Panel Layout Mode",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Card 1: Together (Combined)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isTogether) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isTogether) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics?.sheetDetent()
                                onSplitGestureModeChange(SplitGestureMode.TOGETHER)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = if (isTogether) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                                if (isTogether) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "Together",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = if (isTogether) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "One UI unified feed. Compact QS tiles on top, notifications below. Drag down expands QS.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Card 2: Separate (Split)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (!isTogether) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (!isTogether) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics?.sheetDetent()
                                if (isTogether) {
                                    onSplitGestureModeChange(SplitGestureMode.SEPARATE_70_30)
                                }
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DashboardCustomize,
                                    contentDescription = null,
                                    tint = if (!isTogether) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                                if (!isTogether) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "Separate",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = if (!isTogether) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Open panels independently from top right or left. Swipe horizontally between pages.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (isTogether) {
                    // Together Mode Gesture Guide
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwipeDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Together Gestures (Swipe Down Anywhere)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                text = "• Swiping down anywhere along status bar opens the combined panel.\n" +
                                    "• Pulling down on compact Quick Settings tiles expands full Quick Settings.\n" +
                                    "• Swiping up when QS is expanded smoothly collapses it back to compact.\n" +
                                    "• Swiping up on notifications or bottom handle dismisses the shade.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    // Split status bar visual guide
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Status Bar Pull Split",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = splitGestureMode.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    // Dynamic visual split diagram
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        when (splitGestureMode) {
                            SplitGestureMode.ALWAYS_NOTIFICATIONS -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
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
                                            text = "Entire Status Bar: Notifications & Full Shade",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            SplitGestureMode.ALWAYS_QUICK_SETTINGS -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.70f))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwipeDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "Entire Status Bar: Quick Settings Expanded",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            }
                            SplitGestureMode.SEPARATE_30_70 -> {
                                Box(
                                    modifier = Modifier
                                        .weight(0.30f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f))
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "QS (30%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.70f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = "Notifications (Right 70%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            SplitGestureMode.SEPARATE_50_50 -> {
                                Box(
                                    modifier = Modifier
                                        .weight(0.50f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Notifications (50%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.50f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Quick Settings (50%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                            SplitGestureMode.SEPARATE_70_30, SplitGestureMode.TOGETHER -> {
                                Box(
                                    modifier = Modifier
                                        .weight(0.70f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = "Notifications (Left 70%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.30f)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f))
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "QS (30%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = splitGestureMode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Selectable Split Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SplitGestureMode.entries.filter { !it.isTogether }.forEach { mode ->
                            val isSelected = splitGestureMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSplitGestureModeChange(mode) },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = mode.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "✓",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Bottom Panel Switcher Pill Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bottom Panel Switcher Pill",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Display accessible dock pill at the bottom to switch between Notifications and Quick Settings (swipe gestures are always active)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = showPanelSwitcherPill,
                        onCheckedChange = onShowPanelSwitcherPillChange,
                    )
                }

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
                // Live Interactive Appearance Studio Canvas
                val activeAccent = if (selectedAccentColor == AccentColor.MONET || selectedAccentColor.hex == 0L) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color(selectedAccentColor.hex)
                }
                val previewShapeScheme = remember(selectedTheme, tileShape) {
                    com.supershade.ui.theme.ShadeShapeScheme.forTheme(selectedTheme, tileShape)
                }
                val previewTileShape = previewShapeScheme.tile

                var previewWifiActive by remember { mutableStateOf(true) }
                var previewBtActive by remember { mutableStateOf(true) }
                var previewSoundActive by remember { mutableStateOf(false) }
                var previewPortraitActive by remember { mutableStateOf(false) }
                var previewBrightnessFraction by remember { mutableFloatStateOf(0.68f) }
                var previewUse24Hour by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        backdropOpacity >= 0.99f -> if (darkThemeMode == DarkThemeMode.AMOLED) Color(0xFF000000) else MaterialTheme.colorScheme.surface
                        darkThemeMode == DarkThemeMode.AMOLED -> Color(0xFF05070A).copy(alpha = backdropOpacity)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = backdropOpacity)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        brush = if (backdropTheme == BackdropTheme.LIQUID_GLASS) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.60f),
                                    activeAccent.copy(alpha = 0.40f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                ),
                                start = Offset.Zero,
                                end = Offset(300f, 300f),
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                                )
                            )
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (backdropTheme == BackdropTheme.LIQUID_GLASS) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.White.copy(alpha = 0.04f),
                                                Color.Transparent,
                                                activeAccent.copy(alpha = 0.09f),
                                                Color.Transparent,
                                            ),
                                            start = Offset.Zero,
                                            end = Offset(400f, 600f),
                                        )
                                    )
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Mini status header (interactive tap on time / theme badge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (previewUse24Hour) "21:41" else "9:41",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            haptics?.lightTap()
                                            previewUse24Hour = !previewUse24Hour
                                        },
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (previewWifiActive) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Battery5Bar,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = activeAccent.copy(alpha = 0.20f),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                haptics?.sliderTick()
                                                val nextTheme = when (selectedTheme) {
                                                    is ShadeTheme.OneUI -> ShadeTheme.Pixel
                                                    is ShadeTheme.Pixel -> ShadeTheme.PureMaterial
                                                    is ShadeTheme.PureMaterial -> ShadeTheme.Nothing
                                                    is ShadeTheme.Nothing -> ShadeTheme.Cyberpunk
                                                    is ShadeTheme.Cyberpunk -> ShadeTheme.OneUI
                                                }
                                                onThemeChange(nextTheme)
                                            },
                                    ) {
                                        Text(
                                            text = when (selectedTheme) {
                                                is ShadeTheme.OneUI -> "One UI 8 ↻"
                                                is ShadeTheme.Pixel -> "Pixel ↻"
                                                is ShadeTheme.PureMaterial -> "Material ↻"
                                                is ShadeTheme.Nothing -> "Nothing ↻"
                                                is ShadeTheme.Cyberpunk -> "Cyberpunk ↻"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                                            color = activeAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                            }

                            // Mini Quick Settings Preview — Adapts based on selected theme
                            if (selectedTheme is ShadeTheme.Pixel) {
                                // Pixel 2-column wide stadium pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Pill 1: Internet
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (previewWifiActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewWifiActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewWifiActive = !previewWifiActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (previewWifiActive) Color.White.copy(alpha = 0.20f)
                                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Wifi,
                                                    contentDescription = null,
                                                    tint = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Internet",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = if (previewWifiActive) "Connected" else "Off",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                    color = (if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }

                                    // Pill 2: Bluetooth
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (previewBtActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewBtActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewBtActive = !previewBtActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (previewBtActive) Color.White.copy(alpha = 0.20f)
                                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bluetooth,
                                                    contentDescription = null,
                                                    tint = if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "Bluetooth",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = if (previewBtActive) "Active" else "Off",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                    color = (if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pixel Brightness Slider: Thick 28dp stadium pill with Sun icon embedded inside the track
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                haptics?.sliderTick()
                                                previewBrightnessFraction = (offset.x / size.width).coerceIn(0.08f, 1.0f)
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures { change, _ ->
                                                change.consume()
                                                val newFrac = (change.position.x / size.width).coerceIn(0.08f, 1.0f)
                                                if ((previewBrightnessFraction * 12).roundToInt() != (newFrac * 12).roundToInt()) {
                                                    haptics?.sliderTick()
                                                }
                                                previewBrightnessFraction = newFrac
                                            }
                                        },
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(previewBrightnessFraction)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(activeAccent),
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrightnessMedium,
                                                contentDescription = null,
                                                tint = if (previewBrightnessFraction > 0.15f) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Text(
                                                text = "${(previewBrightnessFraction * 100).roundToInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = if (previewBrightnessFraction > 0.85f) Color.White else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            } else if (selectedTheme is ShadeTheme.OneUI) {
                                // One UI 8.5/9: 2 prominent squircle connectivity cards + dual slider with 'A' badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // One UI Card 1: Wi-Fi
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (previewWifiActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewWifiActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewWifiActive = !previewWifiActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (previewWifiActive) Color.White.copy(alpha = 0.22f)
                                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Wifi,
                                                    contentDescription = null,
                                                    tint = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Wi-Fi",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = if (previewWifiActive) "Connected" else "Off",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                    color = (if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = (if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }

                                    // One UI Card 2: Bluetooth
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (previewBtActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewBtActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewBtActive = !previewBtActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (previewBtActive) Color.White.copy(alpha = 0.22f)
                                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bluetooth,
                                                    contentDescription = null,
                                                    tint = if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Bluetooth",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                )
                                                Text(
                                                    text = if (previewBtActive) "Active" else "Off",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                    color = (if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = (if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface).copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }

                                // One UI Brightness Slider with 'A' auto badge
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                haptics?.sliderTick()
                                                previewBrightnessFraction = (offset.x / size.width).coerceIn(0.08f, 1.0f)
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures { change, _ ->
                                                change.consume()
                                                val newFrac = (change.position.x / size.width).coerceIn(0.08f, 1.0f)
                                                if ((previewBrightnessFraction * 12).roundToInt() != (newFrac * 12).roundToInt()) {
                                                    haptics?.sliderTick()
                                                }
                                                previewBrightnessFraction = newFrac
                                            }
                                        },
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(previewBrightnessFraction)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xFFF57C00),
                                                            Color(0xFFFF9800),
                                                            Color(0xFFFFCA28),
                                                        )
                                                    )
                                                )
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrightnessMedium,
                                                contentDescription = null,
                                                tint = if (previewBrightnessFraction > 0.15f) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Text(
                                                    text = "${(previewBrightnessFraction * 100).roundToInt()}%",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = if (previewBrightnessFraction > 0.85f) Color.White else MaterialTheme.colorScheme.onSurface,
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 16.dp, height = 14.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.White.copy(alpha = 0.22f)),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "A",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black),
                                                        color = Color.White,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (selectedTheme is ShadeTheme.Nothing) {
                                // Nothing OS 3.0: High-contrast monochrome cards with signature red glyph dot
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = if (!previewWifiActive) BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewWifiActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewWifiActive = !previewWifiActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFD71920)),
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "WI-FI",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.8.sp,
                                                    ),
                                                    color = if (previewWifiActive) Color.Black else Color.White,
                                                )
                                                Text(
                                                    text = if (previewWifiActive) "CONNECTED" else "OFF",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                    color = if (previewWifiActive) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (previewBtActive) Color.White else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = if (!previewBtActive) BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewBtActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewBtActive = !previewBtActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (previewBtActive) Color(0xFFD71920) else Color.Gray),
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "BLUETOOTH",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.8.sp,
                                                    ),
                                                    color = if (previewBtActive) Color.Black else Color.White,
                                                )
                                                Text(
                                                    text = if (previewBtActive) "ACTIVE" else "OFF",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                                    color = if (previewBtActive) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Nothing OS Segmented Brightness Slider
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                haptics?.sliderTick()
                                                previewBrightnessFraction = (offset.x / size.width).coerceIn(0.08f, 1.0f)
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures { change, _ ->
                                                change.consume()
                                                val newFrac = (change.position.x / size.width).coerceIn(0.08f, 1.0f)
                                                if ((previewBrightnessFraction * 12).roundToInt() != (newFrac * 12).roundToInt()) {
                                                    haptics?.sliderTick()
                                                }
                                                previewBrightnessFraction = newFrac
                                            }
                                        },
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(previewBrightnessFraction)
                                                .background(Color.White),
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrightnessMedium,
                                                contentDescription = null,
                                                tint = if (previewBrightnessFraction > 0.15f) Color.Black else Color.White,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Text(
                                                text = "${(previewBrightnessFraction * 100).roundToInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                ),
                                                color = if (previewBrightnessFraction > 0.85f) Color.Black else Color.White,
                                            )
                                        }
                                    }
                                }
                            } else if (selectedTheme is ShadeTheme.Cyberpunk) {
                                // Cyberpunk HUD: Chamfered corner cards with neon cyan/magenta telemetry
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        shape = ChamferedCornerShape(6.dp),
                                        color = if (previewWifiActive) Color(0xFF00F0FF).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = if (previewWifiActive) 0.85f else 0.35f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewWifiActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewWifiActive = !previewWifiActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = Color(0xFF00F0FF),
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "[WIFI.NET]",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = Color(0xFF00F0FF),
                                                )
                                                Text(
                                                    text = if (previewWifiActive) "// ACTIVE" else "// OFFLINE",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                    ),
                                                    color = Color.White.copy(alpha = 0.7f),
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = ChamferedCornerShape(6.dp),
                                        color = if (previewBtActive) Color(0xFFFF0055).copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = BorderStroke(1.dp, Color(0xFFFF0055).copy(alpha = if (previewBtActive) 0.85f else 0.35f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clickable {
                                                if (!previewBtActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewBtActive = !previewBtActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = Color(0xFFFF0055),
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "[BT.LINK]",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = Color(0xFFFF0055),
                                                )
                                                Text(
                                                    text = if (previewBtActive) "// PAIRED" else "// STANDBY",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                    ),
                                                    color = Color.White.copy(alpha = 0.7f),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Cyberpunk Neon Slider
                                Surface(
                                    shape = ChamferedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, Color(0xFF00F0FF).copy(alpha = 0.40f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                haptics?.sliderTick()
                                                previewBrightnessFraction = (offset.x / size.width).coerceIn(0.08f, 1.0f)
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures { change, _ ->
                                                change.consume()
                                                val newFrac = (change.position.x / size.width).coerceIn(0.08f, 1.0f)
                                                if ((previewBrightnessFraction * 12).roundToInt() != (newFrac * 12).roundToInt()) {
                                                    haptics?.sliderTick()
                                                }
                                                previewBrightnessFraction = newFrac
                                            }
                                        },
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(previewBrightnessFraction)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF00F0FF), Color(0xFFFF0055))
                                                    )
                                                ),
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrightnessMedium,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp),
                                            )
                                            Text(
                                                text = "LUM // ${(previewBrightnessFraction * 100).roundToInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Pure Material 4-toggle row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Tile 1: Wi-Fi
                                    Surface(
                                        shape = previewTileShape,
                                        color = if (previewWifiActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable {
                                                if (!previewWifiActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewWifiActive = !previewWifiActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Wifi,
                                                contentDescription = null,
                                                tint = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "Wi-Fi",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = if (previewWifiActive) FontWeight.Bold else FontWeight.Medium,
                                                ),
                                                color = if (previewWifiActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                            )
                                        }
                                    }

                                    // Tile 2: Bluetooth
                                    Surface(
                                        shape = previewTileShape,
                                        color = if (previewBtActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable {
                                                if (!previewBtActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewBtActive = !previewBtActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "BT",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = if (previewBtActive) FontWeight.Bold else FontWeight.Medium,
                                                ),
                                                color = if (previewBtActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                            )
                                        }
                                    }

                                    // Tile 3: Sound
                                    Surface(
                                        shape = previewTileShape,
                                        color = if (previewSoundActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable {
                                                if (!previewSoundActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewSoundActive = !previewSoundActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = null,
                                                tint = if (previewSoundActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "Sound",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = if (previewSoundActive) FontWeight.Bold else FontWeight.Medium,
                                                ),
                                                color = if (previewSoundActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                            )
                                        }
                                    }

                                    // Tile 4: Portrait
                                    Surface(
                                        shape = previewTileShape,
                                        color = if (previewPortraitActive) activeAccent else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .clickable {
                                                if (!previewPortraitActive) haptics?.tileToggleOn() else haptics?.tileToggleOff()
                                                previewPortraitActive = !previewPortraitActive
                                            },
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ScreenLockPortrait,
                                                contentDescription = null,
                                                tint = if (previewPortraitActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "Portrait",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = if (previewPortraitActive) FontWeight.Bold else FontWeight.Medium,
                                                ),
                                                color = if (previewPortraitActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }

                                // Mini Brightness Slider (Tactile scrub/tap)
                                Surface(
                                    shape = previewShapeScheme.slider,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures { offset ->
                                                haptics?.sliderTick()
                                                previewBrightnessFraction = (offset.x / size.width).coerceIn(0.08f, 1.0f)
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures { change, _ ->
                                                change.consume()
                                                val newFrac = (change.position.x / size.width).coerceIn(0.08f, 1.0f)
                                                if ((previewBrightnessFraction * 12).roundToInt() != (newFrac * 12).roundToInt()) {
                                                    haptics?.sliderTick()
                                                }
                                                previewBrightnessFraction = newFrac
                                            }
                                        },
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(previewBrightnessFraction)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            Color(0xFFFFA000),
                                                            Color(0xFFFFD54F),
                                                        )
                                                    )
                                                )
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BrightnessMedium,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp),
                                            )
                                            Text(
                                                text = "${(previewBrightnessFraction * 100).roundToInt()}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Shade Style
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Shade Style & OS Design",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    val themes = listOf(
                        ShadeTheme.OneUI,
                        ShadeTheme.Pixel,
                        ShadeTheme.PureMaterial,
                        ShadeTheme.Nothing,
                        ShadeTheme.Cyberpunk,
                    )
                    val labels = listOf("One UI 8", "Pixel", "Material", "Nothing", "Cyberpunk")
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
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.sp,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
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

                // Glass & Backdrop Theme
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Glass & Backdrop Theme",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ) {
                                Text(
                                    text = backdropTheme.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            ) {
                                Text(
                                    text = "${(backdropOpacity * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = backdropTheme.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val backdropOptions = listOf(
                        BackdropTheme.FROSTED_GLASS,
                        BackdropTheme.LIQUID_GLASS,
                        BackdropTheme.BLURRY,
                        BackdropTheme.OPAQUE,
                        BackdropTheme.TRANSPARENT,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        backdropOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = backdropTheme == option,
                                onClick = { onBackdropThemeChange(option) },
                                shape = SegmentedButtonDefaults.itemShape(index, backdropOptions.size),
                                icon = {},
                            ) {
                                Text(
                                    text = when (option) {
                                        BackdropTheme.FROSTED_GLASS -> "Frosted"
                                        BackdropTheme.LIQUID_GLASS -> "Liquid"
                                        BackdropTheme.BLURRY -> "Blurry"
                                        BackdropTheme.OPAQUE -> "Opaque"
                                        BackdropTheme.TRANSPARENT -> "Clear"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // Expressive Transparency Slider
                    var isDraggingSlider by remember { mutableStateOf(false) }
                    var liveSliderValue by remember(backdropOpacity) { mutableFloatStateOf(backdropOpacity) }
                    val currentDisplayOpacity = if (isDraggingSlider) liveSliderValue else backdropOpacity

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "Transparency",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Slider(
                            value = currentDisplayOpacity,
                            onValueChange = { newValue ->
                                isDraggingSlider = true
                                val lastStep = (liveSliderValue * 20).roundToInt()
                                val newStep = (newValue * 20).roundToInt()
                                if (lastStep != newStep) {
                                    haptics?.sliderTick()
                                }
                                liveSliderValue = newValue
                                onBackdropOpacityChange(newValue)
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onBackdropOpacityChange(liveSliderValue)
                            },
                            valueRange = 0.20f..1.00f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            ),
                        )
                        Text(
                            text = "${(currentDisplayOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(38.dp),
                        )
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Card Borders & Outlines
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Card Border Stroke",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "Outline thickness around shade cards, sliders, and tiles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CardBorderWidth.entries.forEachIndexed { index, width ->
                            SegmentedButton(
                                selected = cardBorderWidth == width,
                                onClick = {
                                    haptics?.sliderTick()
                                    onCardBorderWidthChange(width)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, CardBorderWidth.entries.size),
                                icon = {},
                            ) {
                                Text(
                                    text = width.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                }
            }
        }

        // =======================================================================
        // 6. QUICK TILES & GRID CUSTOMIZATION
        // =======================================================================
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
                                TileShape.LEAF -> RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
                                TileShape.SHARP -> RoundedCornerShape(6.dp)
                                TileShape.CLOVER -> com.supershade.ui.theme.M3ExpressiveShapes.Clover4.toComposeShape()
                                TileShape.BURST -> com.supershade.ui.theme.M3ExpressiveShapes.SoftBurst8.toComposeShape()
                            }
                            Surface(
                                shape = previewCorner,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f),
                                ),
                                modifier = Modifier
                                    .width(112.dp)
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
                                            fontSize = 10.5.sp,
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

                // Tile Size & Touch Ergonomics
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tile Size & Height",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = when (tileSize) {
                            TileSize.COMPACT -> "Compact (58dp): Streamlined profile leaving extra space for notifications and media"
                            TileSize.COMFORTABLE -> "Tall & Spacious (84dp): Large touch targets and enhanced thumb readability"
                            else -> "Standard (72dp): Balanced One UI 8 height with clear icon, label & status"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val sizeOptions = listOf(
                        TileSize.COMPACT to "Compact 58dp",
                        TileSize.STANDARD to "Standard 72dp",
                        TileSize.COMFORTABLE to "Tall 84dp",
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sizeOptions.forEachIndexed { index, (sizeOption, label) ->
                            SegmentedButton(
                                selected = tileSize == sizeOption,
                                onClick = { onTileSizeChange(sizeOption) },
                                shape = SegmentedButtonDefaults.itemShape(index, sizeOptions.size),
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

                // Device Control & Media Mode (One UI 8.5/9 style)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Device control & Media",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "When to show device/media output buttons",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DeviceControlMode.entries.forEach { mode ->
                            val isSelected = deviceControlMode == mode
                            Surface(
                                onClick = {
                                    haptics?.sliderTick()
                                    onDeviceControlModeChange(mode)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected)
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                else
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.sp,
                                    ),
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

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
                            text = "💡 Tip: Customize, reorder, and add tiles directly in SuperShade by tapping the edit pencil in the header.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        // =======================================================================
        // 6. NOTIFICATIONS
        // =======================================================================
        SectionHeader(
            title = "Notifications",
            badge = notificationDensity.label.substringBefore(" ("),
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
                // Notification Density
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text(
                            text = "Notification Density",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = notificationDensity.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val densities = NotificationDensity.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        densities.forEachIndexed { index, density ->
                            SegmentedButton(
                                selected = notificationDensity == density,
                                onClick = { onNotificationDensityChange(density) },
                                shape = SegmentedButtonDefaults.itemShape(index, densities.size),
                                icon = {},
                            ) {
                                Text(
                                    text = density.label.substringBefore(" ("),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                // Sticky / Ongoing Notifications toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide Sticky Notifications",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Hide persistent background and ongoing system notifications from the feed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = hideOngoingNotifications,
                        onCheckedChange = { onHideOngoingNotificationsChange(it) },
                    )
                }

                // Hidden Categories manager
                if (hiddenChannels.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Hidden Categories",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "${hiddenChannels.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = "Categories hidden via long-press. Tap an item to restore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            hiddenChannels.forEach { channelKey ->
                                val displayLabel = channelKey.substringAfterLast("/")
                                    .ifBlank { channelKey.substringBefore("/") }
                                Surface(
                                    onClick = { onUnhideChannel(channelKey) },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = getCardBorder(alpha = 0.35f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =======================================================================
        // 7. ABOUT & UPDATES
        // =======================================================================
        var devTapCount by remember { mutableIntStateOf(0) }

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
                // Header with logo and tap-for-dev version
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            devTapCount++
                            if (devTapCount == 7) {
                                android.widget.Toast.makeText(context, "Experimental & Developer Options Unlocked! 🚀", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (devTapCount in 4..6) {
                                android.widget.Toast.makeText(context, "${7 - devTapCount} more taps to unlock developer options", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notification_shade),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
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
                }

                // Action buttons row: What's New & Updates side-by-side with no text overlap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onShowWhatsNew,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("What's new", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onCheckUpdate,
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Checking…", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Updates", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Developer & Experimental Options (revealed only after 7 taps!)
        AnimatedVisibility(
            visible = devTapCount >= 7,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionHeader(
                    title = "Experimental & Developer Options",
                    badge = "Unlocked",
                    badgeColor = MaterialTheme.colorScheme.tertiary,
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp),
                            )
                            Column {
                                Text(
                                    text = "Developer Diagnostics",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(
                                    text = "Runtime system information & active service telemetry",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Device: ${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Shizuku: ${if (shizukuOk) "Authorized & Active" else if (shizukuConnected) "Connected (Awaiting Perm)" else "Not Running"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (shizukuOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Accessibility Interceptor: ${if (accessibilityGranted) "Active" else "Disabled"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (accessibilityGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
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
