package com.phrasevoice.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.model.TtsHistory
import com.phrasevoice.data.repository.HistoryRepository
import com.phrasevoice.data.repository.PhraseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val items: List<TtsHistory> = emptyList(),
)

class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val phraseRepository: PhraseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        viewModelScope.launch {
            historyRepository.history.collect { items ->
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    fun saveAsPhrase(item: TtsHistory) {
        viewModelScope.launch {
            phraseRepository.addPhrase(
                title = item.text.take(24),
                text = item.text,
                isFavorite = false,
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
