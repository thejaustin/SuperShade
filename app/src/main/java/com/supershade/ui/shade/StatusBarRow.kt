package com.supershade.ui.shade

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import com.supershade.ui.theme.getCardBorder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.haptics.LocalSuperHaptics
import com.supershade.ui.shade.nothing.DotMatrixClockDisplay
import com.supershade.ui.shade.nothing.NothingBatteryPill
import com.supershade.ui.theme.LocalShadeShapeScheme
import com.supershade.ui.theme.LocalShadeTheme
import com.supershade.ui.theme.ShadeTheme
import com.supershade.viewmodel.StatusBarState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

private fun formatAMPM(): String =
    SimpleDateFormat("a", Locale.getDefault()).format(Date())

private fun formatDate(): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())

private fun formatNetSpeed(bytesPerSec: Long): String = when {
    bytesPerSec < 1_024L              -> "${bytesPerSec} B/s"
    bytesPerSec < 1_048_576L          -> "${bytesPerSec / 1_024} KB/s"
    else                              -> "%.1f MB/s".format(bytesPerSec / 1_048_576.0)
}

private fun launchClock(context: Context) {
    val intents = listOf(
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(AlarmClock.ACTION_SET_ALARM),
        context.packageManager.getLaunchIntentForPackage("com.sec.android.app.clockpackage"),
        context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock"),
        context.packageManager.getLaunchIntentForPackage("com.android.deskclock"),
    )
    for (intent in intents) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}

private fun launchCalendar(context: Context) {
    val intents = listOf(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
        context.packageManager.getLaunchIntentForPackage("com.samsung.android.calendar"),
        context.packageManager.getLaunchIntentForPackage("com.google.android.calendar"),
        Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("content://com.android.calendar/time")),
    )
    for (intent in intents) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}

private fun launchBatterySettings(context: Context) {
    val intents = listOf(
        Intent(Intent.ACTION_POWER_USAGE_SUMMARY),
        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
    for (intent in intents) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            return
        } catch (_: Exception) {}
    }
}

private fun launchSystemSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {}
}

private fun launchSearch(context: Context) {
    val intents = listOf(
        Intent("com.sec.android.app.launcher.action.OPEN_SEARCH"),
        context.packageManager.getLaunchIntentForPackage("com.sec.android.app.launcher.search"),
        Intent(Intent.ACTION_WEB_SEARCH),
        Intent(Intent.ACTION_ASSIST),
        Intent(Settings.ACTION_SEARCH_SETTINGS),
    )
    for (intent in intents) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}

/**
 * Official Samsung One UI 8.5/9 Horizontal Battery Capsule.
 * Renders an authentic rounded capsule with exact internal fill fraction
 * and positive terminal cap.
 */
