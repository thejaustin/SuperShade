package com.supershade.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

class ShizukuPlusConnector(private val context: Context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var _serviceBinder: IBinder? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isConnected.value = true
        _serviceBinder = Shizuku.getBinder()
        grantRequiredPermissions()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isConnected.value = false
        _serviceBinder = null
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    fun getServiceBinder(): IBinder? = _serviceBinder

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    private fun grantRequiredPermissions() {
        val perms = listOf(
            "android.permission.STATUS_BAR",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.WRITE_SETTINGS",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        )
        perms.forEach { perm ->
            try {
                if (context.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.newProcess(
                        arrayOf("pm", "grant", context.packageName, perm),
                        null, null
                    ).waitFor()
                }
            } catch (_: Exception) {}
        }
    }

    fun cleanup() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}
