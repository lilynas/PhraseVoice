package com.phrasevoice.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DATASTORE_NAME = "phrasevoice_state"

val Context.phraseVoiceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
)

object PhraseVoicePreferenceKeys {
    val PHRASES = stringPreferencesKey("phrases_json")
    val PHRASE_GROUPS = stringPreferencesKey("phrase_groups_json")
    val HISTORY = stringPreferencesKey("history_json")
    val USER_SETTINGS = stringPreferencesKey("user_settings_json")
    val PROVIDER_CONFIGS = stringPreferencesKey("provider_configs_json")
    val AUDIO_CLIPS = stringPreferencesKey("audio_clips_json")
}

object PhraseVoiceJson {
    val instance = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    inline fun <reified T> decode(value: String?, fallback: T): T {
        if (value.isNullOrBlank()) return fallback
        return runCatching { instance.decodeFromString<T>(value) }.getOrDefault(fallback)
    }

    inline fun <reified T> encode(value: T): String = instance.encodeToString(value)
}

fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { throwable ->
        if (throwable is IOException) {
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }
