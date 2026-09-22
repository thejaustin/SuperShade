package com.supershade.domain.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class BatteryState(
    val levelPct: Int,
    val isCharging: Boolean,
    val isPowerSave: Boolean,
)

/**
 * Provides reactive system status flows for battery percentage, charging state,
 * and battery saver mode, eliminating ad-hoc polling in UI layers.
 */
class SystemStatusRepository(private val context: Context) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    val batteryState: Flow<BatteryState> = callbackFlow {
        fun computeState(intent: Intent?): BatteryState {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val isPowerSave = powerManager?.isPowerSaveMode == true
            return BatteryState(pct, isCharging, isPowerSave)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                trySend(computeState(intent))
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }

        val stickyIntent = context.registerReceiver(receiver, filter)
        trySend(computeState(stickyIntent))

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }.conflate()
}
