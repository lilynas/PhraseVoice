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

class AudioFileStore(private val context: Context) {
    private val audioDir: File = File(context.filesDir, "audio").also { directory ->
        directory.mkdirs()
    }

    fun createTarget(format: AudioFormat): AudioTarget {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(audioDir, "phrasevoice_$timestamp.${format.extension}")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return AudioTarget(file = file, uri = uri, mimeType = format.mimeType)
    }
}
