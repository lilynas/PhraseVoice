package com.phrasevoice.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.phrasevoice.domain.model.AudioFormat
import java.io.File
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

class AudioFileStore(private val context: Context) {
    private val audioDir: File = File(context.filesDir, "audio").also { directory ->
        directory.mkdirs()
    }
    private val cacheAudioDir: File = File(context.cacheDir, "audio").also { directory ->
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

    private fun audioFiles(directory: File): List<File> =
        directory
            .listFiles()
            .orEmpty()
            .filter { it.isFile }
}
