package com.phrasevoice.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.model.UserSettings
import com.phrasevoice.data.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { settings -> SettingsUiState(settings) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

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

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }
}
