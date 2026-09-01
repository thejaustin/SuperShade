package com.supershade.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class StatusBarGovernor(
    private val context: Context,
    private val connector: ShizukuPlusConnector,
) {

    private var commander: IShadeCommander? = null

    private val serviceArgs: Shizuku.UserServiceArgs get() = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShadeCommanderService::class.java.name)
    ).daemon(false).processNameSuffix("commander").debuggable(false).version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            commander = IShadeCommander.Stub.asInterface(service)
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            commander = null
        }
    }

    fun bindService() {
        if (!connector.hasPermission()) return
        try {
            Shizuku.bindUserService(serviceArgs, serviceConnection)
        } catch (_: Exception) {}
    }

    fun unbindService() {
        try {
            Shizuku.unbindUserService(serviceArgs, serviceConnection, false)
        } catch (_: Exception) {}
        commander = null
    }

    private suspend fun exec(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext false
            val cmd = Array(args.size) { args[it] }
            commander?.exec(cmd) ?: false
        } catch (_: Exception) { false }
    }

    private suspend fun execOutput(vararg args: String): String = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext ""
            val cmd = Array(args.size) { args[it] }
            commander?.execForOutput(cmd) ?: ""
        } catch (_: Exception) { "" }
    }

    suspend fun disableExpansion(): Boolean =
        exec("cmd", "statusbar", "send-disable-flag", "statusbar-expansion")

    suspend fun enableExpansion(): Boolean =
        exec("cmd", "statusbar", "send-disable-flag", "none")

    suspend fun clickTile(component: String): Boolean =
        exec("cmd", "statusbar", "click-tile", component)

    suspend fun getCurrentTiles(): String =
        execOutput("settings", "get", "secure", "sysui_qs_tiles")

    suspend fun collapse(): Boolean =
        exec("cmd", "statusbar", "collapse")

    suspend fun expandSettings(): Boolean =
        exec("cmd", "statusbar", "expand-settings")
}
