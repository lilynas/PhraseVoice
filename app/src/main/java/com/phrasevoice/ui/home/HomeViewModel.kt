package com.phrasevoice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.model.MimoSettings
import com.phrasevoice.data.model.MimoVoiceDesignPreset
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.repository.HistoryRepository
import com.phrasevoice.data.repository.PhraseRepository
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.ProviderHealthStatus
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.repository.isReady
import com.phrasevoice.data.repository.providerHealthForConfig
import com.phrasevoice.data.tts.AudioPlaybackController
import com.phrasevoice.data.tts.AndroidSystemTtsProvider
import com.phrasevoice.data.tts.CloudTtsService
import com.phrasevoice.data.tts.EdgeForwarderCatalog
import com.phrasevoice.data.tts.EdgeForwarderStyle
import com.phrasevoice.data.tts.GeminiTtsCatalog
import com.phrasevoice.data.tts.MimoTtsCatalog
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.domain.text.TextOptimizationAction
import com.phrasevoice.domain.text.TextOptimizer
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.ReadingPreset
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import com.phrasevoice.domain.tts.TtsVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HomeStatus {
    Idle,
    Loading,
    Playing,
    Saving,
    Error,
}

data class TtsProviderOption(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val status: ProviderHealthStatus = ProviderHealthStatus.Ready,
    val note: String? = null,
)

data class HomeUiState(
    val text: String = "",
    val providers: List<TtsProviderOption> = emptyList(),
    val selectedProviderId: String = ProviderConfigRepository.ANDROID_SYSTEM,
    val voices: List<TtsVoice> = emptyList(),
    val selectedVoiceId: String? = null,
    val voiceStyles: List<EdgeForwarderStyle> = emptyList(),
    val selectedVoiceStyleId: String? = null,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val status: HomeStatus = HomeStatus.Idle,
    val errorMessage: String? = null,
    val quickPhrases: List<Phrase> = emptyList(),
    val lastAudioUri: String? = null,
    val lastAudioMimeType: String? = null,
    val androidTtsReady: Boolean = true,
    val androidTtsMessage: String? = null,
)

