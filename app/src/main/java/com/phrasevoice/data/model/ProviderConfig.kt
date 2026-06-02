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

@Serializable
data class CustomHttpSettings(
    val method: String = "POST",
    val headersTemplate: String = "Authorization: Bearer {{apiKey}}\nContent-Type: application/json",
    val bodyTemplate: String = """
        {"input":"{{text}}","voice":"{{voice}}","speed":{{speed}},"format":"{{format}}"}
    """.trimIndent(),
    val responseType: CustomHttpResponseType = CustomHttpResponseType.RAW_AUDIO,
    val responseField: String = "audio",
)

@Serializable
enum class CustomHttpResponseType {
    RAW_AUDIO,
    JSON_BASE64_FIELD,
    JSON_URL_FIELD,
}