@Composable
private fun OneUIBatteryBar(
    batteryPct: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val levelFraction = (batteryPct / 100f).coerceIn(0f, 1f)
    val barColor = when {
        isCharging -> Color(0xFF2ED573) // One UI Green
        batteryPct <= 15 -> Color(0xFFFF4757) // One UI Red alert
        batteryPct <= 25 -> Color(0xFFFFA502) // One UI Amber
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Text(
            text = "$batteryPct%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = (-0.3).sp,
            ),
            color = barColor
        )

        // Custom Canvas Battery Capsule
        Canvas(modifier = Modifier.size(width = 24.dp, height = 12.dp)) {
            val strokeWidth = 1.25.dp.toPx()
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val capWidth = 2.dp.toPx()
            val mainWidth = size.width - capWidth - 1.dp.toPx()

            // Outer shell
            drawRoundRect(
                color = barColor.copy(alpha = 0.40f),
                topLeft = Offset(0f, 0f),
                size = Size(mainWidth, size.height),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidth)
            )

            // Positive terminal nub on right
            drawRoundRect(
                color = barColor.copy(alpha = 0.40f),
                topLeft = Offset(mainWidth + 0.5.dp.toPx(), size.height * 0.28f),
                size = Size(capWidth, size.height * 0.44f),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )

            // Dynamic internal fill level with inset
            val inset = strokeWidth + 1.dp.toPx()
            val fillMaxWidth = (mainWidth - inset * 2).coerceAtLeast(0f)
            val fillActualWidth = fillMaxWidth * levelFraction
            if (fillActualWidth > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(inset, inset),
                    size = Size(fillActualWidth, size.height - inset * 2),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun StatusBarRow(
    statusBar: StatusBarState,
    isEditing: Boolean = false,
    onOpenPowerMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenDeviceSettings: () -> Unit = {},
    onOpenEdit: () -> Unit = {},
    onLockScreen: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }
    val shapes = LocalShadeShapeScheme.current
    val shadeTheme = LocalShadeTheme.current

    var time by remember { mutableStateOf(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())) }
    var ampm by remember { mutableStateOf(SimpleDateFormat("a", Locale.getDefault()).format(Date())) }
    var date by remember { mutableStateOf(formatDate()) }

    // Battery state derived from statusBar with hardware fallback
    val batteryPct = remember(statusBar.batteryPct) {
        if (statusBar.batteryPct > 0) statusBar.batteryPct
        else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val cap = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (cap in 1..100) cap else 100
        }
    }
    val isCharging = statusBar.isCharging

    // Network speed: computed from TrafficStats delta every second.
    var netDown by remember { mutableStateOf("") }
    var netUp   by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        while (true) {
            delay(1_000L)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            netDown = if (rx > 0 && lastRx > 0 && rx > lastRx) formatNetSpeed(rx - lastRx) else ""
            netUp   = if (tx > 0 && lastTx > 0 && tx > lastTx) formatNetSpeed(tx - lastTx) else ""
            lastRx = rx; lastTx = tx
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val delayMs = (60_000L - (now % 60_000L)).coerceIn(100L, 60_000L)
            delay(delayMs)
            time = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())
            ampm = SimpleDateFormat("a", Locale.getDefault()).format(Date())
            date = formatDate()
        }
    }

    val batteryIcon = when {
        isCharging       -> Icons.Default.BatteryChargingFull
        batteryPct >= 80 -> Icons.Default.Battery6Bar
        batteryPct >= 60 -> Icons.Default.Battery5Bar
        batteryPct >= 40 -> Icons.Default.Battery4Bar
        batteryPct >= 20 -> Icons.Default.Battery3Bar
        else             -> Icons.Default.Battery2Bar
    }
    val batteryTint = when {
        isCharging      -> Color(0xFF4CAF50)
        batteryPct < 20 -> Color(0xFFEF5350)
        else            -> MaterialTheme.colorScheme.onBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        // OneUI signature: large lightweight clock + date stacked on the left
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Two-part clock: large digits + smaller AM/PM — One UI style
            Row(
                modifier = Modifier
                    .clip(shapes.chip)
                    .combinedClickable(
                        onClick = {
                            haptics.lightTap()
                            launchClock(context)
                        },
                        onDoubleClick = {
                            haptics.heavyClick()
                            onLockScreen()
                        },
                        role = Role.Button,
                    )
                    .semantics {
                        contentDescription = "Clock: $time $ampm. Double tap anywhere on header to sleep"
                    },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (shadeTheme is ShadeTheme.Nothing) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 14.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD71920))
                    )
                    DotMatrixClockDisplay(
                        timeString = time,
                        activeColor = MaterialTheme.colorScheme.onBackground,
                        dotSize = 5.dp,
                        dotSpacing = 2.5.dp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                } else {
                    Text(
                        text = time,
                        style = when (shadeTheme) {
                            is ShadeTheme.Cyberpunk -> MaterialTheme.typography.displaySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp,
                            )
                            else -> MaterialTheme.typography.displaySmall
                        },
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (shadeTheme is ShadeTheme.Cyberpunk) "// $ampm" else ampm,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (shadeTheme is ShadeTheme.Cyberpunk) FontWeight.Bold else FontWeight.Light,
                            fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            val displayDate = when (shadeTheme) {
                is ShadeTheme.Cyberpunk -> "[SYS.DAT // $date]"
                is ShadeTheme.Nothing -> date.uppercase()
                else -> date
            }
            Text(
                text = displayDate,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                    letterSpacing = if (shadeTheme is ShadeTheme.Nothing) 1.2.sp else 0.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(shapes.chip)
                    .clickable(
                        onClick = {
                            haptics.lightTap()
                            launchCalendar(context)
                        },
                        role = Role.Button,
                    )
                    .semantics {
                        contentDescription = "Date: $date"
                    }
                    .padding(vertical = 2.dp),
            )
            val netLabel = buildList {
                if (netDown.isNotEmpty()) {
                    if (shadeTheme is ShadeTheme.Cyberpunk) add("DL:$netDown") else add("↓ $netDown")
                }
                if (netUp.isNotEmpty()) {
                    if (shadeTheme is ShadeTheme.Cyberpunk) add("UL:$netUp") else add("↑ $netUp")
                }
            }.joinToString("  ")
            if (netLabel.isNotEmpty()) {
                Text(
                    text = if (shadeTheme is ShadeTheme.Cyberpunk) "[NET // $netLabel]" else netLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        // Center space: double-tap to lock screen without interfering with buttons
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onDoubleClick = {
                        haptics.heavyClick()
                        onLockScreen()
                    },
                )
        )

        // Right side: Header actions (Edit, Power & Settings) + Battery Status
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top action buttons: Finder Search (One UI), Edit quick tiles, Power menu & Settings gear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // One UI 8.5/9 Finder/Search button
                if (shadeTheme is ShadeTheme.OneUI) {
                    Surface(
                        onClick = {
                            haptics.lightTap()
                            launchSearch(context)
                        },
                        shape = shapes.pill,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                        border = getCardBorder(alpha = 0.25f),
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Finder Search"
                            },
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        haptics.sheetDetent()
                        onOpenEdit()
                    },
                    shape = shapes.pill,
                    color = if (isEditing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    border = getCardBorder(alpha = 0.25f),
                    modifier = Modifier
                        .size(44.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (isEditing) "Done Editing Quick Settings" else "Edit Quick Settings"
                        },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isEditing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptics.sheetDetent()
                        onOpenPowerMenu()
                    },
                    shape = shapes.pill,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    border = getCardBorder(alpha = 0.25f),
                    modifier = Modifier
                        .size(44.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Power options"
                        },
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }

                Surface(
                    shape = shapes.pill,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    border = getCardBorder(alpha = 0.25f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = {
                                    haptics.sheetDetent()
                                    launchSystemSettings(context)
                                    onOpenDeviceSettings()
                                },
                                onLongClick = {
                                    haptics.sheetDetent()
                                    onOpenSettings()
                                },
                                role = Role.Button,
                            )
                            .semantics {
                                contentDescription = "Settings. Tap for Android Settings, long press for SuperShade Settings"
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }

            // Battery percentage + icon with comfortable capsule pill & full accessibility description
            if (shadeTheme is ShadeTheme.Nothing) {
                NothingBatteryPill(
                    batteryPct = batteryPct,
                    isCharging = isCharging,
                    modifier = Modifier
                        .clip(shapes.chip)
                        .clickable(
                            onClick = {
                                haptics.lightTap()
                                launchBatterySettings(context)
                            },
                            role = Role.Button,
                        )
                        .semantics {
                            contentDescription = "$batteryPct percent battery" + if (isCharging) ", charging" else ""
                        },
                )
            } else if (shadeTheme is ShadeTheme.OneUI) {
                OneUIBatteryBar(
                    batteryPct = batteryPct,
                    isCharging = isCharging,
                    modifier = Modifier
                        .clip(shapes.chip)
                        .clickable(
                            onClick = {
                                haptics.lightTap()
                                launchBatterySettings(context)
                            },
                            role = Role.Button,
                        )
                        .semantics {
                            contentDescription = "$batteryPct percent battery" + if (isCharging) ", charging" else ""
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            } else {
                Surface(
                    shape = shapes.chip,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    border = getCardBorder(alpha = 0.30f),
                    modifier = Modifier
                        .clip(shapes.chip)
                        .clickable(
                            onClick = {
                                haptics.lightTap()
                                launchBatterySettings(context)
                            },
                            role = Role.Button,
                        )
                        .semantics {
                            contentDescription = "$batteryPct percent battery" + if (isCharging) ", charging" else ""
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 36.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = when (shadeTheme) {
                                is ShadeTheme.Cyberpunk -> "PWR // $batteryPct%"
                                else -> "$batteryPct%"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (shadeTheme is ShadeTheme.Cyberpunk) FontFamily.Monospace else null,
                            ),
                            color = batteryTint,
                        )
                        Icon(
                            imageVector = batteryIcon,
                            contentDescription = null,
                            tint = batteryTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
