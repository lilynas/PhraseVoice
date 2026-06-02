package com.phrasevoice.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DebugLogEntry(
    val timestampMillis: Long,
    val level: String,
    val tag: String,
    val message: String,
)

object AppLogger {
    private const val MAX_ENTRIES = 120
    private val _entries = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val entries: StateFlow<List<DebugLogEntry>> = _entries.asStateFlow()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        append("D", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append("I", tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        append("W", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        append("E", tag, listOfNotNull(message, throwable?.message).joinToString(" | "))
    }

    fun clear() {
        _entries.value = emptyList()
        Log.i("PhraseVoice", "Debug logs cleared")
    }

    private fun append(level: String, tag: String, message: String) {
        _entries.update { current ->
            (current + DebugLogEntry(System.currentTimeMillis(), level, tag, message))
                .takeLast(MAX_ENTRIES)
        }
    }
}
