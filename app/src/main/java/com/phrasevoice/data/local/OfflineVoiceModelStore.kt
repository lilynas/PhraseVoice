package com.phrasevoice.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.phrasevoice.data.model.OfflineVoiceModel
import com.phrasevoice.data.repository.OfflineVoiceModelMetadata
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

class OfflineVoiceModelStore(private val context: Context) {
    private val modelDir: File = File(context.filesDir, "offline_voice_models").also { directory ->
        directory.mkdirs()
    }

    fun importModel(uri: Uri, now: Long = System.currentTimeMillis()): OfflineVoiceModel {
        val resolver = context.contentResolver
        val displayName = displayNameFor(uri) ?: "offline-voice-model"
        val installName = "${UUID.randomUUID()}-${displayName.withoutKnownPackageExtension().sanitizeFileName()}"
        val targetDir = File(modelDir, installName)
        targetDir.mkdirs()

        runCatching {
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open model package" }
                installPackage(
                    input = BufferedInputStream(input),
                    displayName = displayName,
                    targetDir = targetDir,
                )
            }
        }.getOrElse { throwable ->
            targetDir.deleteRecursively()
            throw throwable
        }

        val sizeBytes = targetDir.totalSizeBytes()
        val layout = OfflineVoiceModelInspector.inspect(targetDir)
        val status = when {
            sizeBytes <= 0L -> OfflineVoiceModel.STATUS_CORRUPT
            layout != null -> OfflineVoiceModel.STATUS_AVAILABLE
            OfflineVoiceModelMetadata.isSupportedExtension(displayName) -> OfflineVoiceModel.STATUS_INCOMPATIBLE
            else -> OfflineVoiceModel.STATUS_INCOMPATIBLE
        }
        return OfflineVoiceModel(
            id = UUID.randomUUID().toString(),
            name = layout?.displayName ?: displayName.withoutKnownPackageExtension(),
            fileName = installName,
            engine = OfflineVoiceModel.ENGINE_SHERPA_ONNX,
            language = OfflineVoiceModelMetadata.languageFor(layout?.displayName ?: displayName),
            voiceName = layout?.type?.label ?: OfflineVoiceModelMetadata.voiceNameFor(displayName),
            status = status,
            sizeBytes = sizeBytes,
            importedAt = now,
            updatedAt = now,
            note = when (status) {
                OfflineVoiceModel.STATUS_AVAILABLE -> null
                OfflineVoiceModel.STATUS_CORRUPT -> "Model package is empty."
                OfflineVoiceModel.STATUS_INCOMPATIBLE -> "No supported sherpa-onnx TTS model layout was found."
                else -> null
            },
        )
    }

    fun deleteModel(fileName: String) {
        runCatching { File(modelDir, fileName).deleteRecursively() }
    }

    fun modelEntry(fileName: String): File = File(modelDir, fileName)

    fun inspectModel(fileName: String): OfflineVoiceModelLayout? =
        OfflineVoiceModelInspector.inspect(modelEntry(fileName))

    fun isModelUsable(model: OfflineVoiceModel): Boolean =
        model.status == OfflineVoiceModel.STATUS_AVAILABLE && inspectModel(model.fileName) != null

    fun availableModels(models: List<OfflineVoiceModel>): List<OfflineVoiceModel> =
        models.filter(::isModelUsable)

    private fun installPackage(
        input: InputStream,
        displayName: String,
        targetDir: File,
    ) {
        when (OfflineVoiceModelMetadata.extensionFor(displayName)) {
            "zip" -> extractZip(input, targetDir)
            "tar" -> extractTar(input, targetDir)
            "tar.gz", "tgz" -> extractTar(GZIPInputStream(input), targetDir)
            "tar.bz2" -> extractTar(BZip2CompressorInputStream(input), targetDir)
            else -> copySingleFile(input, File(targetDir, displayName.sanitizeFileName()))
        }
    }

    private fun extractZip(input: InputStream, targetDir: File) {
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = safeTarget(targetDir, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
    }

    private fun extractTar(input: InputStream, targetDir: File) {
        TarArchiveInputStream(input).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                val target = safeTarget(targetDir, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output -> tar.copyTo(output) }
                }
            }
        }
    }

    private fun copySingleFile(input: InputStream, targetFile: File) {
        targetFile.parentFile?.mkdirs()
        targetFile.outputStream().use { output -> input.copyTo(output) }
    }

    private fun safeTarget(targetDir: File, entryName: String): File {
        val root = targetDir.canonicalFile
        val target = File(root, entryName.replace('\\', '/')).canonicalFile
        require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
            "Unsafe model package entry: $entryName"
        }
        return target
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

    private fun String.withoutKnownPackageExtension(): String {
        val lower = lowercase()
        val suffix = listOf(".tar.bz2", ".tar.gz", ".tgz", ".zip", ".tar", ".onnx")
            .firstOrNull { lower.endsWith(it) }
        return if (suffix == null) this else dropLast(suffix.length)
    }

    private fun File.totalSizeBytes(): Long =
        if (isFile) {
            length()
        } else {
            walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
}
