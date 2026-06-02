package com.phrasevoice.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.debug.DebugLogEntry
import com.phrasevoice.data.model.UserSettings
import com.phrasevoice.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val debugLogs: List<DebugLogEntry> = emptyList(),
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        AppLogger.entries,
    ) { settings, logs ->
        SettingsUiState(settings = settings, debugLogs = logs)
    }
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

    fun clearDebugLogs() {
        AppLogger.clear()
    }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }
}
