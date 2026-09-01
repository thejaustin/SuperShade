package com.supershade.ui.shade

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.supershade.viewmodel.StatusBarState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(): String =
    SimpleDateFormat("h:mm", Locale.getDefault()).format(Date())

private fun formatDate(): String =
    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())

@Composable
fun StatusBarRow(statusBar: StatusBarState) {
    val context = LocalContext.current

    var time by remember { mutableStateOf(formatTime()) }
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

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            time = formatTime()
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        // OneUI signature: large lightweight clock + date stacked on the left
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = time,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Right: battery percentage + icon, top-aligned
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$batteryPct%",
                style = MaterialTheme.typography.bodySmall,
                color = batteryTint,
            )
            Icon(
                imageVector = batteryIcon,
                contentDescription = "Battery",
                tint = batteryTint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
