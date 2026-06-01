package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProviderConfig(
    val providerId: String,
    val enabled: Boolean,
    val apiKeyAlias: String? = null,
    val encryptedValue: String? = null,
    val baseUrl: String? = null,
    val model: String? = null,
    val defaultVoice: String? = null,
    val extraJson: String? = null,
)
