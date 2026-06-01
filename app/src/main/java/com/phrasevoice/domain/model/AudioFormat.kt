package com.phrasevoice.domain.model

enum class AudioFormat(
    val extension: String,
    val mimeType: String,
) {
    MP3("mp3", "audio/mpeg"),
    WAV("wav", "audio/wav"),
    M4A("m4a", "audio/mp4"),
}
