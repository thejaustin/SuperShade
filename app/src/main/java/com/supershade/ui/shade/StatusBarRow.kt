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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.supershade.haptics.LocalSuperHaptics
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

@Composable
fun StatusBarRow(
    statusBar: StatusBarState,
    onOpenPowerMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onLockScreen: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalSuperHaptics.current ?: remember(context) { com.supershade.haptics.SuperHaptics(context) }

    var time by remember { mutableStateOf(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())) }
    var ampm by remember { mutableStateOf(SimpleDateFormat("a", Locale.getDefault()).format(Date())) }
    var date by remember { mutableStateOf(formatDate()) }

    // Resolve initial battery state from sticky broadcast
    val batteryIntent = remember(context) {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val initLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val initScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val initPct = if (initLevel >= 0 && initScale > 0) (initLevel * 100 / initScale) else statusBar.batteryPct
    val initStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

    var batteryPct by remember { mutableIntStateOf(initPct) }
    var isCharging by remember {
        mutableStateOf(
            initStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            initStatus == BatteryManager.BATTERY_STATUS_FULL
        )
    }

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
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
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
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onDoubleClick = {
                    haptics.heavyClick()
                    onLockScreen()
                },
            )
            .padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        // OneUI signature: large lightweight clock + date stacked on the left
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Two-part clock: large digits + smaller AM/PM — One UI style
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
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
                Text(
                    text = time,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = ampm,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
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
                if (netDown.isNotEmpty()) add("↓ $netDown")
                if (netUp.isNotEmpty())   add("↑ $netUp")
            }.joinToString("  ")
            if (netLabel.isNotEmpty()) {
                Text(
                    text = netLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }

        // Right side: Header actions (Power & Settings) + Battery Status
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Top action buttons: Power menu & Settings gear with 44dp minimum touch targets
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = {
                        haptics.sheetDetent()
                        onOpenPowerMenu()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Power options"
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = {
                                haptics.sheetDetent()
                                onOpenSettings()
                            },
                            onLongClick = {
                                haptics.sheetDetent()
                                launchSystemSettings(context)
                            },
                            role = Role.Button,
                        )
                        .semantics {
                            contentDescription = "Settings"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Battery percentage + icon with comfortable capsule pill & full accessibility description
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                border = getCardBorder(alpha = 0.30f),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
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
                        text = "$batteryPct%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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
