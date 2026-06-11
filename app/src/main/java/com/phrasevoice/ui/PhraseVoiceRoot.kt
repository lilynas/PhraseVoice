package com.phrasevoice.ui

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phrasevoice.ui.theme.PhraseVoiceTheme
import com.phrasevoice.di.AppContainer
import com.phrasevoice.system.QuickReturnNotifier
import com.phrasevoice.ui.audio.AudioClipsViewModel
import com.phrasevoice.ui.communicate.CommunicateScreen
import com.phrasevoice.ui.communicate.ContactCardUiState
import com.phrasevoice.ui.history.HistoryScreen
import com.phrasevoice.ui.history.HistoryViewModel
import com.phrasevoice.ui.home.HomeStatus
import com.phrasevoice.ui.home.HomeScreen
import com.phrasevoice.ui.home.HomeViewModel
import com.phrasevoice.ui.i18n.LocalAppLanguage
import com.phrasevoice.ui.i18n.resolveAppLanguage
import com.phrasevoice.ui.i18n.t
import com.phrasevoice.ui.library.PhraseLibraryScreen
import com.phrasevoice.ui.library.PhraseLibraryViewModel
import com.phrasevoice.ui.providers.ProviderSettingsScreen
import com.phrasevoice.ui.providers.ProviderSettingsViewModel
import com.phrasevoice.ui.settings.SettingsScreen
import com.phrasevoice.ui.settings.SettingsViewModel

private enum class Destination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Communicate(Icons.Filled.RecordVoiceOver, Icons.Outlined.RecordVoiceOver),
    Home(Icons.Filled.Home, Icons.Outlined.Home),
    Library(Icons.Filled.List, Icons.Outlined.List),
    History(Icons.Filled.History, Icons.Outlined.History),
    Providers(Icons.Filled.Build, Icons.Outlined.Build),
    Settings(Icons.Filled.Settings, Icons.Outlined.Settings),
}

@Composable
private fun Destination.label(): String =
    when (this) {
        Destination.Communicate -> t("交流", "Talk")
        Destination.Home -> t("朗读", "Read")
        Destination.Library -> t("常用语", "Phrases")
        Destination.History -> t("历史", "History")
        Destination.Providers -> t("声音", "Voice")
        Destination.Settings -> t("设置", "Settings")
    }

