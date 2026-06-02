package com.phrasevoice.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phrasevoice.di.AppContainer
import com.phrasevoice.ui.history.HistoryScreen
import com.phrasevoice.ui.history.HistoryViewModel
import com.phrasevoice.ui.home.HomeScreen
import com.phrasevoice.ui.home.HomeViewModel
import com.phrasevoice.ui.library.PhraseLibraryScreen
import com.phrasevoice.ui.library.PhraseLibraryViewModel
import com.phrasevoice.ui.providers.ProviderSettingsScreen
import com.phrasevoice.ui.providers.ProviderSettingsViewModel
import com.phrasevoice.ui.settings.SettingsScreen
import com.phrasevoice.ui.settings.SettingsViewModel

private enum class Destination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("朗读", Icons.Filled.Home, Icons.Outlined.Home),
    Library("常用语", Icons.Filled.List, Icons.Outlined.List),
    History("历史", Icons.Filled.History, Icons.Outlined.History),
    Providers("Provider", Icons.Filled.Build, Icons.Outlined.Build),
    Settings("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
fun PhraseVoiceRoot(container: AppContainer) {
    val factory = remember(container) { AppViewModelFactory(container) }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val libraryViewModel: PhraseLibraryViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val providerSettingsViewModel: ProviderSettingsViewModel = viewModel(factory = factory)

    var destination by rememberSaveable { mutableStateOf(Destination.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    val isSelected = destination == item
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { destination = item },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (destination) {
            Destination.Home -> HomeScreen(
                state = homeViewModel.uiState.collectAsStateWithLifecycle().value,
                onTextChange = homeViewModel::updateText,
                onProviderSelected = homeViewModel::selectProvider,
                onVoiceSelected = homeViewModel::selectVoice,
                onVoiceStyleSelected = homeViewModel::selectVoiceStyle,
                onSpeedChange = homeViewModel::updateSpeed,
                onPitchChange = homeViewModel::updatePitch,
                onVolumeChange = homeViewModel::updateVolume,
                onSpeak = homeViewModel::speak,
                onStop = homeViewModel::stop,
                onSaveAudio = homeViewModel::saveAudio,
                onQuickPhraseClick = { phrase ->
                    homeViewModel.speakPhrase(phrase.id, phrase.text)
                },
                modifier = modifier,
            )

            Destination.Library -> PhraseLibraryScreen(
                state = libraryViewModel.uiState.collectAsStateWithLifecycle().value,
                onQueryChange = libraryViewModel::updateQuery,
                onGroupSelected = libraryViewModel::selectGroup,
                onAddPhrase = libraryViewModel::openAddDialog,
                onEditPhrase = libraryViewModel::openEditDialog,
                onDeletePhrase = libraryViewModel::deletePhrase,
                onToggleFavorite = libraryViewModel::toggleFavorite,
                onTitleDraftChange = libraryViewModel::updateTitleDraft,
                onTextDraftChange = libraryViewModel::updateTextDraft,
                onFavoriteDraftChange = libraryViewModel::updateFavoriteDraft,
                onDismissDialog = libraryViewModel::dismissDialog,
                onSaveDialog = libraryViewModel::saveDialog,
                onSpeakPhrase = { phrase ->
                    homeViewModel.speakPhrase(phrase.id, phrase.text)
                },
                onBuildExportJson = libraryViewModel::buildExportJson,
                onImportJson = libraryViewModel::importJson,
                onExportCompleted = libraryViewModel::markExportSuccess,
                onFileActionMessage = libraryViewModel::showFileActionMessage,
                modifier = modifier,
            )

            Destination.History -> HistoryScreen(
                state = historyViewModel.uiState.collectAsStateWithLifecycle().value,
                onSpeak = { item -> homeViewModel.speakText(item.text) },
                onSaveAsPhrase = historyViewModel::saveAsPhrase,
                onClear = historyViewModel::clearHistory,
                modifier = modifier,
            )

            Destination.Providers -> ProviderSettingsScreen(
                state = providerSettingsViewModel.uiState.collectAsStateWithLifecycle().value,
                onProviderSelected = providerSettingsViewModel::selectProvider,
                onEnabledChange = providerSettingsViewModel::updateEnabled,
                onApiKeyChange = providerSettingsViewModel::updateApiKeyDraft,
                onBaseUrlChange = providerSettingsViewModel::updateBaseUrlDraft,
                onModelChange = providerSettingsViewModel::updateModelDraft,
                onVoiceChange = providerSettingsViewModel::updateVoiceDraft,
                onMethodChange = providerSettingsViewModel::updateMethodDraft,
                onHeadersChange = providerSettingsViewModel::updateHeadersDraft,
                onBodyChange = providerSettingsViewModel::updateBodyDraft,
                onResponseTypeChange = providerSettingsViewModel::updateResponseTypeDraft,
                onResponseFieldChange = providerSettingsViewModel::updateResponseFieldDraft,
                onApplyTemplate = providerSettingsViewModel::applyCustomHttpTemplate,
                onMimoOptimizeTextPreviewChange = providerSettingsViewModel::updateMimoOptimizeTextPreviewDraft,
                onMimoPromptOptimizerModelChange = providerSettingsViewModel::updateMimoPromptOptimizerModelDraft,
                onMimoUseStreamingChange = providerSettingsViewModel::updateMimoUseStreamingDraft,
                onMimoVoiceDesignPresetSelected = providerSettingsViewModel::selectMimoVoiceDesignPreset,
                onMimoVoiceDesignPresetNameChange = providerSettingsViewModel::updateMimoVoiceDesignPresetNameDraft,
                onAddMimoVoiceDesignPreset = providerSettingsViewModel::addMimoVoiceDesignPreset,
                onSaveMimoVoiceDesignPreset = providerSettingsViewModel::saveMimoVoiceDesignPreset,
                onDeleteMimoVoiceDesignPreset = providerSettingsViewModel::deleteMimoVoiceDesignPreset,
                onOptimizeMimoVoiceDesign = providerSettingsViewModel::optimizeMimoVoiceDesignPrompt,
                onSave = providerSettingsViewModel::save,
                onTestVoice = providerSettingsViewModel::saveAndTestVoice,
                modifier = modifier,
            )

            Destination.Settings -> SettingsScreen(
                state = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                onDefaultProviderChange = settingsViewModel::updateDefaultProvider,
                onSpeedChange = settingsViewModel::updateDefaultSpeed,
                onPitchChange = settingsViewModel::updateDefaultPitch,
                onVolumeChange = settingsViewModel::updateDefaultVolume,
                onAutoSaveHistoryChange = settingsViewModel::updateAutoSaveHistory,
                onKeepAudioCacheChange = settingsViewModel::updateKeepAudioCache,
                onClearAudioCache = settingsViewModel::clearAudioCache,
                onClearDebugLogs = settingsViewModel::clearDebugLogs,
                onRefreshAudioCache = settingsViewModel::refreshAudioCacheInfo,
                modifier = modifier,
            )
        }
    }
}
