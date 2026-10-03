package com.supershade.domain.audio

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class VolumeState(
    val current: Int,
    val max: Int,
    val isMuted: Boolean,
)

/**
 * Reactive audio and ringer mode repository.
 * Emits instant updates on hardware key volume events, mute changes, and ringer mode transitions,
 * eliminating high-frequency UI polling loops.
 */
class AudioRepository(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    val musicVolume: Flow<VolumeState> = callbackFlow {
        fun emitCurrent() {
            try {
                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                trySend(VolumeState(current = cur, max = max, isMuted = cur == 0))
            } catch (_: Exception) {}
        }

        emitCurrent()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                emitCurrent()
            }
        }

        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction("android.media.STREAM_MUTE_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioRepo", "Failed to register volume receiver", e)
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }.conflate()

    val ringerMode: Flow<Int> = callbackFlow {
        fun emitCurrent() {
            trySend(audioManager.ringerMode)
        }

        emitCurrent()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                emitCurrent()
            }
        }

        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioRepo", "Failed to register ringer receiver", e)
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }.conflate()

    fun setMusicVolume(volume: Int) {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        } catch (_: Exception) {}
    }

    /**
     * Cycles through ringer modes:
     * Normal -> Vibrate -> Silent -> Normal
     *
     * If DND policy access is not granted, gracefully cycles between Normal and Vibrate
     * without throwing SecurityException or falling back to opening settings.
     */
    fun cycleRingerMode(): Int {
        val current = audioManager.ringerMode
        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true

        val next = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> {
                if (hasDndAccess) AudioManager.RINGER_MODE_SILENT
                else AudioManager.RINGER_MODE_NORMAL
            }
            AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        try {
            audioManager.ringerMode = next
        } catch (_: Exception) {
            // If setting silent fails due to system restriction, fallback to vibrate or normal
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            } catch (_: Exception) {}
        }
        return audioManager.ringerMode
    }

    fun setRingerMode(mode: Int): Int {
        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true
        val targetMode = if (mode == AudioManager.RINGER_MODE_SILENT && !hasDndAccess) {
            AudioManager.RINGER_MODE_VIBRATE
        } else {
            mode
        }
        try {
            audioManager.ringerMode = targetMode
        } catch (_: Exception) {
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            } catch (_: Exception) {}
        }
        return audioManager.ringerMode
    }

    fun getCurrentRingerMode(): Int = audioManager.ringerMode

    fun getStreamVolume(stream: Int): Int = try {
        audioManager.getStreamVolume(stream)
    } catch (_: Exception) {
        0
    }

    fun getStreamMaxVolume(stream: Int): Int = try {
        audioManager.getStreamMaxVolume(stream)
    } catch (_: Exception) {
        15
    }

    fun setStreamVolume(stream: Int, volume: Int) {
        try {
            audioManager.setStreamVolume(stream, volume, 0)
        } catch (_: Exception) {}
    }
}
