package com.phrasevoice.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.model.CustomHttpResponseType
import com.phrasevoice.data.model.CustomHttpSettings
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.tts.AudioPlaybackController
import com.phrasevoice.data.tts.CloudTtsService
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderSettingsUiState(
    val configs: List<ProviderConfig> = emptyList(),
    val selectedProviderId: String = ProviderConfigRepository.OPENAI,
    val enabledDraft: Boolean = false,
    val apiKeyDraft: String = "",
    val hasSavedApiKey: Boolean = false,
    val baseUrlDraft: String = "",
    val modelDraft: String = "",
    val voiceDraft: String = "",
    val methodDraft: String = "POST",
    val headersDraft: String = "",
    val bodyDraft: String = "",
    val responseTypeDraft: CustomHttpResponseType = CustomHttpResponseType.RAW_AUDIO,
    val responseFieldDraft: String = "audio",
    val savedMessage: String? = null,
    val isTesting: Boolean = false,
) {
    val selectedConfig: ProviderConfig?
        get() = configs.firstOrNull { it.providerId == selectedProviderId }

    val isCustomHttp: Boolean
        get() = selectedProviderId == ProviderConfigRepository.CUSTOM_HTTP
}

class ProviderSettingsViewModel(
    private val providerConfigRepository: ProviderConfigRepository,
    private val cloudTtsService: CloudTtsService,
    private val audioPlaybackController: AudioPlaybackController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProviderSettingsUiState())
    val uiState: StateFlow<ProviderSettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            providerConfigRepository.configs.collect { configs ->
                _uiState.update { current ->
                    val selectedId = current.selectedProviderId
                        .takeIf { id -> configs.any { it.providerId == id } }
                        ?: ProviderConfigRepository.OPENAI
                    val selected = configs.first { it.providerId == selectedId }
                    current.copy(configs = configs).withSelectedConfig(selected)
                }
            }
        }
    }

    fun selectProvider(providerId: String) {
        val selected = uiState.value.configs.firstOrNull { it.providerId == providerId } ?: return
        AppLogger.i(TAG, "settings selectProvider id=$providerId enabled=${selected.enabled}")
        _uiState.update {
            it.copy(selectedProviderId = providerId, savedMessage = null)
                .withSelectedConfig(selected)
        }
    }

    fun updateEnabled(value: Boolean) = _uiState.update { it.copy(enabledDraft = value, savedMessage = null) }
    fun updateApiKeyDraft(value: String) = _uiState.update { it.copy(apiKeyDraft = value, savedMessage = null) }
    fun updateBaseUrlDraft(value: String) = _uiState.update { it.copy(baseUrlDraft = value, savedMessage = null) }
    fun updateModelDraft(value: String) = _uiState.update { it.copy(modelDraft = value, savedMessage = null) }
    fun updateVoiceDraft(value: String) = _uiState.update { it.copy(voiceDraft = value, savedMessage = null) }
    fun updateMethodDraft(value: String) = _uiState.update { it.copy(methodDraft = value, savedMessage = null) }
    fun updateHeadersDraft(value: String) = _uiState.update { it.copy(headersDraft = value, savedMessage = null) }
    fun updateBodyDraft(value: String) = _uiState.update { it.copy(bodyDraft = value, savedMessage = null) }
    fun updateResponseTypeDraft(value: CustomHttpResponseType) =
        _uiState.update { it.copy(responseTypeDraft = value, savedMessage = null) }

    fun updateResponseFieldDraft(value: String) =
        _uiState.update { it.copy(responseFieldDraft = value, savedMessage = null) }

    fun save() {
        val state = uiState.value
        AppLogger.i(
            TAG,
            "save provider=${state.selectedProviderId} enabled=${state.enabledDraft} baseUrlSet=${state.baseUrlDraft.isNotBlank()} model=${state.modelDraft} voice=${state.voiceDraft} apiKeyChanged=${state.apiKeyDraft.isNotBlank()}",
        )

        viewModelScope.launch {
            saveDraft(state)
            _uiState.update {
                it.copy(
                    apiKeyDraft = "",
                    hasSavedApiKey = it.hasSavedApiKey || state.apiKeyDraft.isNotBlank(),
                    savedMessage = "已保存",
                    isTesting = false,
                )
            }
            AppLogger.i(TAG, "save complete provider=${state.selectedProviderId}")
        }
    }

    fun saveAndTestVoice() {
        val state = uiState.value
        if (state.selectedProviderId != ProviderConfigRepository.OPENAI &&
            state.selectedProviderId != ProviderConfigRepository.CUSTOM_HTTP
        ) {
            _uiState.update { it.copy(savedMessage = "当前 Provider 不需要云端试听") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, savedMessage = "正在保存并试听") }
            runCatching {
                saveDraft(state)
                val runtimeConfig = providerConfigRepository.getRuntimeConfig(state.selectedProviderId)
                val request = TtsRequest(
                    text = TEST_TEXT,
                    providerId = state.selectedProviderId,
                    voiceId = state.voiceDraft.takeIf { it.isNotBlank() },
                    language = null,
                    speed = 1.0f,
                    pitch = 1.0f,
                    volume = 1.0f,
                    stylePrompt = "自然、清晰、适合日常交流",
                    outputFormat = AudioFormat.MP3,
                )
                when (val result = cloudTtsService.synthesize(request, runtimeConfig, cache = true)) {
                    is TtsResult.AudioFile -> {
                        audioPlaybackController.play(result.uri)
                        _uiState.update {
                            it.copy(
                                apiKeyDraft = "",
                                hasSavedApiKey = it.hasSavedApiKey || state.apiKeyDraft.isNotBlank(),
                                savedMessage = "试听已播放",
                                isTesting = false,
                            )
                        }
                        AppLogger.i(TAG, "test playback started provider=${state.selectedProviderId}")
                    }

                    is TtsResult.Error -> {
                        _uiState.update {
                            it.copy(savedMessage = "试听失败：${result.message}", isTesting = false)
                        }
                        AppLogger.e(TAG, "test failed provider=${state.selectedProviderId}: ${result.message}", result.cause)
                    }

                    is TtsResult.LocalPlaybackStarted -> {
                        _uiState.update { it.copy(savedMessage = "试听已播放", isTesting = false) }
                    }
                }
            }.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        savedMessage = "试听失败：${throwable.message ?: "未知错误"}",
                        isTesting = false,
                    )
                }
                AppLogger.e(TAG, "test failed provider=${state.selectedProviderId}", throwable)
            }
        }
    }

    private suspend fun saveDraft(state: ProviderSettingsUiState) {
        providerConfigRepository.saveConfig(
            providerId = state.selectedProviderId,
            enabled = state.enabledDraft,
            apiKeyPlainText = state.apiKeyDraft.takeIf { it.isNotBlank() },
            baseUrl = state.baseUrlDraft,
            model = state.modelDraft,
            defaultVoice = state.voiceDraft,
            extraJson = extraJsonFor(state),
        )
    }

    private fun extraJsonFor(state: ProviderSettingsUiState): String? =
        if (state.isCustomHttp) {
            PhraseVoiceJson.encode(
                CustomHttpSettings(
                    method = state.methodDraft.ifBlank { "POST" },
                    headersTemplate = state.headersDraft,
                    bodyTemplate = state.bodyDraft,
                    responseType = state.responseTypeDraft,
                    responseField = state.responseFieldDraft.ifBlank { "audio" },
                ),
            )
        } else {
            state.selectedConfig?.extraJson
        }

    private fun ProviderSettingsUiState.withSelectedConfig(config: ProviderConfig): ProviderSettingsUiState {
        val customSettings = PhraseVoiceJson.decode(config.extraJson, CustomHttpSettings())
        return copy(
            selectedProviderId = config.providerId,
            enabledDraft = config.enabled,
            apiKeyDraft = "",
            hasSavedApiKey = !config.encryptedValue.isNullOrBlank(),
            baseUrlDraft = config.baseUrl.orEmpty(),
            modelDraft = config.model.orEmpty(),
            voiceDraft = config.defaultVoice.orEmpty(),
            methodDraft = customSettings.method,
            headersDraft = customSettings.headersTemplate,
            bodyDraft = customSettings.bodyTemplate,
            responseTypeDraft = customSettings.responseType,
            responseFieldDraft = customSettings.responseField,
        )
    }

    companion object {
        private const val TAG = "ProviderSettings"
        private const val TEST_TEXT = "你好，这是 PhraseVoice 的语音试听。"
    }
}
