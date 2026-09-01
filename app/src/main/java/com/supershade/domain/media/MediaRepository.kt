package com.supershade.domain.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.supershade.service.NotificationCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaRepository(private val context: Context) {

    private val _media = MutableStateFlow<MediaState?>(null)
    val media: StateFlow<MediaState?> = _media.asStateFlow()

    private val sessionManager: MediaSessionManager =
        context.getSystemService(MediaSessionManager::class.java)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeController: MediaController? = null

    // Bug 1: track whether the listener was successfully registered
    private var listenerRegistered = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = updateFromActive()
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateFromActive()
        override fun onSessionDestroyed() {
            activeController?.unregisterCallback(this)
            activeController = null
            _media.value = null
            refresh()
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        trackBestController(controllers)
    }

    init {
        try {
            val component = ComponentName(context, NotificationCollector::class.java)
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, component, mainHandler)
            // Bug 1: only set to true after the call succeeds
            listenerRegistered = true
            refresh()
        } catch (_: SecurityException) {}
    }

    fun refresh() {
        try {
            val component = ComponentName(context, NotificationCollector::class.java)
            trackBestController(sessionManager.getActiveSessions(component))
        } catch (_: SecurityException) {
            _media.value = null
        }
    }

    fun play() { activeController?.transportControls?.play() }
    fun pause() { activeController?.transportControls?.pause() }
    fun skipNext() { activeController?.transportControls?.skipToNext() }
    fun skipPrevious() { activeController?.transportControls?.skipToPrevious() }

    fun dispose() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
        // Bug 1: guard remove call so it only runs when the listener was registered
        if (listenerRegistered) {
            try { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
            listenerRegistered = false
        }
        // Bug 3: recycle old album art before nulling the flow
        _media.value?.albumArt?.takeIf { !it.isRecycled }?.recycle()
    }

    // ---------------------------------------------------------------------------

    private fun trackBestController(controllers: List<MediaController>?) {
        val best = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

        // Bug 2: skip emission entirely when both sides are already null
        if (best == null && _media.value == null) return

        if (best?.sessionToken != activeController?.sessionToken) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = best
            activeController?.registerCallback(controllerCallback, mainHandler)
        }
        updateFromController(best)
    }

    private fun updateFromActive() = updateFromController(activeController)

    private fun updateFromController(controller: MediaController?) {
        val meta = controller?.metadata
        val newArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        // Bug 3: recycle the old Bitmap if it's a different instance
        val oldArt = _media.value?.albumArt
        if (oldArt != null && oldArt !== newArt && !oldArt.isRecycled) {
            oldArt.recycle()
        }
        _media.value = if (meta == null) null else MediaState(
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            albumArt = newArt,
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName ?: "",
            duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            position = controller.playbackState?.position ?: 0L,
        )
    }
}