@Composable
fun PhraseVoiceRoot(
    container: AppContainer,
    communicationRequestKey: Int = 0,
) {
    val factory = remember(container) { AppViewModelFactory(container) }
    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val libraryViewModel: PhraseLibraryViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val providerSettingsViewModel: ProviderSettingsViewModel = viewModel(factory = factory)
    val audioClipsViewModel: AudioClipsViewModel = viewModel(factory = factory)

    var destination by rememberSaveable { mutableStateOf(Destination.Communicate) }

    val context = LocalContext.current
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val audioClipsState by audioClipsViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (settingsState.settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }
    val appLanguage = resolveAppLanguage(settingsState.settings.languageMode)

    LaunchedEffect(communicationRequestKey) {
        if (communicationRequestKey > 0) {
            destination = Destination.Communicate
        }
    }

    LaunchedEffect(homeState.status, homeState.text) {
        when (homeState.status) {
            HomeStatus.Loading,
            HomeStatus.Playing -> QuickReturnNotifier.showSpeaking(context, homeState.text)
            else -> QuickReturnNotifier.show(context)
        }
    }

    PhraseVoiceTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
            SystemBarsEffect(darkTheme = isDarkTheme)

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Destination.entries.forEach { item ->
                            val isSelected = destination == item
                            val label = item.label()
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { destination = item },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = {
                        val stiffness = Spring.StiffnessLow
                        if (targetState.ordinal > initialState.ordinal) {
                            (slideInHorizontally { width -> (width * 0.08f).toInt() } + fadeIn(animationSpec = spring(stiffness = stiffness))) togetherWith
                                    (slideOutHorizontally { width -> -(width * 0.08f).toInt() } + fadeOut(animationSpec = spring(stiffness = stiffness)))
                        } else {
                            (slideInHorizontally { width -> -(width * 0.08f).toInt() } + fadeIn(animationSpec = spring(stiffness = stiffness))) togetherWith
                                    (slideOutHorizontally { width -> (width * 0.08f).toInt() } + fadeOut(animationSpec = spring(stiffness = stiffness)))
                        }.using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "screenTransition"
                ) { targetDest ->
                    val modifier = Modifier.padding(innerPadding)
                    when (targetDest) {
                        Destination.Communicate -> CommunicateScreen(
                            state = homeState,
                            contactCard = ContactCardUiState(
                                name = settingsState.settings.contactCardName,
                                subtitle = settingsState.settings.contactCardSubtitle,
                                account = settingsState.settings.contactCardAccount,
                                qrContent = settingsState.settings.contactCardQrContent,
                            ),
                            audioClipsState = audioClipsState,
                            onTextChange = homeViewModel::updateText,
                            onQuickPhraseClick = { phrase ->
                                homeViewModel.stop()
                                homeViewModel.speakPhrase(phrase.id, phrase.text)
                            },
                            onQuickPhraseGroupSelected = homeViewModel::selectQuickPhraseGroup,
                            onAudioClipImport = audioClipsViewModel::importClip,
                            onAudioClipClick = { clip ->
                                homeViewModel.stop()
                                audioClipsViewModel.playClip(clip)
                            },
                            onAudioClipDelete = audioClipsViewModel::deleteClip,
                            onSpeak = homeViewModel::speak,
                            onStop = homeViewModel::stop,
                            onReplay = {
                                homeViewModel.stop()
                                homeViewModel.speak()
                            },
                            modifier = modifier,
                        )

                        Destination.Home -> HomeScreen(
                            state = homeState,
                            onTextChange = homeViewModel::updateText,
                            onProviderSelected = homeViewModel::selectProvider,
                            onVoiceSelected = homeViewModel::selectVoice,
                            onVoiceStyleSelected = homeViewModel::selectVoiceStyle,
                            onSpeedChange = homeViewModel::updateSpeed,
                            onPitchChange = homeViewModel::updatePitch,
                            onVolumeChange = homeViewModel::updateVolume,
                            onReadingPresetSelected = homeViewModel::applyReadingPreset,
                            onTextOptimizationSelected = homeViewModel::optimizeText,
                            onMimoSmartTextOptimizationChange = homeViewModel::updateMimoSmartTextOptimization,
                            onSpeak = homeViewModel::speak,
                            onPreviewVoice = homeViewModel::previewVoice,
                            onStop = homeViewModel::stop,
                            onSaveAudio = homeViewModel::saveAudio,
                            onQuickPhraseClick = { phrase ->
                                homeViewModel.speakPhrase(phrase.id, phrase.text)
                            },
                            onQuickPhraseGroupSelected = homeViewModel::selectQuickPhraseGroup,
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
                            state = settingsState,
                            onDefaultProviderChange = settingsViewModel::updateDefaultProvider,
                            onSpeedChange = settingsViewModel::updateDefaultSpeed,
                            onPitchChange = settingsViewModel::updateDefaultPitch,
                            onVolumeChange = settingsViewModel::updateDefaultVolume,
                            onAutoSaveHistoryChange = settingsViewModel::updateAutoSaveHistory,
                            onKeepAudioCacheChange = settingsViewModel::updateKeepAudioCache,
                            onClearAudioCache = settingsViewModel::clearAudioCache,
                            onClearDebugLogs = settingsViewModel::clearDebugLogs,
                            onDebugLoggingEnabledChange = settingsViewModel::updateDebugLoggingEnabled,
                            onDebugLogLevelChange = settingsViewModel::updateDebugLogLevel,
                            onRefreshAudioCache = settingsViewModel::refreshAudioCacheInfo,
                            onThemeModeChange = settingsViewModel::updateThemeMode,
                            onLanguageModeChange = settingsViewModel::updateLanguageMode,
                            onContactCardNameChange = settingsViewModel::updateContactCardName,
                            onContactCardSubtitleChange = settingsViewModel::updateContactCardSubtitle,
                            onContactCardAccountChange = settingsViewModel::updateContactCardAccount,
                            onContactCardQrContentChange = settingsViewModel::updateContactCardQrContent,
                            modifier = modifier,
                        )
                    }
                }
            }

            if (settingsState.isLoaded && !settingsState.settings.hasCompletedOnboarding) {
                OnboardingDialog(
                    onUseSystemTts = settingsViewModel::completeOnboarding,
                    onConfigureProvider = {
                        settingsViewModel.completeOnboarding()
                        destination = Destination.Providers
                    },
                )
            }
        }
    }
}

@Composable
private fun OnboardingDialog(
    onUseSystemTts: () -> Unit,
    onConfigureProvider: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onUseSystemTts,
        title = {
            Text(t("开始使用 PhraseVoice", "Start with PhraseVoice"))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    t(
                        "先确认一个语音引擎，就可以稳定朗读、保存和分享音频。",
                        "Pick a voice engine first so reading, saving, and sharing audio work reliably.",
                    ),
                )
                Text(t("1. 系统 TTS 无需 API Key，适合马上试用。", "1. System TTS needs no API key and is best for a quick try."))
                Text(t("2. 云端 Provider 需要先填写 Key 和服务地址。", "2. Cloud Providers need a key and service URL first."))
                Text(t("3. 配置页可以保存并试听，确认声音可用。", "3. The Provider page can save and test voices before you use them."))
            }
        },
        confirmButton = {
            Button(onClick = onConfigureProvider) {
                Text(t("去配置 Provider", "Configure Provider"))
            }
        },
        dismissButton = {
            TextButton(onClick = onUseSystemTts) {
                Text(t("先用系统 TTS", "Use System TTS"))
            }
        },
    )
}

@Composable
private fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background.toArgb()
    val navigationColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.toArgb()

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = backgroundColor
        window.navigationBarColor = navigationColor

        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}
