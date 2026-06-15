package com.phrasevoice.di

import android.content.Context
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.local.OfflineVoiceModelStore
import com.phrasevoice.data.local.phraseVoiceDataStore
import com.phrasevoice.data.repository.HistoryRepository
import com.phrasevoice.data.repository.AudioClipRepository
import com.phrasevoice.data.repository.PhraseRepository
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.security.ApiKeyCipher
import com.phrasevoice.data.tts.AudioPlaybackController
import com.phrasevoice.data.tts.AndroidSystemTtsProvider
import com.phrasevoice.data.tts.CloudTtsService
import com.phrasevoice.data.tts.OfflineSherpaTtsProvider

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val dataStore = applicationContext.phraseVoiceDataStore
    private val apiKeyCipher = ApiKeyCipher()

    val phraseRepository = PhraseRepository(dataStore)
    val historyRepository = HistoryRepository(dataStore)
    val settingsRepository = SettingsRepository(dataStore)
    val providerConfigRepository = ProviderConfigRepository(dataStore, apiKeyCipher)
    val audioFileStore = AudioFileStore(applicationContext)
    val offlineVoiceModelStore = OfflineVoiceModelStore(applicationContext)
    val audioClipRepository = AudioClipRepository(dataStore, audioFileStore)
    val systemTtsProvider = AndroidSystemTtsProvider(applicationContext)
    val offlineSherpaTtsProvider = OfflineSherpaTtsProvider(offlineVoiceModelStore, audioFileStore)
    val cloudTtsService = CloudTtsService(audioFileStore)
    val audioPlaybackController = AudioPlaybackController(applicationContext)
}
