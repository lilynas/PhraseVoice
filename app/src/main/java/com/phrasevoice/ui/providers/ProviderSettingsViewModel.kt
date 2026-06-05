package com.phrasevoice.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.model.CustomHttpResponseType
import com.phrasevoice.data.model.CustomHttpSettings
import com.phrasevoice.data.model.MimoSettings
import com.phrasevoice.data.model.MimoVoiceDesignPreset
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.RuntimeProviderConfig
import com.phrasevoice.data.tts.AudioPlaybackController
import com.phrasevoice.data.tts.AndroidSystemTtsProvider
import com.phrasevoice.data.tts.CloudTtsService
import com.phrasevoice.data.tts.MimoTtsCatalog
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

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
    val mimoOptimizeTextPreviewDraft: Boolean = false,
    val mimoPromptOptimizerModelDraft: String = MimoTtsCatalog.DEFAULT_PROMPT_OPTIMIZER_MODEL_ID,
    val mimoUseStreamingDraft: Boolean = false,
    val mimoVoiceDesignPresetsDraft: List<MimoVoiceDesignPreset> = emptyList(),
    val mimoSelectedVoiceDesignPresetIdDraft: String? = null,
    val mimoVoiceDesignPresetNameDraft: String = "",
    val savedMessage: String? = null,
    val isTesting: Boolean = false,
    val isOptimizingVoiceDesign: Boolean = false,
    val androidTtsReady: Boolean = true,
    val androidTtsMessage: String? = null,
) {
    val selectedConfig: ProviderConfig?
        get() = configs.firstOrNull { it.providerId == selectedProviderId }

    val isCustomHttp: Boolean
        get() = selectedProviderId == ProviderConfigRepository.CUSTOM_HTTP

    val isEdgeForwarder: Boolean
        get() = selectedProviderId == ProviderConfigRepository.EDGE_TTS_FORWARDER

    val isGemini: Boolean
        get() = selectedProviderId == ProviderConfigRepository.GEMINI

    val isMimo: Boolean
        get() = selectedProviderId == ProviderConfigRepository.MIMO

    val isMimoVoiceDesign: Boolean
        get() = isMimo && MimoTtsCatalog.isVoiceDesignModel(modelDraft)
}

