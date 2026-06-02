package com.phrasevoice.data.tts

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.phrasevoice.debug.AppLogger

class AudioPlaybackController(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()

    fun play(uri: Uri) {
        AppLogger.i(TAG, "play uri=$uri")
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun stop() {
        AppLogger.i(TAG, "stop")
        player.stop()
        player.clearMediaItems()
    }

    companion object {
        private const val TAG = "AudioPlayback"
    }
}
