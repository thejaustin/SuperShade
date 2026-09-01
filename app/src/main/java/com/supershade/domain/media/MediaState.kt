package com.supershade.domain.media

import android.graphics.Bitmap

data class MediaState(
    val title: String,
    val artist: String,
    val albumArt: Bitmap?,
    val isPlaying: Boolean,
    val packageName: String,
    val duration: Long = 0L,
    val position: Long = 0L
)
