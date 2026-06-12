package com.phrasevoice.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.AudioCacheInfo
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.debug.DebugLogEntry
import com.phrasevoice.data.model.UserSettings
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.ProviderHealthStatus
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.repository.isReady
import com.phrasevoice.data.repository.providerHealthForConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsProviderOption(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val status: ProviderHealthStatus = ProviderHealthStatus.Ready,
    val note: String? = null,
)

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val providers: List<SettingsProviderOption> = emptyList(),
    val audioCacheInfo: AudioCacheInfo = AudioCacheInfo(fileCount = 0, totalBytes = 0),
    val cacheMessage: String? = null,
    val debugLogs: List<DebugLogEntry> = emptyList(),
    val isLoaded: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val providerConfigRepository: ProviderConfigRepository,
    private val audioFileStore: AudioFileStore,
) : ViewModel() {
    private val audioCacheInfo = MutableStateFlow(audioFileStore.cacheInfo())
    private val cacheMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                AppLogger.configure(
                    enabled = settings.debugLoggingEnabled,
                    minLevel = settings.debugLogLevel,
                )
            }
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        providerConfigRepository.configs,
        audioCacheInfo,
        cacheMessage,
        AppLogger.entries,
    ) { settings, providerConfigs, cacheInfo, message, logs ->
        SettingsUiState(
            settings = settings,
            providers = providerConfigs.map(::toProviderOption),
            audioCacheInfo = cacheInfo,
            cacheMessage = message,
            debugLogs = logs,
            isLoaded = true,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun updateDefaultProvider(providerId: String) {
        update { it.copy(defaultProviderId = providerId) }
    }

    fun updateDefaultSpeed(value: Float) {
        update { it.copy(defaultSpeed = value) }
    }

    fun updateDefaultPitch(value: Float) {
        update { it.copy(defaultPitch = value) }
    }

    fun updateDefaultVolume(value: Float) {
        update { it.copy(defaultVolume = value) }
    }

    fun updateAutoSaveHistory(value: Boolean) {
        update { it.copy(autoSaveHistory = value) }
    }

    fun updateKeepAudioCache(value: Boolean) {
        update { it.copy(keepAudioCache = value) }
    }

    fun updateThemeMode(value: String) {
        update { it.copy(themeMode = value) }
    }

    fun updateLanguageMode(value: String) {
        update { it.copy(languageMode = value) }
    }

    fun updateCommunicationTextScale(value: Float) {
        update { it.copy(communicationTextScale = value.coerceIn(0.85f, 1.35f)) }
    }

    fun updateCommunicationTextTone(value: String) {
        val normalized = if (value in COMMUNICATION_TEXT_TONES) value else "mint"
        update { it.copy(communicationTextTone = normalized) }
    }

    fun updateLockScreenCommunicationEnabled(value: Boolean) {
        update { it.copy(lockScreenCommunicationEnabled = value) }
    }

    fun updateContactCardName(value: String) {
        update { it.copy(contactCardName = value) }
    }

    fun updateContactCardSubtitle(value: String) {
        update { it.copy(contactCardSubtitle = value) }
    }

    fun updateContactCardAccount(value: String) {
        update { it.copy(contactCardAccount = value) }
    }

    fun updateContactCardQrContent(value: String) {
        update { it.copy(contactCardQrContent = value) }
    }

    fun completeOnboarding() {
        update { it.copy(hasCompletedOnboarding = true) }
    }

    fun updateDebugLoggingEnabled(value: Boolean) {
        update { it.copy(debugLoggingEnabled = value) }
    }

    fun updateDebugLogLevel(value: String) {
        update { it.copy(debugLogLevel = value) }
    }

    fun clearDebugLogs() {
        AppLogger.clear()
    }

    fun refreshAudioCacheInfo() {
        viewModelScope.launch {
            audioCacheInfo.value = withContext(Dispatchers.IO) {
                audioFileStore.cacheInfo()
            }
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            val before = withContext(Dispatchers.IO) {
                val current = audioFileStore.cacheInfo()
                audioFileStore.clearCache()
                current
            }
            audioCacheInfo.value = withContext(Dispatchers.IO) {
                audioFileStore.cacheInfo()
            }
            cacheMessage.value = if (before.fileCount == 0) {
                "没有可清理的音频缓存"
            } else {
                "已清理 ${before.fileCount} 个音频文件"
            }
            AppLogger.i(TAG, "audio cache cleared files=${before.fileCount} bytes=${before.totalBytes}")
        }
    }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    private fun toProviderOption(config: ProviderConfig): SettingsProviderOption {
        val status = providerHealthForConfig(config)
        return SettingsProviderOption(
            id = config.providerId,
            name = when (config.providerId) {
                ProviderConfigRepository.ANDROID_SYSTEM -> "Android System TTS"
                ProviderConfigRepository.OPENAI -> "OpenAI TTS"
                ProviderConfigRepository.EDGE_TTS_FORWARDER -> "Edge TTS Forwarder"
                ProviderConfigRepository.GEMINI -> "Gemini TTS"
                ProviderConfigRepository.MIMO -> "MiMo TTS"
                ProviderConfigRepository.CUSTOM_HTTP -> "Custom TTS API"
                else -> config.providerId
            },
            enabled = status.isReady,
            status = status,
            note = when (status) {
                ProviderHealthStatus.Ready -> null
                ProviderHealthStatus.Disabled -> "未配置"
                ProviderHealthStatus.MissingApiKey -> "缺少 API Key"
                ProviderHealthStatus.MissingBaseUrl -> "缺少 Base URL"
                ProviderHealthStatus.SystemUnavailable -> "系统 TTS 不可用"
            },
        )
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private val COMMUNICATION_TEXT_TONES = setOf("mint", "sky", "warm", "lavender")
    }
}
