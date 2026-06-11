package com.phrasevoice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.phrasevoice.di.AppContainer
import com.phrasevoice.ui.audio.AudioClipsViewModel
import com.phrasevoice.ui.history.HistoryViewModel
import com.phrasevoice.ui.home.HomeViewModel
import com.phrasevoice.ui.library.PhraseLibraryViewModel
import com.phrasevoice.ui.providers.ProviderSettingsViewModel
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
                providerConfigRepository = container.providerConfigRepository,
                systemTtsProvider = container.systemTtsProvider,
                cloudTtsService = container.cloudTtsService,
                audioPlaybackController = container.audioPlaybackController,
                audioFileStore = container.audioFileStore,
            )

            modelClass.isAssignableFrom(PhraseLibraryViewModel::class.java) -> PhraseLibraryViewModel(
                phraseRepository = container.phraseRepository,
            )

            modelClass.isAssignableFrom(AudioClipsViewModel::class.java) -> AudioClipsViewModel(
                audioClipRepository = container.audioClipRepository,
                audioFileStore = container.audioFileStore,
                audioPlaybackController = container.audioPlaybackController,
            )

            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
                historyRepository = container.historyRepository,
                phraseRepository = container.phraseRepository,
            )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                providerConfigRepository = container.providerConfigRepository,
                audioFileStore = container.audioFileStore,
            )

            modelClass.isAssignableFrom(ProviderSettingsViewModel::class.java) -> ProviderSettingsViewModel(
                providerConfigRepository = container.providerConfigRepository,
                cloudTtsService = container.cloudTtsService,
                audioPlaybackController = container.audioPlaybackController,
                systemTtsProvider = container.systemTtsProvider,
            )

            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
}
