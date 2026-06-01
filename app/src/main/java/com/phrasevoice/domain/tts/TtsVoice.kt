package com.phrasevoice.domain.tts

data class TtsVoice(
    val id: String,
    val name: String,
    val language: String?,
    val gender: String? = null,
    val description: String? = null,
    val providerId: String,
)
