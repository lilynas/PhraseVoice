package com.phrasevoice.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.model.PhraseGroup
import com.phrasevoice.data.repository.PhraseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhraseLibraryUiState(
    val groups: List<PhraseGroup> = emptyList(),
    val phrases: List<Phrase> = emptyList(),
    val query: String = "",
    val selectedGroupId: String? = null,
    val isDialogOpen: Boolean = false,
    val editingPhraseId: String? = null,
    val titleDraft: String = "",
    val textDraft: String = "",
    val favoriteDraft: Boolean = false,
) {
    val filteredPhrases: List<Phrase>
        get() = phrases.filter { phrase ->
            val matchesGroup = selectedGroupId == null || phrase.groupId == selectedGroupId
            val matchesQuery = query.isBlank() ||
                phrase.title.contains(query, ignoreCase = true) ||
                phrase.text.contains(query, ignoreCase = true)
            matchesGroup && matchesQuery
        }
}

class PhraseLibraryViewModel(
    private val phraseRepository: PhraseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhraseLibraryUiState())
    val uiState: StateFlow<PhraseLibraryUiState> = _uiState

    init {
        viewModelScope.launch {
            phraseRepository.library.collect { library ->
                _uiState.update {
                    it.copy(groups = library.groups, phrases = library.phrases)
                }
            }
        }
    }

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun selectGroup(groupId: String?) {
        _uiState.update { it.copy(selectedGroupId = groupId) }
    }

    fun openAddDialog() {
        _uiState.update {
            it.copy(
                isDialogOpen = true,
                editingPhraseId = null,
                titleDraft = "",
                textDraft = "",
                favoriteDraft = false,
            )
        }
    }

    fun openEditDialog(phrase: Phrase) {
        _uiState.update {
            it.copy(
                isDialogOpen = true,
                editingPhraseId = phrase.id,
                titleDraft = phrase.title,
                textDraft = phrase.text,
                favoriteDraft = phrase.isFavorite,
            )
        }
    }

    fun updateTitleDraft(value: String) {
        _uiState.update { it.copy(titleDraft = value) }
    }

    fun updateTextDraft(value: String) {
        _uiState.update { it.copy(textDraft = value) }
    }

    fun updateFavoriteDraft(value: Boolean) {
        _uiState.update { it.copy(favoriteDraft = value) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isDialogOpen = false) }
    }

    fun saveDialog() {
        val state = uiState.value
        if (state.textDraft.isBlank()) return

        viewModelScope.launch {
            val groupId = state.selectedGroupId ?: PhraseRepository.DEFAULT_GROUP_ID
            if (state.editingPhraseId == null) {
                phraseRepository.addPhrase(
                    title = state.titleDraft,
                    text = state.textDraft,
                    groupId = groupId,
                    isFavorite = state.favoriteDraft,
                )
            } else {
                phraseRepository.updatePhrase(
                    id = state.editingPhraseId,
                    title = state.titleDraft,
                    text = state.textDraft,
                    groupId = groupId,
                    isFavorite = state.favoriteDraft,
                )
            }
            dismissDialog()
        }
    }

    fun deletePhrase(id: String) {
        viewModelScope.launch {
            phraseRepository.deletePhrase(id)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            phraseRepository.toggleFavorite(id)
        }
    }
}
