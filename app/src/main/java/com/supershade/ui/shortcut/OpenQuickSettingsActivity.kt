package com.supershade.ui.shortcut

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.supershade.service.ShadeService
import com.supershade.service.SuperShadeAccessibilityService

/**
 * Transparent, zero-latency trampoline activity that immediately presents SuperShade's
 * Quick Settings Panel.
 *
 * Configured as an app shortcut and main activity target for Samsung Good Lock One Hand
 * Operation+, Bixby Routines, Nova Launcher, Tasker, and edge panels.
 */
class OpenQuickSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val a11y = SuperShadeAccessibilityService.instance
        if (a11y != null) {
            a11y.openSuperShadeFromReceiver(expandQs = true)
        } else {
            val intent = Intent(this, ShadeService::class.java).apply {
                action = ShadeService.ACTION_OPEN_SHADE
                putExtra(ShadeService.EXTRA_EXPAND_QS, true)
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (_: Exception) {}
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
