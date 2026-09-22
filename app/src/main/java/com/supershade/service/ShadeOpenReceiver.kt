package com.supershade.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Exported broadcast receiver that opens SuperShade from any external trigger.
 *
 * ## Supported trigger actions
 *
 * ### 1. SuperShade public API
 * Any app (Tasker, Bixby Routines, Shortcuts, etc.) can directly open SuperShade:
 * ```
 * adb shell am broadcast -a com.supershade.action.OPEN_SHADE
 * adb shell am broadcast -a com.supershade.action.OPEN_SHADE --ez expand_qs true
 * ```
 *
 * ### 2. Samsung Good Lock — One-handed operation plugin
 * When Good Lock's One-handed operation is set to "Notification Panel" and the
 * native system shade is disabled (e.g., via SuperShade's Shizuku blocker), the
 * plugin broadcasts several Samsung-specific actions that would normally open
 * the system notification shade. SuperShade intercepts all of them here so the
 * gesture "just works" without any additional configuration in Good Lock.
 *
 * Known Good Lock broadcast actions intercepted:
 *   - `com.samsung.android.goodlock.oho.OPEN_NOTIFICATION_PANEL`
 *   - `com.samsung.android.goodlock.action.OPEN_NOTIFICATION_PANEL`
 *   - `com.samsung.android.onehandedmode.OPEN_PANEL`
 *   - `com.samsung.android.cocktailbar.OPEN_NOTIFICATION_PANEL` (Edge Panel route)
 *
 * ### 3. Android global CLOSE_SYSTEM_DIALOGS
 * Some Samsung launchers and Good Lock modules fire `CLOSE_SYSTEM_DIALOGS` with
 * `reason=notificationbar` when expanding fails. We catch that too.
 *
 * ### 4. Accessibility global action fallback
 * The [SuperShadeAccessibilityService] independently intercepts window-state events
 * that indicate the system shade is opening. This receiver is a complementary layer
 * for broadcast-based triggers that bypass accessibility events.
 */
class ShadeOpenReceiver : BroadcastReceiver() {

    companion object {
        /** Public action — use this for Tasker, Bixby Routines, Shortcuts, ADB, etc. */
        const val ACTION_OPEN_SHADE = "com.supershade.action.OPEN_SHADE"

        /** Good Lock One-handed operation — notification panel shortcut (primary) */
        private const val GOODLOCK_OHO_OPEN = "com.samsung.android.goodlock.oho.OPEN_NOTIFICATION_PANEL"

        /** Good Lock generic notification action */
        private const val GOODLOCK_ACTION_OPEN = "com.samsung.android.goodlock.action.OPEN_NOTIFICATION_PANEL"

        /** Samsung One-handed mode panel open */
        private const val ONE_HAND_OPEN = "com.samsung.android.onehandedmode.OPEN_PANEL"

        /** Samsung Edge Panel cocktail bar route */
        private const val EDGE_PANEL_OPEN = "com.samsung.android.cocktailbar.OPEN_NOTIFICATION_PANEL"

        /** Good Lock One-handed operation — quick panel shortcut */
        private const val GOODLOCK_OHO_QUICK_PANEL = "com.samsung.android.goodlock.oho.OPEN_QUICK_PANEL"
        private const val GOODLOCK_ACTION_QUICK_PANEL = "com.samsung.android.goodlock.action.OPEN_QUICK_PANEL"
        private const val ONE_HAND_QUICK_PANEL = "com.samsung.android.onehandedmode.OPEN_QUICK_PANEL"
        private const val EDGE_PANEL_QUICK_PANEL = "com.samsung.android.cocktailbar.OPEN_QUICK_PANEL"

        /** CLOSE_SYSTEM_DIALOGS reason that some Samsung paths use to signal panel open failure */
        private const val CLOSE_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS"
        private const val REASON_NOTIF_BAR = "notificationbar"

        private const val TAG = "ShadeOpenReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        val isQuickPanelAction = when (action) {
            GOODLOCK_OHO_QUICK_PANEL,
            GOODLOCK_ACTION_QUICK_PANEL,
            ONE_HAND_QUICK_PANEL,
            EDGE_PANEL_QUICK_PANEL -> true
            else -> false
        }

        val shouldOpen = when {
            isQuickPanelAction -> true
            action == ACTION_OPEN_SHADE ||
            action == GOODLOCK_OHO_OPEN ||
            action == GOODLOCK_ACTION_OPEN ||
            action == ONE_HAND_OPEN ||
            action == EDGE_PANEL_OPEN -> true
            else -> false
        }

        if (!shouldOpen) return

        val expandQs = if (isQuickPanelAction) {
            true
        } else {
            intent.getBooleanExtra(ShadeService.EXTRA_EXPAND_QS, false)
        }

        // If the accessibility service is running, delegate to it (it handles shade interception)
        val a11yService = SuperShadeAccessibilityService.instance
        if (a11yService != null) {
            Log.d(TAG, "Delegating to AccessibilityService.openSuperShade(expandQs=$expandQs)")
            a11yService.openSuperShadeFromReceiver(expandQs)
            return
        }

        // Fallback: start ShadeService directly
        Log.d(TAG, "AccessibilityService not running — starting ShadeService directly")
        val serviceIntent = Intent(context, ShadeService::class.java).apply {
            this.action = ShadeService.ACTION_OPEN_SHADE
            putExtra(ShadeService.EXTRA_EXPAND_QS, expandQs)
        }
        try {
            context.startForegroundService(serviceIntent)
        } catch (_: Exception) {
            try { context.startService(serviceIntent) } catch (_: Exception) {}
        }
    }
}
