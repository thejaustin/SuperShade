package com.supershade.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class StatusBarGovernor(
    private val context: Context,
    private val connector: ShizukuPlusConnector,
) {

    @Volatile private var commander: IShadeCommander? = null
    @Volatile private var shouldDisableExpansion = false

    private val _isCommanderConnected = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isCommanderConnected: kotlinx.coroutines.flow.StateFlow<Boolean> = _isCommanderConnected

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val serviceArgs: Shizuku.UserServiceArgs get() = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, ShadeCommanderService::class.java.name)
    ).daemon(false).processNameSuffix("commander").debuggable(false).version(1)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            commander = IShadeCommander.Stub.asInterface(service)
            _isCommanderConnected.value = true
            if (shouldDisableExpansion) {
                serviceScope.launch {
                    disableExpansion()
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            commander = null
            _isCommanderConnected.value = false
        }
    }

    init {
        connector.isConnected
            .onEach { connected -> if (connected) bindService() }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Main))
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
        _isCommanderConnected.value = false
    }

    suspend fun runShell(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext false
            val cmd = Array(args.size) { args[it] }
            commander?.exec(cmd) ?: false
        } catch (_: Exception) { false }
    }

    suspend fun runShellOutput(vararg args: String): String = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext ""
            val cmd = Array(args.size) { args[it] }
            commander?.execForOutput(cmd) ?: ""
        } catch (_: Exception) { "" }
    }

    suspend fun disableExpansion(): Boolean {
        shouldDisableExpansion = true
        return runShell("cmd", "statusbar", "send-disable-flag", "statusbar-expansion")
    }

    suspend fun enableExpansion(): Boolean {
        shouldDisableExpansion = false
        return runShell("cmd", "statusbar", "send-disable-flag", "none")
    }

    suspend fun clickTile(component: String): Boolean =
        runShell("cmd", "statusbar", "click-tile", component)

    suspend fun getCurrentTiles(): String =
        runShellOutput("settings", "get", "secure", "sysui_qs_tiles")

    suspend fun collapse(): Boolean =
        runShell("cmd", "statusbar", "collapse")

    suspend fun expandSettings(): Boolean =
        runShell("cmd", "statusbar", "expand-settings")
}
