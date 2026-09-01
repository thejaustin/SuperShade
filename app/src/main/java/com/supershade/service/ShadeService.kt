package com.supershade.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.supershade.overlay.GestureOverlay
import com.supershade.overlay.ShadeWindowManager
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Foreground service that is the backbone of SuperShade.
 *
 * Lifecycle:
 *   onCreate  → attaches the gesture overlay (1 px strip below status bar),
 *               calls [StatusBarGovernor.disableExpansion] so the system shade
 *               cannot be pulled down, then calls [ShadeWindowManager.show] in
 *               response to swipe-down gestures detected by [GestureOverlay].
 *   onDestroy → detaches the gesture overlay, hides any open shade window,
 *               re-enables system expansion.
 */
class ShadeService : Service() {

    private val governor: StatusBarGovernor by inject()

    // ShadeViewModel is a Koin singleton — resolved here so the service and the
    // ComposeView overlay share the exact same instance.
    private val shadeViewModel: ShadeViewModel by inject()

    /** Supervisor scope kept alive for the lifetime of the service. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: ShadeWindowManager
    private var gestureOverlay: GestureOverlay? = null

    companion object {
        const val CHANNEL_ID = "supershade_service"
        const val NOTIFICATION_ID = 1001
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        windowManager = ShadeWindowManager(applicationContext, shadeViewModel)

        // Attach the 1-px gesture capture strip. When a swipe-down is detected
        // the overlay tells the ShadeWindowManager to show the full shade UI.
        gestureOverlay = GestureOverlay(this) { windowManager.show() }
        gestureOverlay?.attach()

        // Disable the system notification shade so our overlay takes over.
        scope.launch { governor.disableExpansion() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        gestureOverlay?.detach()
        gestureOverlay = null
        windowManager.hide()
        // Re-enable the system shade before we fully shut down, then cancel scope.
        scope.launch { governor.enableExpansion() }.invokeOnCompletion { scope.cancel() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ---------------------------------------------------------------------------
    // Notification channel + foreground notification
    // ---------------------------------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SuperShade",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "SuperShade notification shade service"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("SuperShade active")
            .setContentText("Swipe down to open the shade")
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setOngoing(true)
            .build()
}
