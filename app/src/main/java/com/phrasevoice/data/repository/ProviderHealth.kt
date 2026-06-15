package com.phrasevoice.data.repository

import com.phrasevoice.data.model.ProviderConfig

enum class ProviderHealthStatus {
    Ready,
    Disabled,
    MissingApiKey,
    MissingBaseUrl,
    MissingOfflineModel,
    SystemUnavailable,
}

val ProviderHealthStatus.isReady: Boolean
    get() = this == ProviderHealthStatus.Ready

fun providerHealthForConfig(
    config: ProviderConfig,
    androidTtsReady: Boolean = true,
    hasOfflineModels: Boolean = true,
): ProviderHealthStatus =
    providerHealthForDraft(
        providerId = config.providerId,
        enabled = config.enabled,
        hasApiKey = !config.encryptedValue.isNullOrBlank(),
        baseUrl = config.baseUrl,
        androidTtsReady = androidTtsReady,
        hasOfflineModels = hasOfflineModels,
    )

fun providerHealthForDraft(
    providerId: String,
    enabled: Boolean,
    hasApiKey: Boolean,
    baseUrl: String?,
    androidTtsReady: Boolean = true,
    hasOfflineModels: Boolean = true,
): ProviderHealthStatus {
    if (providerId == ProviderConfigRepository.ANDROID_SYSTEM) {
        if (!androidTtsReady) return ProviderHealthStatus.SystemUnavailable
        return if (enabled) ProviderHealthStatus.Ready else ProviderHealthStatus.Disabled
    }

    if (!enabled) return ProviderHealthStatus.Disabled

    if (providerId == ProviderConfigRepository.OFFLINE_SHERPA && !hasOfflineModels) {
        return ProviderHealthStatus.MissingOfflineModel
    }

    if (requiresBaseUrl(providerId) && baseUrl.isNullOrBlank()) {
        return ProviderHealthStatus.MissingBaseUrl
    }

    if (requiresApiKey(providerId) && !hasApiKey) {
        return ProviderHealthStatus.MissingApiKey
    }

    return ProviderHealthStatus.Ready
}

private fun requiresApiKey(providerId: String): Boolean =
    providerId == ProviderConfigRepository.OPENAI ||
        providerId == ProviderConfigRepository.GEMINI ||
        providerId == ProviderConfigRepository.MIMO

private fun requiresBaseUrl(providerId: String): Boolean =
    providerId == ProviderConfigRepository.OPENAI ||
        providerId == ProviderConfigRepository.EDGE_TTS_FORWARDER ||
        providerId == ProviderConfigRepository.GEMINI ||
        providerId == ProviderConfigRepository.MIMO ||
        providerId == ProviderConfigRepository.CUSTOM_HTTP
