package com.phrasevoice.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.local.PhraseVoicePreferenceKeys
import com.phrasevoice.data.local.safeData
import com.phrasevoice.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<UserSettings> = dataStore.safeData()
        .map { preferences ->
            PhraseVoiceJson.decode(
                preferences[PhraseVoicePreferenceKeys.USER_SETTINGS],
                UserSettings(),
            )
        }

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode(
                preferences[PhraseVoicePreferenceKeys.USER_SETTINGS],
                UserSettings(),
            )
            preferences[PhraseVoicePreferenceKeys.USER_SETTINGS] =
                PhraseVoiceJson.encode(transform(current))
        }
    }

    suspend fun currentSettings(): UserSettings = settings.first()
}
