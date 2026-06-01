package com.phrasevoice.domain.tts

interface TtsProvider {
    val id: String
    val displayName: String
    val supportsDirectPlayback: Boolean
    val supportsFileOutput: Boolean

    suspend fun listVoices(): List<TtsVoice>
    suspend fun synthesize(request: TtsRequest): TtsResult
    fun stop()
}
