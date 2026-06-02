package com.phrasevoice.data.tts

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class AudioPlaybackController(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()

    fun play(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
    }
}
