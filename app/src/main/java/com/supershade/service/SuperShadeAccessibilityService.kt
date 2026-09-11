package com.supershade.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.viewmodel.ShadeViewModel
import org.koin.android.ext.android.inject

/**
 * Optional zero-ADB, zero-Shizuku accessibility service that intercepts
 * the system status bar expansion and smoothly presents SuperShade.
 */
class SuperShadeAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: SuperShadeAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }

    private val connector: ShizukuPlusConnector by inject()
    private val shadeViewModel: ShadeViewModel by inject()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg == "com.android.systemui") {
                val cls = event.className?.toString().orEmpty()
                // Check if SystemUI opened the notification panel or quick settings
                if (cls.contains("Notification", ignoreCase = true) ||
                    cls.contains("Shade", ignoreCase = true) ||
                    cls.contains("Panel", ignoreCase = true)
                ) {
                    // Only intercept if Shizuku is not actively running (Shizuku already blocks it at system level)
                    if (!connector.isConnected.value || !connector.hasPermission()) {
                        dismissSystemShade()
                        // Ensure foreground ShadeService is running and show shade
                        val intent = Intent(this, ShadeService::class.java).apply {
                            action = ShadeService.ACTION_OPEN_SHADE
                        }
                        try {
                            startForegroundService(intent)
                        } catch (_: Exception) {
                            try { startService(intent) } catch (_: Exception) {}
                        }
                        shadeViewModel.open()
                    }
                }
            }
        }
    }

    fun dismissSystemShade(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Official API 31+ action to collapse the system notification shade
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            } else {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
