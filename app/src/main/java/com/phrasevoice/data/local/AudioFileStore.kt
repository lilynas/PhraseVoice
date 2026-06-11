package com.phrasevoice.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.phrasevoice.domain.model.AudioFormat
import java.io.File
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AudioTarget(
    val file: File,
    val uri: Uri,
    val mimeType: String,
)

data class AudioCacheInfo(
    val fileCount: Int,
    val totalBytes: Long,
)

data class ImportedAudioFile(
    val title: String,
    val fileName: String,
    val mimeType: String,
)

class AudioFileStore(private val context: Context) {
    private val audioDir: File = File(context.filesDir, "audio").also { directory ->
        directory.mkdirs()
    }
    private val cacheAudioDir: File = File(context.cacheDir, "audio").also { directory ->
        directory.mkdirs()
    }
    private val importedAudioDir: File = File(context.filesDir, "audio_clips").also { directory ->
        directory.mkdirs()
    }

    fun createTarget(format: AudioFormat, cache: Boolean = false): AudioTarget {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val directory = if (cache) cacheAudioDir else audioDir
        val file = File(directory, "phrasevoice_$timestamp.${format.extension}")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return AudioTarget(file = file, uri = uri, mimeType = format.mimeType)
    }

    fun cacheInfo(): AudioCacheInfo {
        val files = audioFiles(cacheAudioDir)
        return AudioCacheInfo(
            fileCount = files.size,
            totalBytes = files.sumOf { it.length() },
        )
    }

    fun clearCache(): AudioCacheInfo {
        audioFiles(cacheAudioDir).forEach { file ->
            runCatching { file.delete() }
        }
        return cacheInfo()
    }

    fun importAudioClip(uri: Uri): ImportedAudioFile {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "audio/*"
        val displayName = displayNameFor(uri) ?: "audio_clip"
        val extension = extensionFor(displayName, mimeType)
        val fileName = "${UUID.randomUUID()}$extension"
        val targetFile = File(importedAudioDir, fileName)

        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open audio file" }
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return ImportedAudioFile(
            title = displayName.substringBeforeLast('.').ifBlank { "Audio Clip" },
            fileName = fileName,
            mimeType = mimeType,
        )
    }

    fun audioClipUri(fileName: String): Uri = Uri.fromFile(File(importedAudioDir, fileName))

    fun deleteAudioClip(fileName: String) {
        runCatching { File(importedAudioDir, fileName).delete() }
    }

    private fun audioFiles(directory: File): List<File> =
        directory
            .listFiles()
            .orEmpty()
            .filter { it.isFile }

    private fun displayNameFor(uri: Uri): String? =
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }

    private fun extensionFor(displayName: String, mimeType: String): String {
        val fromName = displayName.substringAfterLast('.', missingDelimiterValue = "")
        if (fromName.isNotBlank() && fromName.length <= 8) return ".$fromName"
        return when (mimeType.lowercase(Locale.US)) {
            "audio/mpeg", "audio/mp3" -> ".mp3"
            "audio/wav", "audio/x-wav" -> ".wav"
            "audio/aac" -> ".aac"
            "audio/ogg" -> ".ogg"
            "audio/flac" -> ".flac"
            "audio/mp4", "audio/m4a" -> ".m4a"
            else -> ".audio"
        }
    }
}
