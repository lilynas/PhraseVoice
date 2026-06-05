package com.phrasevoice.domain.tts

import com.phrasevoice.domain.model.AudioFormat

data class TtsRequest(
    val text: String,
    val providerId: String,
    val voiceId: String?,
    val language: String?,
    val speed: Float,
    val pitch: Float,
    val volume: Float,
    val stylePrompt: String? = null,
    val outputFormat: AudioFormat = AudioFormat.MP3,
    val mimoOptimizeTextPreview: Boolean = false,
)
