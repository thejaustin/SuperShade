package com.supershade.shizuku

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class StatusBarGovernor(private val connector: ShizukuPlusConnector) {

    private suspend fun exec(vararg args: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext false
            Shizuku.newProcess(args, null, null).waitFor() == 0
        } catch (_: Exception) { false }
    }

    private suspend fun execOutput(vararg args: String): String = withContext(Dispatchers.IO) {
        try {
            if (!connector.hasPermission()) return@withContext ""
            Shizuku.newProcess(args, null, null).inputStream.bufferedReader().readText().trim()
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
