package com.supershade.shizuku

class ShadeCommanderService : IShadeCommander.Stub() {

    override fun exec(cmd: Array<out String>): Boolean = try {
        Runtime.getRuntime().exec(cmd).waitFor() == 0
    } catch (_: Exception) { false }

    override fun execForOutput(cmd: Array<out String>): String = try {
        Runtime.getRuntime().exec(cmd).inputStream.bufferedReader().readText().trim()
    } catch (_: Exception) { "" }
}
