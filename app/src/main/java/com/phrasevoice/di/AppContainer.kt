package com.phrasevoice.di

import android.content.Context
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.local.phraseVoiceDataStore
import com.phrasevoice.data.repository.HistoryRepository
import com.phrasevoice.data.repository.PhraseRepository
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.tts.AndroidSystemTtsProvider

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext
    private val dataStore = applicationContext.phraseVoiceDataStore

    val phraseRepository = PhraseRepository(dataStore)
    val historyRepository = HistoryRepository(dataStore)
    val settingsRepository = SettingsRepository(dataStore)
    val audioFileStore = AudioFileStore(applicationContext)
    val systemTtsProvider = AndroidSystemTtsProvider(applicationContext)
}
