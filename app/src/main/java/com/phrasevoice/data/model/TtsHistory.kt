package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TtsHistory(
    val id: String,
    val text: String,
    val providerId: String,
    val voiceId: String?,
    val createdAt: Long,
    val audioUri: String? = null,
)
