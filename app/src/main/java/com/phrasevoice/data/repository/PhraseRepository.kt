package com.phrasevoice.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.local.PhraseVoicePreferenceKeys
import com.phrasevoice.data.local.safeData
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.model.PhraseGroup
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class PhraseLibrary(
    val groups: List<PhraseGroup>,
    val phrases: List<Phrase>,
)

class PhraseRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val groups: Flow<List<PhraseGroup>> = dataStore.safeData()
        .map { preferences ->
            PhraseVoiceJson.decode(
                preferences[PhraseVoicePreferenceKeys.PHRASE_GROUPS],
                defaultGroups(),
            ).sortedBy { it.sortOrder }
        }

    val phrases: Flow<List<Phrase>> = dataStore.safeData()
        .map { preferences ->
            PhraseVoiceJson.decode<List<Phrase>>(
                preferences[PhraseVoicePreferenceKeys.PHRASES],
                emptyList(),
            ).sortedWith(compareBy<Phrase> { it.sortOrder }.thenByDescending { it.updatedAt })
        }

    val library: Flow<PhraseLibrary> = combine(groups, phrases) { groups, phrases ->
        PhraseLibrary(groups = groups, phrases = phrases)
    }

    suspend fun ensureSeedData() {
        val preferences = dataStore.data.first()
        val storedGroups = preferences[PhraseVoicePreferenceKeys.PHRASE_GROUPS]
        val storedPhrases = preferences[PhraseVoicePreferenceKeys.PHRASES]
        if (!storedGroups.isNullOrBlank() && !storedPhrases.isNullOrBlank()) return

        val now = System.currentTimeMillis()
        dataStore.edit { mutablePreferences ->
            if (storedGroups.isNullOrBlank()) {
                mutablePreferences[PhraseVoicePreferenceKeys.PHRASE_GROUPS] =
                    PhraseVoiceJson.encode(defaultGroups())
            }
            if (storedPhrases.isNullOrBlank()) {
                mutablePreferences[PhraseVoicePreferenceKeys.PHRASES] =
                    PhraseVoiceJson.encode(defaultPhrases(now))
            }
        }
    }

    suspend fun addPhrase(
        title: String,
        text: String,
        groupId: String = DEFAULT_GROUP_ID,
        isFavorite: Boolean = false,
    ) {
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<Phrase>>(
                preferences[PhraseVoicePreferenceKeys.PHRASES],
                emptyList(),
            )
            val nextSortOrder = (current.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val phrase = Phrase(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { text.take(24) },
                text = text,
                groupId = groupId,
                sortOrder = nextSortOrder,
                createdAt = now,
                updatedAt = now,
                isFavorite = isFavorite,
            )
            preferences[PhraseVoicePreferenceKeys.PHRASES] = PhraseVoiceJson.encode(current + phrase)
        }
    }

    suspend fun updatePhrase(
        id: String,
        title: String,
        text: String,
        groupId: String,
        isFavorite: Boolean,
    ) {
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<Phrase>>(
                preferences[PhraseVoicePreferenceKeys.PHRASES],
                emptyList(),
            )
            val updated = current.map { phrase ->
                if (phrase.id == id) {
                    phrase.copy(
                        title = title.ifBlank { text.take(24) },
                        text = text,
                        groupId = groupId,
                        isFavorite = isFavorite,
                        updatedAt = now,
                    )
                } else {
                    phrase
                }
            }
            preferences[PhraseVoicePreferenceKeys.PHRASES] = PhraseVoiceJson.encode(updated)
        }
    }

    suspend fun deletePhrase(id: String) {
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<Phrase>>(
                preferences[PhraseVoicePreferenceKeys.PHRASES],
                emptyList(),
            )
            preferences[PhraseVoicePreferenceKeys.PHRASES] =
                PhraseVoiceJson.encode(current.filterNot { it.id == id })
        }
    }

    suspend fun toggleFavorite(id: String) {
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<Phrase>>(
                preferences[PhraseVoicePreferenceKeys.PHRASES],
                emptyList(),
            )
            preferences[PhraseVoicePreferenceKeys.PHRASES] = PhraseVoiceJson.encode(
                current.map { phrase ->
                    if (phrase.id == id) phrase.copy(isFavorite = !phrase.isFavorite) else phrase
                },
            )
        }
    }

    suspend fun touchPhrase(id: String) {
        val now = System.currentTimeMillis()
        dataStore.edit { preferences ->
            val current = PhraseVoiceJson.decode<List<Phrase>>(
                preferences[PhraseVoicePreferenceKeys.PHRASES],
                emptyList(),
            )
            preferences[PhraseVoicePreferenceKeys.PHRASES] = PhraseVoiceJson.encode(
                current.map { phrase ->
                    if (phrase.id == id) {
                        phrase.copy(lastUsedAt = now, updatedAt = now)
                    } else {
                        phrase
                    }
                },
            )
        }
    }

    companion object {
        const val DEFAULT_GROUP_ID = "default"

        fun defaultGroups(): List<PhraseGroup> =
            listOf(PhraseGroup(id = DEFAULT_GROUP_ID, name = "常用", sortOrder = 0))

        fun defaultPhrases(now: Long): List<Phrase> =
            listOf(
                Phrase(
                    id = "seed_hello",
                    title = "问候",
                    text = "你好，很高兴见到你。",
                    groupId = DEFAULT_GROUP_ID,
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now,
                    isFavorite = true,
                ),
                Phrase(
                    id = "seed_thanks",
                    title = "感谢",
                    text = "谢谢你的帮助。",
                    groupId = DEFAULT_GROUP_ID,
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now,
                    isFavorite = true,
                ),
                Phrase(
                    id = "seed_later",
                    title = "稍后回复",
                    text = "我现在不太方便，稍后回复你。",
                    groupId = DEFAULT_GROUP_ID,
                    sortOrder = 2,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
    }
}