class HomeViewModel(
    private val phraseRepository: PhraseRepository,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val providerConfigRepository: ProviderConfigRepository,
    private val systemTtsProvider: AndroidSystemTtsProvider,
    private val cloudTtsService: CloudTtsService,
    private val audioPlaybackController: AudioPlaybackController,
    private val audioFileStore: AudioFileStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    private var providerConfigs: List<ProviderConfig> = ProviderConfigRepository.defaultConfigs()
    private var androidVoices: List<TtsVoice> = emptyList()

    init {
        viewModelScope.launch {
            phraseRepository.ensureSeedData()
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        selectedProviderId = settings.defaultProviderId,
                        speed = settings.defaultSpeed,
                        pitch = settings.defaultPitch,
                        volume = settings.defaultVolume,
                    )
                }
            }
        }
        viewModelScope.launch {
            providerConfigRepository.configs.collect { configs ->
                providerConfigs = configs
                _uiState.update { state ->
                    val providers = configs.map {
                        it.toProviderOption(
                            androidTtsReady = state.androidTtsReady,
                            androidTtsMessage = state.androidTtsMessage,
                        )
                    }
                    val selectedProviderId = state.selectedProviderId
                        .takeIf { id -> configs.any { it.providerId == id } }
                        ?: ProviderConfigRepository.ANDROID_SYSTEM
                    val voices = voicesFor(selectedProviderId)
                    val styles = stylesFor(selectedProviderId)
                    state.copy(
                        providers = providers,
                        selectedProviderId = selectedProviderId,
                        voices = voices,
                        selectedVoiceId = state.selectedVoiceId
                            ?.takeIf { voiceId -> voices.any { it.id == voiceId } }
                            ?: voices.firstOrNull()?.id,
                        voiceStyles = styles,
                        selectedVoiceStyleId = state.selectedVoiceStyleId
                            ?.takeIf { styleId -> styles.any { it.id == styleId } }
                            ?: styles.firstOrNull()?.id,
                    )
                }
            }
        }
        viewModelScope.launch {
            val voices = runCatching { systemTtsProvider.listVoices() }.getOrElse { emptyList() }
            androidVoices = voices
            _uiState.update {
                it.copy(
                    voices = voicesFor(it.selectedProviderId),
                    selectedVoiceId = voicesFor(it.selectedProviderId).firstOrNull()?.id,
                    voiceStyles = stylesFor(it.selectedProviderId),
                    selectedVoiceStyleId = stylesFor(it.selectedProviderId).firstOrNull()?.id,
                )
            }
        }
        viewModelScope.launch {
            val readiness = systemTtsProvider.readiness()
            _uiState.update { state ->
                val selectedAndroid = state.selectedProviderId == ProviderConfigRepository.ANDROID_SYSTEM
                state.copy(
                    androidTtsReady = readiness.ready,
                    androidTtsMessage = readiness.message,
                    providers = providerConfigs.map {
                        it.toProviderOption(
                            androidTtsReady = readiness.ready,
                            androidTtsMessage = readiness.message,
                        )
                    },
                    status = if (selectedAndroid && !readiness.ready) HomeStatus.Error else state.status,
                    errorMessage = if (selectedAndroid && !readiness.ready) readiness.message else state.errorMessage,
                )
            }
        }
        viewModelScope.launch {
            phraseRepository.phrases.collect { phrases ->
                val quick = phrases
                    .sortedWith(
                        compareByDescending<Phrase> { it.isFavorite }
                            .thenByDescending { it.lastUsedAt ?: 0L }
                            .thenBy { it.sortOrder },
                    )
                    .take(12)
                _uiState.update { it.copy(quickPhrases = quick) }
            }
        }
    }

    fun updateText(value: String) {
        _uiState.update {
            it.copy(
                text = value,
                errorMessage = null,
                status = HomeStatus.Idle,
                lastAudioUri = null,
                lastAudioMimeType = null,
            )
        }
    }

    fun selectProvider(providerId: String) {
        val option = uiState.value.providers.firstOrNull { it.id == providerId } ?: return
        val voices = voicesFor(providerId)
        val styles = stylesFor(providerId)
        AppLogger.i(TAG, "selectProvider id=$providerId enabled=${option.enabled}")
        _uiState.update {
            val androidUnavailable = providerId == ProviderConfigRepository.ANDROID_SYSTEM && !it.androidTtsReady
            it.copy(
                selectedProviderId = providerId,
                voices = voices,
                selectedVoiceId = voices.firstOrNull()?.id,
                voiceStyles = styles,
                selectedVoiceStyleId = styles.firstOrNull()?.id,
                lastAudioUri = null,
                lastAudioMimeType = null,
                status = when {
                    androidUnavailable -> HomeStatus.Error
                    option.status.isReady -> HomeStatus.Idle
                    else -> HomeStatus.Error
                },
                errorMessage = when {
                    androidUnavailable -> it.androidTtsMessage
                    option.status.isReady -> null
                    else -> providerHealthErrorMessage(option.name, option.status)
                },
            )
        }
    }

    fun selectVoiceStyle(styleId: String?) {
        _uiState.update {
            it.copy(
                selectedVoiceStyleId = styleId,
                lastAudioUri = null,
                lastAudioMimeType = null,
            )
        }
    }

    fun selectVoice(voiceId: String?) {
        _uiState.update {
            it.copy(
                selectedVoiceId = voiceId,
                lastAudioUri = null,
                lastAudioMimeType = null,
            )
        }
    }

    fun updateSpeed(value: Float) {
        _uiState.update { it.copy(speed = value) }
    }

    fun updatePitch(value: Float) {
        _uiState.update { it.copy(pitch = value) }
    }

    fun updateVolume(value: Float) {
        _uiState.update { it.copy(volume = value) }
    }

    fun applyReadingPreset(preset: ReadingPreset) {
        _uiState.update { state ->
            val presetStyleId = preset.edgeStyleId
                ?.takeIf { styleId -> state.voiceStyles.any { it.id == styleId } }
            state.copy(
                speed = preset.speed,
                pitch = preset.pitch,
                volume = preset.volume,
                selectedVoiceStyleId = presetStyleId ?: state.selectedVoiceStyleId,
                errorMessage = null,
                status = HomeStatus.Idle,
            )
        }
    }

    fun optimizeText(action: TextOptimizationAction) {
        _uiState.update { state ->
            val optimized = TextOptimizer.apply(state.text, action)
            state.copy(
                text = optimized,
                errorMessage = null,
                status = HomeStatus.Idle,
                lastAudioUri = null,
                lastAudioMimeType = null,
            )
        }
    }

    fun speak() {
        speakText(uiState.value.text)
    }

    fun previewVoice() {
        viewModelScope.launch {
            val sampleText = "你好，这是 PhraseVoice 的声音试听。Hello, this is a voice preview."
            _uiState.update { it.copy(status = HomeStatus.Loading, errorMessage = null) }
            val request = currentRequest(
                text = sampleText,
                outputFormat = outputFormatForSelectedProvider(),
            )
            AppLogger.i(
                TAG,
                "preview start provider=${request.providerId} voice=${request.voiceId.orEmpty()}",
            )
            when (val result = synthesizeForCurrentProvider(request, playAudioFile = true)) {
                is TtsResult.LocalPlaybackStarted -> {
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Playing,
                            errorMessage = null,
                            lastAudioUri = null,
                            lastAudioMimeType = null,
                        )
                    }
                }

                is TtsResult.AudioFile -> {
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Playing,
                            lastAudioUri = result.uri.toString(),
                            lastAudioMimeType = result.mimeType,
                            errorMessage = null,
                        )
                    }
                }

                is TtsResult.Error -> {
                    AppLogger.e(TAG, "preview failed provider=${request.providerId}: ${result.message}", result.cause)
                    _uiState.update {
                        it.copy(status = HomeStatus.Error, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun speakPhrase(phraseId: String, text: String) {
        _uiState.update { it.copy(text = text) }
        viewModelScope.launch {
            phraseRepository.touchPhrase(phraseId)
        }
        speakText(text)
    }

    fun speakText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(status = HomeStatus.Error, errorMessage = "请输入要朗读的文字。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = HomeStatus.Loading, errorMessage = null) }
            val request = currentRequest(text = trimmed, outputFormat = outputFormatForSelectedProvider())
            AppLogger.i(
                TAG,
                "speak start provider=${request.providerId} voice=${request.voiceId.orEmpty()} length=${trimmed.length}",
            )
            when (val result = synthesizeForCurrentProvider(request, playAudioFile = true)) {
                is TtsResult.LocalPlaybackStarted -> {
                    if (settingsRepository.settings.first().autoSaveHistory) {
                        historyRepository.addHistory(
                            text = trimmed,
                            providerId = request.providerId,
                            voiceId = request.voiceId,
                        )
                    }
                    AppLogger.i(TAG, "speak local playback started provider=${request.providerId}")
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Playing,
                            errorMessage = null,
                            lastAudioUri = null,
                            lastAudioMimeType = null,
                        )
                    }
                }

                is TtsResult.AudioFile -> {
                    if (settingsRepository.settings.first().autoSaveHistory) {
                        historyRepository.addHistory(
                            text = trimmed,
                            providerId = request.providerId,
                            voiceId = request.voiceId,
                            audioUri = result.uri.toString(),
                        )
                    }
                    AppLogger.i(TAG, "speak audio file ready provider=${request.providerId} uri=${result.uri}")
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Playing,
                            lastAudioUri = result.uri.toString(),
                            lastAudioMimeType = result.mimeType,
                            errorMessage = null,
                        )
                    }
                }

                is TtsResult.Error -> {
                    AppLogger.e(TAG, "speak failed provider=${request.providerId}: ${result.message}", result.cause)
                    _uiState.update {
                        it.copy(status = HomeStatus.Error, errorMessage = result.message)
                    }
                }
            }
        }
    }

    fun saveAudio() {
        val trimmed = uiState.value.text.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(status = HomeStatus.Error, errorMessage = "请输入要保存为音频的文字。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = HomeStatus.Saving, errorMessage = null) }
            val outputFormat = outputFormatForSelectedProvider()
            val request = currentRequest(text = trimmed, outputFormat = outputFormat)
            AppLogger.i(
                TAG,
                "saveAudio start provider=${request.providerId} voice=${request.voiceId.orEmpty()} format=$outputFormat length=${trimmed.length}",
            )
            when (val result = synthesizeForCurrentProvider(request, playAudioFile = false)) {
                is TtsResult.AudioFile -> {
                    historyRepository.addHistory(
                        text = trimmed,
                        providerId = request.providerId,
                        voiceId = request.voiceId,
                        audioUri = result.uri.toString(),
                    )
                    AppLogger.i(TAG, "saveAudio success provider=${request.providerId} uri=${result.uri}")
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Idle,
                            lastAudioUri = result.uri.toString(),
                            lastAudioMimeType = result.mimeType,
                            errorMessage = null,
                        )
                    }
                }

                is TtsResult.Error -> {
                    AppLogger.e(TAG, "saveAudio failed provider=${request.providerId}: ${result.message}", result.cause)
                    _uiState.update {
                        it.copy(status = HomeStatus.Error, errorMessage = result.message)
                    }
                }

                is TtsResult.LocalPlaybackStarted -> Unit
            }
        }
    }

    fun stop() {
        systemTtsProvider.stop()
        audioPlaybackController.stop()
        _uiState.update { it.copy(status = HomeStatus.Idle) }
    }

    private suspend fun synthesizeForCurrentProvider(
        request: TtsRequest,
        playAudioFile: Boolean,
    ): TtsResult {
        val runtimeConfig = providerConfigRepository.getRuntimeConfig(request.providerId)
        if (!runtimeConfig.config.enabled) {
            return TtsResult.Error("请先在 Provider 页面启用并保存该 Provider。")
        }

        return when (request.providerId) {
            ProviderConfigRepository.ANDROID_SYSTEM -> {
                if (request.outputFormat == AudioFormat.WAV && !playAudioFile) {
                    val target = audioFileStore.createTarget(AudioFormat.WAV)
                    systemTtsProvider.synthesizeToFile(
                        request = request,
                        file = target.file,
                        uri = target.uri,
                        mimeType = target.mimeType,
                    )
                } else {
                    systemTtsProvider.synthesize(request)
                }
            }

            ProviderConfigRepository.OPENAI,
            ProviderConfigRepository.EDGE_TTS_FORWARDER,
            ProviderConfigRepository.GEMINI,
            ProviderConfigRepository.MIMO,
            ProviderConfigRepository.CUSTOM_HTTP -> {
                val settings = settingsRepository.currentSettings()
                if (playAudioFile && !settings.keepAudioCache) {
                    withContext(Dispatchers.IO) {
                        audioFileStore.clearCache()
                    }
                }
                val result = cloudTtsService.synthesize(
                    request = request,
                    runtimeConfig = runtimeConfig,
                    cache = playAudioFile,
                )
                if (playAudioFile && result is TtsResult.AudioFile) {
                    audioPlaybackController.play(result.uri)
                }
                result
            }

            else -> TtsResult.Error("未知 Provider：${request.providerId}")
        }
    }

    private fun outputFormatForSelectedProvider(): AudioFormat =
        when (uiState.value.selectedProviderId) {
            ProviderConfigRepository.ANDROID_SYSTEM,
            ProviderConfigRepository.GEMINI,
            ProviderConfigRepository.MIMO -> AudioFormat.WAV
            else -> AudioFormat.MP3
        }

    private fun currentRequest(text: String, outputFormat: AudioFormat): TtsRequest {
        val state = uiState.value
        return TtsRequest(
            text = text,
            providerId = state.selectedProviderId,
            voiceId = state.selectedVoiceId?.takeIf { it.isNotBlank() },
            language = state.voices.firstOrNull { it.id == state.selectedVoiceId }?.language,
            speed = state.speed,
            pitch = state.pitch,
            volume = state.volume,
            stylePrompt = state.selectedVoiceStyleId
                ?.takeIf { state.selectedProviderId == ProviderConfigRepository.EDGE_TTS_FORWARDER }
                ?.takeIf { it.isNotBlank() },
            outputFormat = outputFormat,
        )
    }

    private fun ProviderConfig.toProviderOption(
        androidTtsReady: Boolean = true,
        androidTtsMessage: String? = null,
    ): TtsProviderOption {
        val status = providerHealthForConfig(
            config = this,
            androidTtsReady = androidTtsReady,
        )
        return TtsProviderOption(
            id = providerId,
            name = providerLabel(providerId),
            enabled = status.isReady,
            status = status,
            note = when (status) {
                ProviderHealthStatus.Ready -> null
                ProviderHealthStatus.Disabled -> "未配置"
                ProviderHealthStatus.MissingApiKey -> "缺少 API Key"
                ProviderHealthStatus.MissingBaseUrl -> "缺少 Base URL"
                ProviderHealthStatus.SystemUnavailable ->
                    androidTtsMessage ?: "系统 TTS 不可用"
            },
        )
    }

    private fun providerLabel(providerId: String): String =
        when (providerId) {
                ProviderConfigRepository.ANDROID_SYSTEM -> "Android System TTS"
                ProviderConfigRepository.OPENAI -> "OpenAI TTS"
                ProviderConfigRepository.EDGE_TTS_FORWARDER -> "Edge TTS Forwarder"
                ProviderConfigRepository.GEMINI -> "Gemini TTS"
                ProviderConfigRepository.MIMO -> "MiMo TTS"
                ProviderConfigRepository.CUSTOM_HTTP -> "Custom TTS API"
                else -> providerId
        }

    private fun providerHealthErrorMessage(
        providerName: String,
        status: ProviderHealthStatus,
    ): String =
        when (status) {
            ProviderHealthStatus.Ready -> ""
            ProviderHealthStatus.Disabled -> "$providerName 未配置，请先在 Provider 页面启用并保存。"
            ProviderHealthStatus.MissingApiKey -> "$providerName 缺少 API Key，请先在 Provider 页面保存 API Key。"
            ProviderHealthStatus.MissingBaseUrl -> "$providerName 缺少 Base URL，请先在 Provider 页面填写服务地址。"
            ProviderHealthStatus.SystemUnavailable -> "Android 系统 TTS 暂不可用。"
        }

    private fun voicesFor(providerId: String): List<TtsVoice> =
        when (providerId) {
            ProviderConfigRepository.ANDROID_SYSTEM -> androidVoices.ifEmpty {
                listOf(
                    TtsVoice(
                        id = "",
                        name = "System default",
                        language = null,
                        providerId = ProviderConfigRepository.ANDROID_SYSTEM,
                    ),
                )
            }

            ProviderConfigRepository.OPENAI -> openAiVoices()
            ProviderConfigRepository.EDGE_TTS_FORWARDER -> edgeForwarderVoices()
            ProviderConfigRepository.GEMINI -> geminiVoices()
            ProviderConfigRepository.MIMO -> mimoVoices()
            ProviderConfigRepository.CUSTOM_HTTP -> {
                val voice = providerConfigs
                    .firstOrNull { it.providerId == ProviderConfigRepository.CUSTOM_HTTP }
                    ?.defaultVoice
                    ?.takeIf { it.isNotBlank() }
                    ?: "default"
                listOf(
                    TtsVoice(
                        id = voice,
                        name = voice,
                        language = null,
                        description = "Configured voice",
                        providerId = ProviderConfigRepository.CUSTOM_HTTP,
                    ),
                )
            }

            else -> emptyList()
        }

    private fun stylesFor(providerId: String): List<EdgeForwarderStyle> =
        if (providerId == ProviderConfigRepository.EDGE_TTS_FORWARDER) {
            EdgeForwarderCatalog.styles
        } else {
            emptyList()
        }

    private fun edgeForwarderVoices(): List<TtsVoice> {
        val providerId = ProviderConfigRepository.EDGE_TTS_FORWARDER
        val configured = providerConfigs.firstOrNull { it.providerId == providerId }?.defaultVoice
        val defaults = EdgeForwarderCatalog.voices.map { it.id to it.name }
        val configuredVoice = configured
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless { voice -> defaults.any { it.first == voice } }
            ?.let { it to it }
        return (listOfNotNull(configuredVoice) + defaults)
            .distinctBy { it.first }
            .map { (voice, name) ->
                TtsVoice(
                    id = voice,
                    name = name,
                    language = voice.substringAfter("(", "").substringBefore(",").takeIf { it.isNotBlank() },
                    providerId = providerId,
                )
            }
    }

    private fun mimoVoices(): List<TtsVoice> {
        val providerId = ProviderConfigRepository.MIMO
        val config = providerConfigs.firstOrNull { it.providerId == providerId }
        if (MimoTtsCatalog.isVoiceDesignModel(config?.model)) {
            val settings = PhraseVoiceJson.decode(config?.extraJson, MimoSettings())
            val description = config?.defaultVoice
                ?.takeIf { it.isNotBlank() }
                ?: MimoTtsCatalog.DEFAULT_VOICE_DESIGN_PROMPT
            val fallbackPreset = MimoVoiceDesignPreset(
                id = "default",
                name = "默认角色",
                description = description,
            )
            val presets = settings.voiceDesignPresets
                .filter { it.description.isNotBlank() }
                .ifEmpty { listOf(fallbackPreset) }
            val selected = presets.firstOrNull { it.id == settings.selectedVoiceDesignPresetId }
            val orderedPresets = if (selected == null) {
                presets
            } else {
                listOf(selected) + presets.filterNot { it.id == selected.id }
            }
            return orderedPresets
                .distinctBy { it.description }
                .map { preset ->
                    TtsVoice(
                        id = preset.description,
                        name = preset.name.ifBlank { "VoiceDesign 角色声音" },
                        language = null,
                        description = preset.description,
                        providerId = providerId,
                    )
                }
        }
        val configured = config?.defaultVoice
        val defaults = MimoTtsCatalog.presetVoices.map { voice ->
            voice.id to buildString {
                append(voice.name)
                voice.language?.let { append(" $it") }
                voice.gender?.let { append(" $it") }
            }
        }
        val configuredVoice = configured
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless { voice -> defaults.any { it.first == voice } }
            ?.let { it to it }
        return (listOfNotNull(configuredVoice) + defaults)
            .distinctBy { it.first }
            .map { (voice, name) ->
                TtsVoice(
                    id = voice,
                    name = name,
                    language = null,
                    providerId = providerId,
                )
            }
    }

    private fun geminiVoices(): List<TtsVoice> {
        val providerId = ProviderConfigRepository.GEMINI
        val configured = providerConfigs.firstOrNull { it.providerId == providerId }?.defaultVoice
        val defaults = GeminiTtsCatalog.voices.map { it.id to "${it.id} - ${it.tone}" }
        val configuredVoice = configured
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless { voice -> defaults.any { it.first == voice } }
            ?.let { it to it }
        return (listOfNotNull(configuredVoice) + defaults)
            .distinctBy { it.first }
            .map { (voice, name) ->
                TtsVoice(
                    id = voice,
                    name = name,
                    language = null,
                    providerId = providerId,
                )
            }
    }

    private fun openAiVoices(): List<TtsVoice> {
        val providerId = ProviderConfigRepository.OPENAI
        val configured = providerConfigs.firstOrNull { it.providerId == providerId }?.defaultVoice
        val defaults = listOf("alloy", "ash", "ballad", "coral", "echo", "fable", "nova", "onyx", "sage", "shimmer")
        return (listOfNotNull(configured?.takeIf { it.isNotBlank() }) + defaults)
            .distinct()
            .map { voice ->
                TtsVoice(
                    id = voice,
                    name = voice,
                    language = null,
                    providerId = providerId,
                )
            }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
