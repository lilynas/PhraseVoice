package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val defaultProviderId: String = "android_system",
    val defaultSpeed: Float = 1.0f,
    val defaultPitch: Float = 1.0f,
    val defaultVolume: Float = 1.0f,
    val autoSaveHistory: Boolean = true,
    val keepAudioCache: Boolean = true,
    val cloudFallbackToSystemTts: Boolean = true,
    val themeMode: String = "system",
    val languageMode: String = "system",
    val communicationTextScale: Float = 1.0f,
    val communicationTextTone: String = "mint",
    val lockScreenCommunicationEnabled: Boolean = true,
    val contactCardName: String = "PhraseVoice",
    val contactCardSubtitle: String = "很高兴认识你",
    val contactCardAccount: String = "@phrasevoice",
    val contactCardQrContent: String = "PhraseVoice",
    val displayCards: List<DisplayCard> = emptyList(),
    val displayCardsMigrated: Boolean = false,
    val offlineVoiceModels: List<OfflineVoiceModel> = emptyList(),
    val hasCompletedOnboarding: Boolean = false,
    val debugLoggingEnabled: Boolean = false,
    val debugLogLevel: String = "INFO",
)
