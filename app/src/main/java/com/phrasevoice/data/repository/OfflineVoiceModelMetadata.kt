package com.phrasevoice.data.repository

import com.phrasevoice.data.model.OfflineVoiceModel
import java.util.Locale

object OfflineVoiceModelMetadata {
    private val supportedExtensions = setOf("onnx", "zip", "tar", "tgz", "tar.gz", "tar.bz2")

    fun statusFor(displayName: String, sizeBytes: Long): String {
        if (sizeBytes <= 0L) return OfflineVoiceModel.STATUS_CORRUPT
        val extension = extensionFor(displayName)
        return if (extension in supportedExtensions) {
            OfflineVoiceModel.STATUS_AVAILABLE
        } else {
            OfflineVoiceModel.STATUS_INCOMPATIBLE
        }
    }

    fun voiceNameFor(displayName: String): String =
        displayName
            .substringBeforeLast('.', displayName)
            .removeSuffix(".tar")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { "Offline Voice" }

    fun languageFor(displayName: String): String {
        val lower = displayName.lowercase(Locale.US)
        return when {
            lower.contains("zh") || lower.contains("cn") || lower.contains("chinese") -> "zh-CN"
            lower.contains("en") || lower.contains("english") -> "en"
            lower.contains("ja") || lower.contains("jp") || lower.contains("japanese") -> "ja"
            lower.contains("ko") || lower.contains("korean") -> "ko"
            else -> ""
        }
    }

    fun extensionFor(displayName: String): String {
        val lower = displayName.lowercase(Locale.US)
        if (lower.endsWith(".tar.gz")) return "tar.gz"
        if (lower.endsWith(".tar.bz2")) return "tar.bz2"
        return lower.substringAfterLast('.', missingDelimiterValue = "")
    }
}