class ProviderSettingsViewModel(
    private val providerConfigRepository: ProviderConfigRepository,
    private val cloudTtsService: CloudTtsService,
    private val audioPlaybackController: AudioPlaybackController,
    private val systemTtsProvider: AndroidSystemTtsProvider,
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
        viewModelScope.launch {
            val readiness = systemTtsProvider.readiness()
            _uiState.update {
                it.copy(
                    androidTtsReady = readiness.ready,
                    androidTtsMessage = readiness.message,
                )
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
    fun updateModelDraft(value: String) = _uiState.update { state ->
        val voiceDraft = if (state.isMimo) {
            when {
                MimoTtsCatalog.isVoiceDesignModel(value) &&
                    MimoTtsCatalog.isPresetVoice(state.voiceDraft) -> MimoTtsCatalog.DEFAULT_VOICE_DESIGN_PROMPT
                !MimoTtsCatalog.isVoiceDesignModel(value) &&
                    !MimoTtsCatalog.isPresetVoice(state.voiceDraft) -> MimoTtsCatalog.DEFAULT_VOICE_ID
                else -> state.voiceDraft
            }
        } else {
            state.voiceDraft
        }
        val next = state.copy(modelDraft = value, voiceDraft = voiceDraft, savedMessage = null)
        if (state.isMimo && MimoTtsCatalog.isVoiceDesignModel(value)) {
            next.ensureMimoVoiceDesignDrafts()
        } else {
            next
        }
    }
    fun updateVoiceDraft(value: String) = _uiState.update { it.copy(voiceDraft = value, savedMessage = null) }
    fun updateMethodDraft(value: String) = _uiState.update { it.copy(methodDraft = value, savedMessage = null) }
    fun updateHeadersDraft(value: String) = _uiState.update { it.copy(headersDraft = value, savedMessage = null) }
    fun updateBodyDraft(value: String) = _uiState.update { it.copy(bodyDraft = value, savedMessage = null) }
    fun updateResponseTypeDraft(value: CustomHttpResponseType) =
        _uiState.update { it.copy(responseTypeDraft = value, savedMessage = null) }

    fun updateResponseFieldDraft(value: String) =
        _uiState.update { it.copy(responseFieldDraft = value, savedMessage = null) }

    fun applyCustomHttpTemplate(presetName: String) {
        val preset = when (presetName) {
            "OpenAI" -> HttpPreset(
                url = "https://api.openai.com/v1/audio/speech",
                method = "POST",
                headers = "Authorization: Bearer {{ apiKey }}\nContent-Type: application/json",
                body = "{\n  \"model\": \"{{ model }}\",\n  \"input\": \"{{ text }}\",\n  \"voice\": \"{{ voice }}\"\n}",
                responseType = CustomHttpResponseType.RAW_AUDIO,
                responseField = "audio"
            )
            "MiniMax" -> HttpPreset(
                url = "https://api.minimax.chat/v1/t2a_v2?GroupId=YOUR_GROUP_ID",
                method = "POST",
                headers = "Authorization: Bearer {{ apiKey }}\nContent-Type: application/json",
                body = "{\n  \"model\": \"{{ model }}\",\n  \"text\": \"{{ text }}\",\n  \"voice_setting\": {\n    \"voice_id\": \"{{ voice }}\"\n  },\n  \"audio_setting\": {\n    \"sample_rate\": 24000,\n    \"bitrate\": 128000,\n    \"format\": \"mp3\"\n  }\n}",
                responseType = CustomHttpResponseType.JSON_BASE64_FIELD,
                responseField = "data.audio"
            )
            "Volcengine" -> HttpPreset(
                url = "https://openspeech.bytedance.com/api/v1/tts",
                method = "POST",
                headers = "Authorization: Bearer {{ apiKey }}\nContent-Type: application/json",
                body = "{\n  \"app\": {\n    \"appid\": \"YOUR_APPID\",\n    \"token\": \"{{ apiKey }}\",\n    \"cluster\": \"volc_tts_outdoor\"\n  },\n  \"user\": {\n    \"uid\": \"12345\"\n  },\n  \"audio\": {\n    \"voice_type\": \"{{ voice }}\",\n    \"encoding\": \"mp3\"\n  },\n  \"request\": {\n    \"reqid\": \"12345\",\n    \"text\": \"{{ text }}\",\n    \"text_type\": \"plain\",\n    \"operation\": \"query\"\n  }\n}",
                responseType = CustomHttpResponseType.JSON_BASE64_FIELD,
                responseField = "data"
            )
            else -> return
        }

        _uiState.update {
            it.copy(
                baseUrlDraft = preset.url,
                methodDraft = preset.method,
                headersDraft = preset.headers,
                bodyDraft = preset.body,
                responseTypeDraft = preset.responseType,
                responseFieldDraft = preset.responseField,
                savedMessage = "已应用 $presetName 模板"
            )
        }
    }

    private data class HttpPreset(
        val url: String,
        val method: String,
        val headers: String,
        val body: String,
        val responseType: CustomHttpResponseType,
        val responseField: String
    )

    fun updateMimoOptimizeTextPreviewDraft(value: Boolean) =
        _uiState.update { it.copy(mimoOptimizeTextPreviewDraft = value, savedMessage = null) }

    fun updateMimoPromptOptimizerModelDraft(value: String) =
        _uiState.update { it.copy(mimoPromptOptimizerModelDraft = value, savedMessage = null) }

    fun updateMimoUseStreamingDraft(value: Boolean) =
        _uiState.update { it.copy(mimoUseStreamingDraft = value, savedMessage = null) }

    fun updateMimoVoiceDesignPresetNameDraft(value: String) =
        _uiState.update { it.copy(mimoVoiceDesignPresetNameDraft = value, savedMessage = null) }

    fun selectMimoVoiceDesignPreset(presetId: String) {
        _uiState.update { state ->
            val preset = state.mimoVoiceDesignPresetsDraft.firstOrNull { it.id == presetId } ?: return@update state
            state.copy(
                mimoSelectedVoiceDesignPresetIdDraft = preset.id,
                mimoVoiceDesignPresetNameDraft = preset.name,
                voiceDraft = preset.description,
                savedMessage = null,
            )
        }
    }

    fun addMimoVoiceDesignPreset() {
        _uiState.update { state ->
            val now = System.currentTimeMillis()
            val preset = MimoVoiceDesignPreset(
                id = UUID.randomUUID().toString(),
                name = "新角色声音",
                description = MimoTtsCatalog.DEFAULT_VOICE_DESIGN_PROMPT,
                createdAt = now,
                updatedAt = now,
            )
            state.copy(
                mimoVoiceDesignPresetsDraft = state.mimoVoiceDesignPresetsDraft + preset,
                mimoSelectedVoiceDesignPresetIdDraft = preset.id,
                mimoVoiceDesignPresetNameDraft = preset.name,
                voiceDraft = preset.description,
                savedMessage = null,
            )
        }
    }

    fun saveMimoVoiceDesignPreset() {
        _uiState.update { state ->
            val description = state.voiceDraft.trim()
            if (description.isBlank()) {
                return@update state.copy(savedMessage = "请先填写音色描述")
            }

            val now = System.currentTimeMillis()
            val selectedId = state.mimoSelectedVoiceDesignPresetIdDraft
            val name = state.mimoVoiceDesignPresetNameDraft.trim().ifBlank { "角色声音" }
            var matched = false
            val updatedPresets = state.mimoVoiceDesignPresetsDraft.map { preset ->
                if (preset.id == selectedId) {
                    matched = true
                    preset.copy(name = name, description = description, updatedAt = now)
                } else {
                    preset
                }
            }.toMutableList()
            val nextId = if (matched && selectedId != null) {
                selectedId
            } else {
                val preset = MimoVoiceDesignPreset(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    createdAt = now,
                    updatedAt = now,
                )
                updatedPresets += preset
                preset.id
            }

            state.copy(
                mimoVoiceDesignPresetsDraft = updatedPresets.distinctBy { it.id },
                mimoSelectedVoiceDesignPresetIdDraft = nextId,
                mimoVoiceDesignPresetNameDraft = name,
                voiceDraft = description,
                savedMessage = "角色声音已暂存，请保存配置",
            )
        }
    }

    fun deleteMimoVoiceDesignPreset() {
        _uiState.update { state ->
            val selectedId = state.mimoSelectedVoiceDesignPresetIdDraft
            val remaining = state.mimoVoiceDesignPresetsDraft.filterNot { it.id == selectedId }
            if (selectedId == null || remaining.isEmpty()) {
                return@update state.copy(savedMessage = "至少保留一个角色声音")
            }
            val next = remaining.first()
            state.copy(
                mimoVoiceDesignPresetsDraft = remaining,
                mimoSelectedVoiceDesignPresetIdDraft = next.id,
                mimoVoiceDesignPresetNameDraft = next.name,
                voiceDraft = next.description,
                savedMessage = "角色声音已删除，请保存配置",
            )
        }
    }

    fun optimizeMimoVoiceDesignPrompt() {
        val state = uiState.value
        if (!state.isMimoVoiceDesign) {
            _uiState.update { it.copy(savedMessage = "请先切换到 MiMo VoiceDesign 模式") }
            return
        }
        if (state.voiceDraft.isBlank()) {
            _uiState.update { it.copy(savedMessage = "请先填写音色描述") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOptimizingVoiceDesign = true,
                    savedMessage = "正在优化音色描述",
                )
            }
            runCatching {
                val runtimeConfig = runtimeConfigFromDraft(state)
                cloudTtsService.optimizeMimoVoiceDesignPrompt(
                    draft = state.voiceDraft,
                    runtimeConfig = runtimeConfig,
                    optimizerModel = state.mimoPromptOptimizerModelDraft,
                )
            }.fold(
                onSuccess = { optimized ->
                    _uiState.update {
                        it.copy(
                            voiceDraft = optimized,
                            savedMessage = "已优化音色描述，请保存配置",
                            isOptimizingVoiceDesign = false,
                        )
                    }
                    AppLogger.i(TAG, "mimo voice design prompt optimized")
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            savedMessage = "优化失败：${throwable.message ?: "未知错误"}",
                            isOptimizingVoiceDesign = false,
                        )
                    }
                    AppLogger.e(TAG, "mimo voice design prompt optimize failed", throwable)
                },
            )
        }
    }

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
                    isOptimizingVoiceDesign = false,
                )
            }
            AppLogger.i(TAG, "save complete provider=${state.selectedProviderId}")
        }
    }

    fun saveAndTestVoice() {
        val state = uiState.value
        if (state.selectedProviderId != ProviderConfigRepository.OPENAI &&
            state.selectedProviderId != ProviderConfigRepository.EDGE_TTS_FORWARDER &&
            state.selectedProviderId != ProviderConfigRepository.GEMINI &&
            state.selectedProviderId != ProviderConfigRepository.MIMO &&
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
                    outputFormat = if (state.isGemini || state.isMimo) AudioFormat.WAV else AudioFormat.MP3,
                    mimoOptimizeTextPreview = state.isMimoVoiceDesign && state.mimoOptimizeTextPreviewDraft,
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
        when {
            state.isCustomHttp -> {
                PhraseVoiceJson.encode(
                    CustomHttpSettings(
                        method = state.methodDraft.ifBlank { "POST" },
                        headersTemplate = state.headersDraft,
                        bodyTemplate = state.bodyDraft,
                        responseType = state.responseTypeDraft,
                        responseField = state.responseFieldDraft.ifBlank { "audio" },
                    ),
                )
            }

            state.isMimo -> {
                val voiceDesignPresets = normalizedMimoVoiceDesignPresets(state)
                val selectedVoiceDesignPresetId = state.mimoSelectedVoiceDesignPresetIdDraft
                    ?.takeIf { id -> voiceDesignPresets.any { it.id == id } }
                    ?: voiceDesignPresets.firstOrNull()?.id
                PhraseVoiceJson.encode(
                    MimoSettings(
                        optimizeTextPreview = state.mimoOptimizeTextPreviewDraft,
                        promptOptimizerModel = state.mimoPromptOptimizerModelDraft
                            .ifBlank { MimoTtsCatalog.DEFAULT_PROMPT_OPTIMIZER_MODEL_ID },
                        useStreaming = state.mimoUseStreamingDraft,
                        selectedVoiceDesignPresetId = selectedVoiceDesignPresetId,
                        voiceDesignPresets = voiceDesignPresets,
                    ),
                )
            }

            else -> {
                state.selectedConfig?.extraJson
            }
        }

    private suspend fun runtimeConfigFromDraft(state: ProviderSettingsUiState): RuntimeProviderConfig {
        val saved = providerConfigRepository.getRuntimeConfig(state.selectedProviderId)
        return saved.copy(
            config = saved.config.copy(
                enabled = state.enabledDraft,
                baseUrl = state.baseUrlDraft.trim().takeIf { it.isNotBlank() },
                model = state.modelDraft.trim().takeIf { it.isNotBlank() },
                defaultVoice = state.voiceDraft.trim().takeIf { it.isNotBlank() },
                extraJson = extraJsonFor(state),
            ),
            apiKey = state.apiKeyDraft.takeIf { it.isNotBlank() } ?: saved.apiKey,
        )
    }

    private fun ProviderSettingsUiState.withSelectedConfig(config: ProviderConfig): ProviderSettingsUiState {
        val customSettings = PhraseVoiceJson.decode(config.extraJson, CustomHttpSettings())
        val mimoSettings = PhraseVoiceJson.decode(config.extraJson, MimoSettings())
        val modelDraft = config.model.orEmpty()
        val legacyVoiceDraft = if (
            config.providerId == ProviderConfigRepository.MIMO &&
            MimoTtsCatalog.isVoiceDesignModel(modelDraft) &&
            MimoTtsCatalog.isPresetVoice(config.defaultVoice)
        ) {
            MimoTtsCatalog.DEFAULT_VOICE_DESIGN_PROMPT
        } else {
            config.defaultVoice.orEmpty()
        }
        val mimoVoiceDesignPresets = seededMimoVoiceDesignPresets(config, modelDraft, mimoSettings, legacyVoiceDraft)
        val selectedMimoVoiceDesignPreset = mimoVoiceDesignPresets
            .firstOrNull { it.id == mimoSettings.selectedVoiceDesignPresetId }
            ?: mimoVoiceDesignPresets.firstOrNull()
        val voiceDraft = if (
            config.providerId == ProviderConfigRepository.MIMO &&
            MimoTtsCatalog.isVoiceDesignModel(modelDraft)
        ) {
            selectedMimoVoiceDesignPreset?.description ?: legacyVoiceDraft
        } else {
            legacyVoiceDraft
        }
        return copy(
            selectedProviderId = config.providerId,
            enabledDraft = config.enabled,
            apiKeyDraft = "",
            hasSavedApiKey = !config.encryptedValue.isNullOrBlank(),
            baseUrlDraft = config.baseUrl.orEmpty(),
            modelDraft = modelDraft,
            voiceDraft = voiceDraft,
            methodDraft = customSettings.method,
            headersDraft = customSettings.headersTemplate,
            bodyDraft = customSettings.bodyTemplate,
            responseTypeDraft = customSettings.responseType,
            responseFieldDraft = customSettings.responseField,
            mimoOptimizeTextPreviewDraft = mimoSettings.optimizeTextPreview,
            mimoPromptOptimizerModelDraft = mimoSettings.promptOptimizerModel
                .ifBlank { MimoTtsCatalog.DEFAULT_PROMPT_OPTIMIZER_MODEL_ID },
            mimoUseStreamingDraft = mimoSettings.useStreaming,
            mimoVoiceDesignPresetsDraft = mimoVoiceDesignPresets,
            mimoSelectedVoiceDesignPresetIdDraft = selectedMimoVoiceDesignPreset?.id,
            mimoVoiceDesignPresetNameDraft = selectedMimoVoiceDesignPreset?.name.orEmpty(),
            isOptimizingVoiceDesign = false,
        )
    }

    private fun ProviderSettingsUiState.ensureMimoVoiceDesignDrafts(): ProviderSettingsUiState {
        if (!isMimoVoiceDesign || mimoVoiceDesignPresetsDraft.isNotEmpty()) return this
        val now = System.currentTimeMillis()
        val description = voiceDraft.takeIf { it.isNotBlank() } ?: MimoTtsCatalog.DEFAULT_VOICE_DESIGN_PROMPT
        val preset = MimoVoiceDesignPreset(
            id = DEFAULT_MIMO_VOICE_DESIGN_PRESET_ID,
            name = "默认角色",
            description = description,
            createdAt = now,
            updatedAt = now,
        )
        return copy(
            voiceDraft = description,
            mimoVoiceDesignPresetsDraft = listOf(preset),
            mimoSelectedVoiceDesignPresetIdDraft = preset.id,
            mimoVoiceDesignPresetNameDraft = preset.name,
        )
    }

    private fun normalizedMimoVoiceDesignPresets(state: ProviderSettingsUiState): List<MimoVoiceDesignPreset> {
        val now = System.currentTimeMillis()
        val existing = state.mimoVoiceDesignPresetsDraft
            .filter { it.id.isNotBlank() && it.description.isNotBlank() }
            .distinctBy { it.id }
            .toMutableList()

        if (!state.isMimoVoiceDesign) return existing

        val description = state.voiceDraft.trim()
        if (description.isBlank()) return existing

        val selectedId = state.mimoSelectedVoiceDesignPresetIdDraft
            ?: existing.firstOrNull()?.id
            ?: DEFAULT_MIMO_VOICE_DESIGN_PRESET_ID
        val name = state.mimoVoiceDesignPresetNameDraft.trim().ifBlank { "默认角色" }
        val index = existing.indexOfFirst { it.id == selectedId }
        if (index >= 0) {
            existing[index] = existing[index].copy(
                name = name,
                description = description,
                updatedAt = now,
            )
        } else {
            existing += MimoVoiceDesignPreset(
                id = selectedId,
                name = name,
                description = description,
                createdAt = now,
                updatedAt = now,
            )
        }
        return existing
    }

    private fun seededMimoVoiceDesignPresets(
        config: ProviderConfig,
        modelDraft: String,
        settings: MimoSettings,
        legacyVoiceDraft: String,
    ): List<MimoVoiceDesignPreset> {
        val presets = settings.voiceDesignPresets
            .filter { it.id.isNotBlank() && it.description.isNotBlank() }
            .distinctBy { it.id }
        if (presets.isNotEmpty()) return presets
        if (config.providerId != ProviderConfigRepository.MIMO || !MimoTtsCatalog.isVoiceDesignModel(modelDraft)) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        return listOf(
            MimoVoiceDesignPreset(
                id = DEFAULT_MIMO_VOICE_DESIGN_PRESET_ID,
                name = "默认角色",
                description = legacyVoiceDraft.ifBlank { MimoTtsCatalog.DEFAULT_VOICE_DESIGN_PROMPT },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    companion object {
        private const val TAG = "ProviderSettings"
        private const val TEST_TEXT = "你好，这是 PhraseVoice 的语音试听。"
        private const val DEFAULT_MIMO_VOICE_DESIGN_PRESET_ID = "default"
    }
}
