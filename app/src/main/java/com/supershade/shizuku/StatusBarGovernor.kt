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
import kotlinx.coroutines.launch
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
    val canRunPrivileged: Boolean get() = _isCommanderConnected.value || connector.hasPermission()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val serviceArgs: Shizuku.UserServiceArgs get() {
        val ver = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) { 1 }
        return Shizuku.UserServiceArgs(
            ComponentName(context.packageName, ShadeCommanderService::class.java.name)
        ).daemon(false).processNameSuffix("commander").debuggable(false).version(ver)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            android.util.Log.i("StatusBarGovernor", "ShadeCommanderService bound successfully via Shizuku!")
            commander = IShadeCommander.Stub.asInterface(service)
            _isCommanderConnected.value = true
            if (shouldDisableExpansion) {
                serviceScope.launch {
                    disableExpansion()
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            android.util.Log.w("StatusBarGovernor", "ShadeCommanderService disconnected")
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
        if (!connector.hasPermission()) {
            android.util.Log.d("StatusBarGovernor", "bindService skipped: Shizuku permission not granted")
            return
        }
        try {
            android.util.Log.i("StatusBarGovernor", "Binding Shizuku UserService...")
            Shizuku.bindUserService(serviceArgs, serviceConnection)
        } catch (e: Exception) {
            android.util.Log.e("StatusBarGovernor", "bindUserService failed", e)
        }
    }

    fun unbindService() {
        try {
            Shizuku.unbindUserService(serviceArgs, serviceConnection, false)
        } catch (e: Exception) {}
        commander = null
        _isCommanderConnected.value = false
    }

    private fun executeShizukuProcess(cmd: Array<String>): java.lang.Process? {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            method.invoke(null, cmd, null, null) as? java.lang.Process
        } catch (e: Exception) {
            null
        }
    }

    suspend fun runShell(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext false
            val cmd = Array(args.size) { args[it] }
            val direct = commander?.exec(cmd)
            if (direct != null) return@withContext direct
            val proc = executeShizukuProcess(cmd)
            proc?.waitFor() == 0
        } catch (e: Exception) { false }
    }

    suspend fun runShellOutput(vararg args: String): String = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext ""
            val cmd = Array(args.size) { args[it] }
            val direct = commander?.execForOutput(cmd)
            if (!direct.isNullOrEmpty()) return@withContext direct
            val proc = executeShizukuProcess(cmd)
            val output = proc?.inputStream?.bufferedReader()?.use { it.readText() }?.trim().orEmpty()
            proc?.waitFor()
            output
        } catch (e: Exception) { "" }
    }

    fun runShellBlocking(vararg args: String): Boolean {
        return try {
            if (!connector.hasPermission()) return false
            val cmd = Array(args.size) { args[it] }
            val direct = commander?.exec(cmd)
            if (direct != null) return direct
            val proc = executeShizukuProcess(cmd)
            proc?.waitFor() == 0
        } catch (e: Exception) { false }
    }

    suspend fun disableExpansion(): Boolean {
        shouldDisableExpansion = true
        return runShell("cmd", "statusbar", "send-disable-flag", "statusbar-expansion")
    }

    /**
     * Completely restores the native system status bar and all elements (clock, icons,
     * Good Lock QuickStar battery bar, notification icons, and expansion).
     */
    suspend fun restoreSystemStatusBar(): Boolean = withContext(Dispatchers.IO) {
        shouldDisableExpansion = false
        // 1. Reset all disable flags to none (clears any lingering expansion/system-icons flags)
        val res1 = runShell("cmd", "statusbar", "send-disable-flag", "none")
        // 2. Collapse any stuck panels
        val res2 = runShell("cmd", "statusbar", "collapse")
        // 3. Notify SystemUI & Good Lock to restore custom bars / layout
        runShell("am", "broadcast", "-a", "android.intent.action.CLOSE_SYSTEM_DIALOGS")
        res1 || res2
    }

    fun restoreSystemStatusBarBlocking(): Boolean {
        shouldDisableExpansion = false
        val res1 = runShellBlocking("cmd", "statusbar", "send-disable-flag", "none")
        val res2 = runShellBlocking("cmd", "statusbar", "collapse")
        runShellBlocking("am", "broadcast", "-a", "android.intent.action.CLOSE_SYSTEM_DIALOGS")
        return res1 || res2
    }

    suspend fun enableExpansion(): Boolean = restoreSystemStatusBar()

    fun enableExpansionBlocking(): Boolean = restoreSystemStatusBarBlocking()

    suspend fun clickTile(component: String): Boolean =
        runShell("cmd", "statusbar", "click-tile", component)

    suspend fun getCurrentTiles(): String =
        runShellOutput("settings", "get", "secure", "sysui_qs_tiles")

    suspend fun collapse(): Boolean =
        runShell("cmd", "statusbar", "collapse")

    suspend fun expandSettings(): Boolean =
        runShell("cmd", "statusbar", "expand-settings")
}

