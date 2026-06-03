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
import com.phrasevoice.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsProviderOption(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val note: String? = null,
)

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val providers: List<SettingsProviderOption> = emptyList(),
    val audioCacheInfo: AudioCacheInfo = AudioCacheInfo(fileCount = 0, totalBytes = 0),
    val cacheMessage: String? = null,
    val debugLogs: List<DebugLogEntry> = emptyList(),
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val providerConfigRepository: ProviderConfigRepository,
    private val audioFileStore: AudioFileStore,
) : ViewModel() {
    private val audioCacheInfo = MutableStateFlow(audioFileStore.cacheInfo())
    private val cacheMessage = MutableStateFlow<String?>(null)

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

    private fun toProviderOption(config: ProviderConfig): SettingsProviderOption =
        SettingsProviderOption(
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
            enabled = config.enabled,
            note = when {
                !config.enabled -> "未启用"
                else -> null
            },
        )

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
