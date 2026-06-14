package com.phrasevoice.data.repository

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.local.PhraseVoicePreferenceKeys
import com.phrasevoice.data.local.safeData
import com.phrasevoice.data.model.AudioClip
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AudioClipRepository(
    private val dataStore: DataStore<Preferences>,
    private val audioFileStore: AudioFileStore,
) {
    val clips: Flow<List<AudioClip>> = dataStore.safeData()
        .map { preferences ->
            PhraseVoiceJson.decode<List<AudioClip>>(
                preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS],
                emptyList(),
            ).sortedWith(
                compareByDescending<AudioClip> { it.lastPlayedAt ?: 0L }
                    .thenBy { it.sortOrder },
            )
        }
        .distinctUntilChanged()

    suspend fun importClip(uri: Uri) {
        val imported = audioFileStore.importAudioClip(uri)
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<AudioClip>>(
                preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS],
                emptyList(),
            )
            val nextSortOrder = (current.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val clip = AudioClip(
                id = UUID.randomUUID().toString(),
                title = imported.title,
                fileName = imported.fileName,
                mimeType = imported.mimeType,
                sortOrder = nextSortOrder,
                createdAt = now,
                updatedAt = now,
            )
            preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS] = PhraseVoiceJson.encode(current + clip)
        }
    }

    suspend fun touchClip(id: String) {
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<AudioClip>>(
                preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS],
                emptyList(),
            )
            preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS] = PhraseVoiceJson.encode(
                current.map { clip ->
                    if (clip.id == id) clip.copy(lastPlayedAt = now, updatedAt = now) else clip
                },
            )
        }
    }

    suspend fun deleteClip(id: String) {
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<AudioClip>>(
                preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS],
                emptyList(),
            )
            val deleted = current.firstOrNull { it.id == id }
            if (deleted != null) {
                audioFileStore.deleteAudioClip(deleted.fileName)
            }
            preferences[PhraseVoicePreferenceKeys.AUDIO_CLIPS] =
                PhraseVoiceJson.encode(current.filterNot { it.id == id })
        }
    }
}
