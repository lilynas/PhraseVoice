package com.phrasevoice.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.local.PhraseVoicePreferenceKeys
import com.phrasevoice.data.local.safeData
import com.phrasevoice.data.model.TtsHistory
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val history: Flow<List<TtsHistory>> = dataStore.safeData()
        .map { preferences ->
            PhraseVoiceJson.decode<List<TtsHistory>>(
                preferences[PhraseVoicePreferenceKeys.HISTORY],
                emptyList(),
            ).sortedByDescending { it.createdAt }
        }

    suspend fun addHistory(
        text: String,
        providerId: String,
        voiceId: String?,
        audioUri: String? = null,
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val item = TtsHistory(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            providerId = providerId,
            voiceId = voiceId,
            createdAt = System.currentTimeMillis(),
            audioUri = audioUri,
        )

        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<TtsHistory>>(
                preferences[PhraseVoicePreferenceKeys.HISTORY],
                emptyList(),
            )
            preferences[PhraseVoicePreferenceKeys.HISTORY] =
                PhraseVoiceJson.encode((listOf(item) + current).take(MAX_HISTORY_ITEMS))
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences[PhraseVoicePreferenceKeys.HISTORY] = PhraseVoiceJson.encode(emptyList<TtsHistory>())
        }
    }

    companion object {
        private const val MAX_HISTORY_ITEMS = 200
    }
}
