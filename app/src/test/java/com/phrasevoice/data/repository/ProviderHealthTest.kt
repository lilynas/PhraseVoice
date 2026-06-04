package com.phrasevoice.data.repository

import com.phrasevoice.data.model.ProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderHealthTest {
    @Test
    fun providerHealth_disabledProviderIsNotConfigured() {
        val config = ProviderConfig(
            providerId = ProviderConfigRepository.OPENAI,
            enabled = false,
            baseUrl = "https://api.openai.com/v1/audio/speech",
            encryptedValue = "saved",
        )

        assertEquals(ProviderHealthStatus.Disabled, providerHealthForConfig(config))
    }

    @Test
    fun providerHealth_openAiRequiresApiKey() {
        val config = ProviderConfig(
            providerId = ProviderConfigRepository.OPENAI,
            enabled = true,
            baseUrl = "https://api.openai.com/v1/audio/speech",
        )

        assertEquals(ProviderHealthStatus.MissingApiKey, providerHealthForConfig(config))
    }

    @Test
    fun providerHealth_customHttpRequiresBaseUrlButNotApiKey() {
        val missingUrl = ProviderConfig(
            providerId = ProviderConfigRepository.CUSTOM_HTTP,
            enabled = true,
        )
        val ready = missingUrl.copy(baseUrl = "https://example.com/tts")

        assertEquals(ProviderHealthStatus.MissingBaseUrl, providerHealthForConfig(missingUrl))
        assertEquals(ProviderHealthStatus.Ready, providerHealthForConfig(ready))
    }

    @Test
    fun providerHealth_edgeForwarderTokenIsOptional() {
        val config = ProviderConfig(
            providerId = ProviderConfigRepository.EDGE_TTS_FORWARDER,
            enabled = true,
            baseUrl = "https://tts.example.com/api/text-to-speech",
        )

        assertEquals(ProviderHealthStatus.Ready, providerHealthForConfig(config))
    }

    @Test
    fun providerHealth_androidReflectsSystemReadiness() {
        val config = ProviderConfig(
            providerId = ProviderConfigRepository.ANDROID_SYSTEM,
            enabled = true,
        )

        assertEquals(
            ProviderHealthStatus.SystemUnavailable,
            providerHealthForConfig(config, androidTtsReady = false),
        )
    }
}
