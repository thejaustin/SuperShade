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
            .onEach { connected ->
                if (connected && connector.hasPermission()) {
                    bindService()
                    if (shouldDisableExpansion) disableExpansion()
                }
            }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Main))

        connector.hasPermissionFlow
            .onEach { granted ->
                if (granted && connector.isConnected.value) {
                    bindService()
                    if (shouldDisableExpansion) disableExpansion()
                }
            }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Main))
    }

    fun bindService() {
        if (!connector.hasPermission()) {
            android.util.Log.d("StatusBarGovernor", "bindService skipped: Shizuku permission not granted")
            return
        }
        if (_isCommanderConnected.value && commander != null) return
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

    // --- Direct Settings Control via Shizuku ---

    suspend fun putSetting(table: String, key: String, value: String): Boolean =
        runShell("settings", "put", table, key, value)

    suspend fun getSetting(table: String, key: String): String =
        runShellOutput("settings", "get", table, key)

    // --- Privileged Hardware & System Toggles ---

    suspend fun setWifi(enabled: Boolean): Boolean =
        runShell("svc", "wifi", if (enabled) "enable" else "disable")

    suspend fun setData(enabled: Boolean): Boolean =
        runShell("svc", "data", if (enabled) "enable" else "disable")

    suspend fun setBluetooth(enabled: Boolean): Boolean =
        runShell("svc", "bluetooth", if (enabled) "enable" else "disable")

    suspend fun setNfc(enabled: Boolean): Boolean =
        runShell("svc", "nfc", if (enabled) "enable" else "disable")

    suspend fun setAirplaneMode(enabled: Boolean): Boolean =
        runShell("cmd", "connectivity", "airplane-mode", if (enabled) "enable" else "disable")

    suspend fun setLocationEnabled(enabled: Boolean): Boolean =
        runShell("cmd", "location", "set-location-enabled", if (enabled) "true" else "false")

    suspend fun setUiModeNight(night: Boolean): Boolean =
        runShell("cmd", "uimode", "night", if (night) "yes" else "no")

    suspend fun setDnd(enabled: Boolean): Boolean =
        runShell("cmd", "notification", "set_dnd", if (enabled) "on" else "off")

    suspend fun setBatterySaver(enabled: Boolean): Boolean =
        runShell("cmd", "power", "set-mode", if (enabled) "1" else "0")

    suspend fun setHotspot(enabled: Boolean): Boolean =
        runShell("cmd", "connectivity", "tether", if (enabled) "start-tethering" else "stop-tethering")

    suspend fun setAutoRotate(enabled: Boolean): Boolean =
        putSetting("system", "accelerometer_rotation", if (enabled) "1" else "0")

    suspend fun setExtraDim(enabled: Boolean): Boolean =
        putSetting("secure", "reduce_bright_colors_activated", if (enabled) "1" else "0")

    suspend fun setAlwaysOnDisplay(enabled: Boolean): Boolean {
        val s1 = putSetting("secure", "aod_mode", if (enabled) "1" else "0")
        val s2 = putSetting("secure", "doze_always_on", if (enabled) "1" else "0")
        return s1 || s2
    }

    // --- Privileged Power & System Actions ---

    suspend fun restartSystemUI(): Boolean =
        runShell("pkill", "-f", "com.android.systemui")

    suspend fun reboot(reason: String? = null): Boolean = withContext(Dispatchers.IO) {
        when (reason?.lowercase()) {
            "recovery" -> {
                runShell("cmd", "power", "reboot", "recovery") ||
                runShell("setprop", "sys.powerctl", "reboot,recovery")
            }
            "bootloader", "download" -> {
                runShell("cmd", "power", "reboot", "bootloader") ||
                runShell("reboot", "bootloader") ||
                runShell("reboot", "download")
            }
            "systemui", "soft" -> {
                restartSystemUI()
            }
            else -> {
                runShell("svc", "power", "reboot") ||
                runShell("cmd", "power", "reboot") ||
                runShell("reboot")
            }
        }
    }

    suspend fun shutdown(): Boolean = withContext(Dispatchers.IO) {
        runShell("svc", "power", "shutdown") ||
        runShell("cmd", "power", "shutdown") ||
        runShell("reboot", "-p")
    }

    suspend fun lockScreen(): Boolean =
        runShell("input", "keyevent", "26")

    suspend fun takeScreenshot(): Boolean =
        runShell("input", "keyevent", "120")
}
