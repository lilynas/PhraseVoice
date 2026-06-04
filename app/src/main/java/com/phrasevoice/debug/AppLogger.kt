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
    private val defaultConfig = LoggerConfig(
        enabled = true,
        minLevel = LogLevel.INFO,
    )
    private val _entries = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    @Volatile
    private var config = defaultConfig

    val entries: StateFlow<List<DebugLogEntry>> = _entries.asStateFlow()

    fun v(tag: String, message: String) {
        log(LogLevel.VERBOSE, tag, message)
    }

    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }

    fun w(tag: String, message: String) {
        log(LogLevel.WARN, tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    fun configure(enabled: Boolean, minLevel: String) {
        config = LoggerConfig(
            enabled = enabled,
            minLevel = LogLevel.fromValue(minLevel),
        )
    }

    fun clear() {
        _entries.value = emptyList()
        if (shouldLog(LogLevel.INFO)) {
            Log.i("PhraseVoice", "Debug logs cleared")
        }
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (!shouldLog(level)) return
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message)
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> if (throwable == null) {
                Log.e(tag, message)
            } else {
                Log.e(tag, message, throwable)
            }
        }
        append(level.shortName, tag, listOfNotNull(message, throwable?.message).joinToString(" | "))
    }

    private fun shouldLog(level: LogLevel): Boolean {
        val current = config
        return current.enabled && level.priority >= current.minLevel.priority
    }

    private fun append(level: String, tag: String, message: String) {
        _entries.update { current ->
            (current + DebugLogEntry(System.currentTimeMillis(), level, tag, message))
                .takeLast(MAX_ENTRIES)
        }
    }

    private data class LoggerConfig(
        val enabled: Boolean,
        val minLevel: LogLevel,
    )

    private enum class LogLevel(
        val value: String,
        val shortName: String,
        val priority: Int,
    ) {
        VERBOSE("VERBOSE", "V", 0),
        DEBUG("DEBUG", "D", 10),
        INFO("INFO", "I", 20),
        WARN("WARN", "W", 30),
        ERROR("ERROR", "E", 40);

        companion object {
            fun fromValue(value: String): LogLevel =
                enumValues<LogLevel>().firstOrNull { it.value.equals(value, ignoreCase = true) } ?: INFO
        }
    }
}
