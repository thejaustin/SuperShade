package com.supershade.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.supershade.R
import com.supershade.overlay.GestureOverlay
import com.supershade.overlay.ShadeWindowManager
import com.supershade.settings.ShadeSettings
import com.supershade.shizuku.ShizukuPlusConnector
import com.supershade.shizuku.StatusBarGovernor
import com.supershade.viewmodel.ShadeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
    private val connector: ShizukuPlusConnector by inject()
    private val settings: ShadeSettings by inject()
    private val notificationRepo: com.supershade.domain.notification.NotificationRepository by inject()

    // ShadeViewModel is a Koin singleton — resolved here so the service and the
    // ComposeView overlay share the exact same instance.
    private val shadeViewModel: ShadeViewModel by inject()

    /** Supervisor scope kept alive for the lifetime of the service. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val windowManager: ShadeWindowManager by inject()
    private val headsUpOverlay: HeadsUpOverlay by inject()
    private var gestureOverlay: GestureOverlay? = null

    companion object {
        const val CHANNEL_ID = "supershade_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_OPEN_SHADE = "com.supershade.action.OPEN_SHADE"
        const val EXTRA_EXPAND_QS = "expand_qs"
    }

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        var currentSplitMode = com.supershade.settings.SplitGestureMode.SEPARATE_70_30
        settings.splitGestureMode
            .onEach { currentSplitMode = it }
            .launchIn(scope)

        // Attach the gesture capture overlay. When a downward swipe is detected
        // the overlay tells the ShadeWindowManager to show the full shade UI.
        gestureOverlay = GestureOverlay(
            context = this,
            isShadeOpen = { shadeViewModel.state.value.isOpen },
            splitGestureMode = { currentSplitMode },
            onSwipeDown = { expandQs -> shadeViewModel.open(expandQs) },
        )
        gestureOverlay?.attach()

        // Show peek cards for new notifications when the shade panel is closed.
        notificationRepo.newNotifications
            .onEach { notification ->
                if (!shadeViewModel.state.value.isOpen) {
                    headsUpOverlay.show(notification)
                }
            }
            .launchIn(scope)

        // Observe isOpen state from ViewModel so opening shade from any component
        // presents the overlay window.
        shadeViewModel.state
            .map { it.isOpen }
            .distinctUntilChanged()
            .onEach { isOpen ->
                if (isOpen) windowManager.show() else windowManager.hide()
            }
            .launchIn(scope)

        // Start the Shizuku UserService so tile-click and status-bar commands work.
        governor.bindService()

        // Stop foreground service whenever SuperShade is disabled.
        settings.isActive
            .distinctUntilChanged()
            .onEach { active ->
                if (!active) {
                    stopSelf()
                }
            }
            .launchIn(scope)

        // Apply or lift the system-shade block whenever the setting changes.
        settings.blockSystemShade
            .distinctUntilChanged()
            .onEach { block ->
                if (block) governor.disableExpansion() else governor.enableExpansion()
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPEN_SHADE) {
            val expandQs = intent.getBooleanExtra(EXTRA_EXPAND_QS, false)
            shadeViewModel.open(expandQs)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureOverlay?.detach()
        gestureOverlay = null
        headsUpOverlay.destroy()
        windowManager.hide()

        // Re-enable the native system status bar synchronously before unbinding service.
        governor.enableExpansionBlocking()

        // Tear down the Shizuku UserService and clean up binder listeners.
        governor.unbindService()
        connector.cleanup()
        scope.cancel()
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
            .setSmallIcon(R.drawable.ic_notification_shade)
            .setOngoing(true)
            .build()
}
