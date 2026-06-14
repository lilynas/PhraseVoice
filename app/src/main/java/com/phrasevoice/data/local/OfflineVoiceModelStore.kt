package com.phrasevoice.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.phrasevoice.data.model.OfflineVoiceModel
import com.phrasevoice.data.repository.OfflineVoiceModelMetadata
import java.io.File
import java.util.UUID

class OfflineVoiceModelStore(private val context: Context) {
    private val modelDir: File = File(context.filesDir, "offline_voice_models").also { directory ->
        directory.mkdirs()
    }

    fun importModel(uri: Uri, now: Long = System.currentTimeMillis()): OfflineVoiceModel {
        val resolver = context.contentResolver
        val displayName = displayNameFor(uri) ?: "offline-voice-model"
        val fileName = "${UUID.randomUUID()}-${displayName.sanitizeFileName()}"
        val targetFile = File(modelDir, fileName)

        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open model package" }
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val sizeBytes = targetFile.length()
        val status = OfflineVoiceModelMetadata.statusFor(displayName, sizeBytes)
        return OfflineVoiceModel(
            id = UUID.randomUUID().toString(),
            name = displayName,
            fileName = fileName,
            engine = OfflineVoiceModel.ENGINE_SHERPA_ONNX,
            language = OfflineVoiceModelMetadata.languageFor(displayName),
            voiceName = OfflineVoiceModelMetadata.voiceNameFor(displayName),
            status = status,
            sizeBytes = sizeBytes,
            importedAt = now,
            updatedAt = now,
            note = when (status) {
                OfflineVoiceModel.STATUS_AVAILABLE -> null
                OfflineVoiceModel.STATUS_CORRUPT -> "Model package is empty."
                OfflineVoiceModel.STATUS_INCOMPATIBLE -> "Unsupported package type."
                else -> null
            },
        )
    }

    fun deleteModel(fileName: String) {
        runCatching { File(modelDir, fileName).delete() }
    }

    private fun displayNameFor(uri: Uri): String? =
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .ifBlank { "offline-voice-model" }
}
