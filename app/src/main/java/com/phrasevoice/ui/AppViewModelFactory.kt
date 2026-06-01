package com.phrasevoice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.phrasevoice.di.AppContainer
import com.phrasevoice.ui.history.HistoryViewModel
import com.phrasevoice.ui.home.HomeViewModel
import com.phrasevoice.ui.library.PhraseLibraryViewModel
import com.phrasevoice.ui.settings.SettingsViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(
                phraseRepository = container.phraseRepository,
                historyRepository = container.historyRepository,
                settingsRepository = container.settingsRepository,
                systemTtsProvider = container.systemTtsProvider,
                audioFileStore = container.audioFileStore,
            )

            modelClass.isAssignableFrom(PhraseLibraryViewModel::class.java) -> PhraseLibraryViewModel(
                phraseRepository = container.phraseRepository,
            )

            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
                historyRepository = container.historyRepository,
                phraseRepository = container.phraseRepository,
            )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
            )

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
}
