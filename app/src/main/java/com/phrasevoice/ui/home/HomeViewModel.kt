package com.phrasevoice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.repository.HistoryRepository
import com.phrasevoice.data.repository.PhraseRepository
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.tts.AudioPlaybackController
import com.phrasevoice.data.tts.AndroidSystemTtsProvider
import com.phrasevoice.data.tts.CloudTtsService
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import com.phrasevoice.domain.tts.TtsVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val note: String? = null,
)

data class HomeUiState(
    val text: String = "",
    val providers: List<TtsProviderOption> = emptyList(),
    val selectedProviderId: String = ProviderConfigRepository.ANDROID_SYSTEM,
    val voices: List<TtsVoice> = emptyList(),
    val selectedVoiceId: String? = null,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val status: HomeStatus = HomeStatus.Idle,
    val errorMessage: String? = null,
    val quickPhrases: List<Phrase> = emptyList(),
    val lastAudioUri: String? = null,
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
                    val providers = configs.map { it.toProviderOption() }
                    val selectedProviderId = state.selectedProviderId
                        .takeIf { id -> configs.any { it.providerId == id } }
                        ?: ProviderConfigRepository.ANDROID_SYSTEM
                    val voices = voicesFor(selectedProviderId)
                    state.copy(
                        providers = providers,
                        selectedProviderId = selectedProviderId,
                        voices = voices,
                        selectedVoiceId = state.selectedVoiceId
                            ?.takeIf { voiceId -> voices.any { it.id == voiceId } }
                            ?: voices.firstOrNull()?.id,
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
        _uiState.update { it.copy(text = value, errorMessage = null, status = HomeStatus.Idle) }
    }

    fun selectProvider(providerId: String) {
        val option = uiState.value.providers.firstOrNull { it.id == providerId } ?: return
        val voices = voicesFor(providerId)
        _uiState.update {
            it.copy(
                selectedProviderId = providerId,
                voices = voices,
                selectedVoiceId = voices.firstOrNull()?.id,
                status = if (option.enabled) HomeStatus.Idle else HomeStatus.Error,
                errorMessage = if (option.enabled) null else "${option.name} 尚未启用，请先在 Provider 页面保存配置。",
            )
        }
    }

    fun selectVoice(voiceId: String?) {
        _uiState.update { it.copy(selectedVoiceId = voiceId) }
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

    fun speak() {
        speakText(uiState.value.text)
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
            val request = currentRequest(text = trimmed, outputFormat = AudioFormat.MP3)
            when (val result = synthesizeForCurrentProvider(request, playAudioFile = true)) {
                is TtsResult.LocalPlaybackStarted -> {
                    if (settingsRepository.settings.first().autoSaveHistory) {
                        historyRepository.addHistory(
                            text = trimmed,
                            providerId = request.providerId,
                            voiceId = request.voiceId,
                        )
                    }
                    _uiState.update { it.copy(status = HomeStatus.Playing, errorMessage = null) }
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
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Playing,
                            lastAudioUri = result.uri.toString(),
                            errorMessage = null,
                        )
                    }
                }

                is TtsResult.Error -> {
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
            val outputFormat = if (uiState.value.selectedProviderId == ProviderConfigRepository.ANDROID_SYSTEM) {
                AudioFormat.WAV
            } else {
                AudioFormat.MP3
            }
            val request = currentRequest(text = trimmed, outputFormat = outputFormat)
            when (val result = synthesizeForCurrentProvider(request, playAudioFile = false)) {
                is TtsResult.AudioFile -> {
                    historyRepository.addHistory(
                        text = trimmed,
                        providerId = request.providerId,
                        voiceId = request.voiceId,
                        audioUri = result.uri.toString(),
                    )
                    _uiState.update {
                        it.copy(
                            status = HomeStatus.Idle,
                            lastAudioUri = result.uri.toString(),
                            errorMessage = null,
                        )
                    }
                }

                is TtsResult.Error -> {
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
            ProviderConfigRepository.CUSTOM_HTTP -> {
                val result = cloudTtsService.synthesize(request, runtimeConfig)
                if (playAudioFile && result is TtsResult.AudioFile) {
                    audioPlaybackController.play(result.uri)
                }
                result
            }

            ProviderConfigRepository.GEMINI -> TtsResult.Error("Gemini TTS 会在后续版本接入，请先使用 Custom HTTP。")
            else -> TtsResult.Error("未知 Provider：${request.providerId}")
        }
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
            outputFormat = outputFormat,
        )
    }

    private fun ProviderConfig.toProviderOption(): TtsProviderOption =
        TtsProviderOption(
            id = providerId,
            name = when (providerId) {
                ProviderConfigRepository.ANDROID_SYSTEM -> "Android System TTS"
                ProviderConfigRepository.OPENAI -> "OpenAI-compatible TTS"
                ProviderConfigRepository.GEMINI -> "Gemini TTS"
                ProviderConfigRepository.CUSTOM_HTTP -> "Custom HTTP TTS"
                else -> providerId
            },
            enabled = enabled,
            note = when {
                providerId == ProviderConfigRepository.GEMINI -> "后续接入"
                !enabled -> "未启用"
                else -> null
            },
        )

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
}
