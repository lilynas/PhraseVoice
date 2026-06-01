package com.phrasevoice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.repository.HistoryRepository
import com.phrasevoice.data.repository.PhraseRepository
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.tts.AndroidSystemTtsProvider
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
)

data class HomeUiState(
    val text: String = "",
    val providers: List<TtsProviderOption> = listOf(
        TtsProviderOption(id = "android_system", name = "Android System TTS"),
        TtsProviderOption(id = "openai", name = "OpenAI TTS", enabled = false),
        TtsProviderOption(id = "gemini", name = "Gemini TTS", enabled = false),
        TtsProviderOption(id = "custom_http", name = "Custom HTTP", enabled = false),
    ),
    val selectedProviderId: String = "android_system",
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
    private val systemTtsProvider: AndroidSystemTtsProvider,
    private val audioFileStore: AudioFileStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

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
            val voices = runCatching { systemTtsProvider.listVoices() }.getOrElse { emptyList() }
            _uiState.update {
                it.copy(
                    voices = voices,
                    selectedVoiceId = voices.firstOrNull()?.id,
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
        if (!option.enabled) {
            _uiState.update {
                it.copy(
                    status = HomeStatus.Error,
                    errorMessage = "${option.name} 会在第二阶段接入。",
                )
            }
            return
        }
        _uiState.update { it.copy(selectedProviderId = providerId) }
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
            when (val result = systemTtsProvider.synthesize(request)) {
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
            val target = audioFileStore.createTarget(AudioFormat.WAV)
            val request = currentRequest(text = trimmed, outputFormat = AudioFormat.WAV)
            when (val result = systemTtsProvider.synthesizeToFile(
                request = request,
                file = target.file,
                uri = target.uri,
                mimeType = target.mimeType,
            )) {
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
        _uiState.update { it.copy(status = HomeStatus.Idle) }
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
}
