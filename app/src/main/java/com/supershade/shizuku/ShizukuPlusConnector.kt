package com.supershade.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuPlusConnector(private val context: Context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermissionFlow: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var _serviceBinder: IBinder? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isConnected.value = true
        _serviceBinder = Shizuku.getBinder()
        updatePermissionState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isConnected.value = false
        _serviceBinder = null
        _hasPermission.value = false
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        _hasPermission.value = granted
    }

    init {
        try {
            if (Shizuku.pingBinder()) {
                _isConnected.value = true
                _serviceBinder = Shizuku.getBinder()
                updatePermissionState()
            }
        } catch (e: Exception) {}

        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {}
    }

    fun updatePermissionState() {
        _hasPermission.value = hasPermission()
    }

    fun getServiceBinder(): IBinder? = _serviceBinder

    fun hasPermission(): Boolean = try {
        if (!Shizuku.pingBinder()) false
        else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) { false }

    fun requestPermission(requestCode: Int = 0) {
        try {
            if (Shizuku.pingBinder() && !hasPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {}
    }

    fun isInstalled(): Boolean {
        val pm = context.packageManager
        return isPackageInstalled(pm, PACKAGE_SHIZUKU_PLUS) || isPackageInstalled(pm, PACKAGE_SHIZUKU_STANDARD)
    }

    fun getManagerPackage(): String? {
        val pm = context.packageManager
        return when {
            isPackageInstalled(pm, PACKAGE_SHIZUKU_PLUS) -> PACKAGE_SHIZUKU_PLUS
            isPackageInstalled(pm, PACKAGE_SHIZUKU_STANDARD) -> PACKAGE_SHIZUKU_STANDARD
            else -> null
        }
    }

    fun getManagerLaunchIntent(): Intent? {
        val pkg = getManagerPackage() ?: return null
        return context.packageManager.getLaunchIntentForPackage(pkg)
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun cleanup() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {}
    }

    companion object {
        const val PACKAGE_SHIZUKU_PLUS = "af.shizuku.plus.api"
        const val PACKAGE_SHIZUKU_STANDARD = "moe.shizuku.privileged.api"
    }
}
