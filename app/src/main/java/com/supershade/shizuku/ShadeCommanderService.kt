package com.supershade.shizuku

class ShadeCommanderService : IShadeCommander.Stub() {

    override fun exec(cmd: Array<out String>): Boolean {
        val process = ProcessBuilder(*cmd).start()
        return try {
            process.waitFor() == 0
        } finally {
            process.inputStream.close()
            process.errorStream.close()
            process.outputStream.close()
            process.destroy()
        }
    }

    override fun execForOutput(cmd: Array<out String>): String {
        val process = ProcessBuilder(*cmd).start()
        return try {
            process.inputStream.bufferedReader().readText().trim()
        } finally {
            process.inputStream.close()
            process.errorStream.close()
            process.outputStream.close()
            process.destroy()
        }
    }
}
