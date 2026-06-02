package com.phrasevoice.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.local.PhraseVoicePreferenceKeys
import com.phrasevoice.data.local.safeData
import com.phrasevoice.data.model.CustomHttpSettings
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.security.ApiKeyCipher
import com.phrasevoice.data.tts.EdgeForwarderCatalog
import com.phrasevoice.data.tts.GeminiTtsCatalog
import com.phrasevoice.debug.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class RuntimeProviderConfig(
    val config: ProviderConfig,
    val apiKey: String?,
)

class ProviderConfigRepository(
    private val dataStore: DataStore<Preferences>,
    private val apiKeyCipher: ApiKeyCipher,
) {
    val configs: Flow<List<ProviderConfig>> = dataStore.safeData()
        .map { preferences ->
            normalizeConfigs(
                PhraseVoiceJson.decode(
                    preferences[PhraseVoicePreferenceKeys.PROVIDER_CONFIGS],
                    defaultConfigs(),
                ),
            )
        }

    suspend fun getConfig(providerId: String): ProviderConfig =
        configs.first().firstOrNull { it.providerId == providerId }
            ?: defaultConfigs().first { it.providerId == providerId }

    suspend fun getRuntimeConfig(providerId: String): RuntimeProviderConfig {
        val config = getConfig(providerId)
        AppLogger.i(
            TAG,
            "runtimeConfig provider=$providerId enabled=${config.enabled} baseUrlSet=${!config.baseUrl.isNullOrBlank()} model=${config.model.orEmpty()} voice=${config.defaultVoice.orEmpty()} apiKeySaved=${!config.encryptedValue.isNullOrBlank()}",
        )
        return RuntimeProviderConfig(
            config = config,
            apiKey = apiKeyCipher.decrypt(config.encryptedValue),
        )
    }

    suspend fun saveConfig(
        providerId: String,
        enabled: Boolean,
        apiKeyPlainText: String?,
        baseUrl: String?,
        model: String?,
        defaultVoice: String?,
        extraJson: String?,
    ) {
        dataStore.edit { preferences ->
            val current = normalizeConfigs(
                PhraseVoiceJson.decode(
                    preferences[PhraseVoicePreferenceKeys.PROVIDER_CONFIGS],
                    defaultConfigs(),
                ),
            )
            val existing = current.firstOrNull { it.providerId == providerId }
                ?: defaultConfigs().first { it.providerId == providerId }
            val encryptedValue = apiKeyPlainText
                ?.takeIf { it.isNotBlank() }
                ?.let(apiKeyCipher::encrypt)
                ?: existing.encryptedValue
            val nextConfig = existing.copy(
                enabled = enabled,
                apiKeyAlias = if (encryptedValue.isNullOrBlank()) null else "saved",
                encryptedValue = encryptedValue,
                baseUrl = baseUrl?.trim()?.takeIf { it.isNotEmpty() },
                model = model?.trim()?.takeIf { it.isNotEmpty() },
                defaultVoice = defaultVoice?.trim()?.takeIf { it.isNotEmpty() },
                extraJson = extraJson,
            )
            val next = current.map { if (it.providerId == providerId) nextConfig else it }
            preferences[PhraseVoicePreferenceKeys.PROVIDER_CONFIGS] = PhraseVoiceJson.encode(next)
        }
    }

    private fun normalizeConfigs(configs: List<ProviderConfig>): List<ProviderConfig> {
        val byId = configs.associateBy { it.providerId }
        return defaultConfigs().map { default ->
            val stored = byId[default.providerId] ?: return@map default
            default.copy(
                enabled = stored.enabled,
                apiKeyAlias = stored.apiKeyAlias,
                encryptedValue = stored.encryptedValue,
                baseUrl = stored.baseUrl ?: default.baseUrl,
                model = stored.model ?: default.model,
                defaultVoice = stored.defaultVoice ?: default.defaultVoice,
                extraJson = stored.extraJson ?: default.extraJson,
            )
        }
    }

    companion object {
        private const val TAG = "ProviderConfigRepo"

        const val ANDROID_SYSTEM = "android_system"
        const val OPENAI = "openai"
        const val EDGE_TTS_FORWARDER = "edge_tts_forwarder"
        const val GEMINI = "gemini"
        const val MIMO = "mimo"
        const val CUSTOM_HTTP = "custom_http"

        fun defaultConfigs(): List<ProviderConfig> =
            listOf(
                ProviderConfig(
                    providerId = ANDROID_SYSTEM,
                    enabled = true,
                ),
                ProviderConfig(
                    providerId = OPENAI,
                    enabled = false,
                    baseUrl = "https://api.openai.com/v1/audio/speech",
                    model = "gpt-4o-mini-tts",
                    defaultVoice = "alloy",
                ),
                ProviderConfig(
                    providerId = EDGE_TTS_FORWARDER,
                    enabled = false,
                    baseUrl = "https://tts.shirone.de/api/text-to-speech",
                    defaultVoice = EdgeForwarderCatalog.DEFAULT_VOICE_ID,
                ),
                ProviderConfig(
                    providerId = GEMINI,
                    enabled = false,
                    baseUrl = "https://generativelanguage.googleapis.com/v1beta/models",
                    model = "gemini-3.1-flash-tts-preview",
                    defaultVoice = GeminiTtsCatalog.DEFAULT_VOICE_ID,
                ),
                ProviderConfig(
                    providerId = MIMO,
                    enabled = false,
                    baseUrl = "https://api.xiaomimimo.com/v1/chat/completions",
                    model = "mimo-v2.5-tts",
                    defaultVoice = "mimo_default",
                ),
                ProviderConfig(
                    providerId = CUSTOM_HTTP,
                    enabled = false,
                    baseUrl = "",
                    defaultVoice = "default",
                    extraJson = PhraseVoiceJson.encode(CustomHttpSettings()),
                ),
            )
    }
}
