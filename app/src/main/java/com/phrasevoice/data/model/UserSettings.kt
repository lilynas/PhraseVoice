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
    val themeMode: String = "system",
    val debugLoggingEnabled: Boolean = true,
    val debugLogLevel: String = "INFO",
)
