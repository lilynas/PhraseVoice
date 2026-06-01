package com.phrasevoice.domain.tts

import android.net.Uri

sealed class TtsResult {
    data class LocalPlaybackStarted(val utteranceId: String) : TtsResult()
    data class AudioFile(val uri: Uri, val mimeType: String) : TtsResult()
    data class Error(val message: String, val cause: Throwable? = null) : TtsResult()
}
