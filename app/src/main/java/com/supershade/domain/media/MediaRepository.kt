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
        try { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
    }

    // ---------------------------------------------------------------------------

    private fun trackBestController(controllers: List<MediaController>?) {
        val best = controllers?.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers?.firstOrNull()

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
        _media.value = if (meta == null) null else MediaState(
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "",
            albumArt = meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART),
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = controller.packageName ?: "",
            duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            position = controller.playbackState?.position ?: 0L,
        )
    }
}
