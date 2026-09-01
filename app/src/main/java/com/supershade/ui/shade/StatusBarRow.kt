package com.supershade.ui.shade

import android.content.IntentFilter
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.supershade.viewmodel.StatusBarState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(): String =
    SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

@Composable
fun StatusBarRow(statusBar: StatusBarState) {
    val context = LocalContext.current

    // Live time — updates every 30 seconds
    var time by remember { mutableStateOf(formatTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            time = formatTime()
        }
    }

    // Live battery — read from sticky ACTION_BATTERY_CHANGED broadcast
    val batteryIntent = remember(context) {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val initialLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val initialScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val initialPct = if (initialLevel >= 0 && initialScale > 0) {
        (initialLevel * 100 / initialScale)
    } else {
        statusBar.batteryPct
    }
    val initialStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val initialCharging = initialStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            initialStatus == BatteryManager.BATTERY_STATUS_FULL

    var batteryPct by remember { mutableStateOf(initialPct) }
    var isCharging by remember { mutableStateOf(initialCharging) }

    // Refresh battery reading every 30 seconds alongside the clock tick
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                batteryPct = level * 100 / scale
            }
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    // Icon selection
    val batteryIcon = when {
        isCharging -> Icons.Default.BatteryChargingFull
        batteryPct >= 80 -> Icons.Default.Battery6Bar
        batteryPct >= 60 -> Icons.Default.Battery5Bar
        batteryPct >= 40 -> Icons.Default.Battery4Bar
        batteryPct >= 20 -> Icons.Default.Battery3Bar
        else -> Icons.Default.Battery2Bar
    }

    // Tint selection
    val batteryTint = when {
        isCharging -> Color(0xFF4CAF50)
        batteryPct < 20 -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$batteryPct%",
                style = MaterialTheme.typography.bodySmall,
                color = batteryTint
            )
            Icon(
                imageVector = batteryIcon,
                contentDescription = "Battery",
                tint = batteryTint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
