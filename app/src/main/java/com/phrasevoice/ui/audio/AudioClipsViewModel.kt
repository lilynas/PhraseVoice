package com.phrasevoice.ui.audio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.model.AudioClip
import com.phrasevoice.data.repository.AudioClipRepository
import com.phrasevoice.data.tts.AudioPlaybackController
import com.phrasevoice.debug.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AudioClipsUiState(
    val clips: List<AudioClip> = emptyList(),
    val isImporting: Boolean = false,
    val message: String? = null,
)

class AudioClipsViewModel(
    private val audioClipRepository: AudioClipRepository,
    private val audioFileStore: AudioFileStore,
    private val audioPlaybackController: AudioPlaybackController,
) : ViewModel() {
    private val isImporting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AudioClipsUiState> = combine(
        audioClipRepository.clips,
        isImporting,
        message,
    ) { clips, importing, currentMessage ->
        AudioClipsUiState(
            clips = clips,
            isImporting = importing,
            message = currentMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AudioClipsUiState(),
    )

    fun importClip(uri: Uri) {
        viewModelScope.launch {
            isImporting.value = true
            message.value = null
            runCatching {
                audioClipRepository.importClip(uri)
            }.onSuccess {
                message.value = "音频已导入"
            }.onFailure { throwable ->
                AppLogger.e(TAG, "import audio clip failed", throwable)
                message.value = "导入失败：${throwable.message ?: "无法读取音频"}"
            }
            isImporting.value = false
        }
    }

    fun playClip(clip: AudioClip) {
        viewModelScope.launch {
            runCatching {
                audioPlaybackController.stop()
                audioPlaybackController.play(audioFileStore.audioClipUri(clip.fileName))
                audioClipRepository.touchClip(clip.id)
            }.onFailure { throwable ->
                AppLogger.e(TAG, "play audio clip failed id=${clip.id}", throwable)
                message.value = "播放失败：${throwable.message ?: "无法播放音频"}"
            }
        }
    }

    fun deleteClip(id: String) {
        viewModelScope.launch {
            runCatching {
                audioClipRepository.deleteClip(id)
            }.onFailure { throwable ->
                AppLogger.e(TAG, "delete audio clip failed id=$id", throwable)
                message.value = "删除失败：${throwable.message ?: "无法删除音频"}"
            }
        }
    }

    companion object {
        private const val TAG = "AudioClipsViewModel"
    }
}
