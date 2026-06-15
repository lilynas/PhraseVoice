package com.phrasevoice.data.local

import java.io.File
import java.util.Locale

enum class OfflineVoiceModelType(val label: String) {
    Vits("VITS / Piper"),
    Kokoro("Kokoro"),
    Kitten("Kitten"),
    Matcha("Matcha"),
    Supertonic("Supertonic"),
}

data class OfflineVoiceModelLayout(
    val root: File,
    val type: OfflineVoiceModelType,
    val modelFile: File? = null,
    val acousticModelFile: File? = null,
    val vocoderFile: File? = null,
    val voicesFile: File? = null,
    val tokensFile: File? = null,
    val dataDir: File? = null,
    val lexiconFiles: List<File> = emptyList(),
    val ruleFstFiles: List<File> = emptyList(),
    val ruleFarFiles: List<File> = emptyList(),
    val durationPredictorFile: File? = null,
    val textEncoderFile: File? = null,
    val vectorEstimatorFile: File? = null,
    val ttsJsonFile: File? = null,
    val unicodeIndexerFile: File? = null,
    val voiceStyleFile: File? = null,
) {
    val displayName: String =
        root.name.replace(Regex("^[0-9a-fA-F-]{36}-"), "")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { type.label }
}

object OfflineVoiceModelInspector {
    fun inspect(entry: File): OfflineVoiceModelLayout? {
        if (!entry.exists() || !entry.isDirectory) return null
        return entry
            .walkTopDown()
            .filter { it.isDirectory }
            .sortedBy { it.relativeTo(entry).invariantSeparatorsPath.length }
            .firstNotNullOfOrNull(::inspectDirectory)
    }

    private fun inspectDirectory(root: File): OfflineVoiceModelLayout? {
        val files = root.walkTopDown().filter { it.isFile }.toList()
        val directories = root.walkTopDown().filter { it.isDirectory }.toList()
        val onnxFiles = files.filter { it.extension.equals("onnx", ignoreCase = true) }
        val tokens = files.firstOrNull { it.name.equals("tokens.txt", ignoreCase = true) }
        val voices = files.firstOrNull { it.name.equals("voices.bin", ignoreCase = true) }
        val dataDir = directories.firstOrNull { it.name.equals("espeak-ng-data", ignoreCase = true) }
        val lexicons = files
            .filter {
                it.extension.equals("txt", ignoreCase = true) &&
                    it.name.lowercase(Locale.US).contains("lexicon")
            }
            .sortedBy { it.name }
        val ruleFsts = files
            .filter { it.extension.equals("fst", ignoreCase = true) }
            .sortedBy { it.name }
        val ruleFars = files
            .filter { it.extension.equals("far", ignoreCase = true) }
            .sortedBy { it.name }

        inspectSupertonic(root, files)?.let { return it }

        val rootName = root.name.lowercase(Locale.US)
        if (voices != null && tokens != null && onnxFiles.isNotEmpty()) {
            val modelFile = preferredOnnx(onnxFiles, "kokoro", "model") ?: onnxFiles.first()
            return OfflineVoiceModelLayout(
                root = root,
                type = if (rootName.contains("kitten")) OfflineVoiceModelType.Kitten else OfflineVoiceModelType.Kokoro,
                modelFile = modelFile,
                voicesFile = voices,
                tokensFile = tokens,
                dataDir = dataDir,
                lexiconFiles = lexicons,
                ruleFstFiles = ruleFsts,
                ruleFarFiles = ruleFars,
            )
        }

        inspectMatcha(root, onnxFiles, tokens, dataDir, lexicons, ruleFsts, ruleFars)?.let { return it }

        if (tokens != null && onnxFiles.isNotEmpty()) {
            val modelFile = onnxFiles
                .filterNot { it.name.lowercase(Locale.US).contains("vocoder") }
                .filterNot { it.name.lowercase(Locale.US).contains("vocos") }
                .minByOrNull { it.name.length }
                ?: onnxFiles.first()
            return OfflineVoiceModelLayout(
                root = root,
                type = OfflineVoiceModelType.Vits,
                modelFile = modelFile,
                tokensFile = tokens,
                dataDir = dataDir,
                lexiconFiles = lexicons,
                ruleFstFiles = ruleFsts,
                ruleFarFiles = ruleFars,
            )
        }

        return null
    }

    private fun inspectMatcha(
        root: File,
        onnxFiles: List<File>,
        tokens: File?,
        dataDir: File?,
        lexicons: List<File>,
        ruleFsts: List<File>,
        ruleFars: List<File>,
    ): OfflineVoiceModelLayout? {
        if (tokens == null || onnxFiles.size < 2) return null
        val acoustic = preferredOnnx(onnxFiles, "model-steps", "acoustic", "matcha")
        val vocoder = preferredOnnx(onnxFiles, "vocos", "vocoder")
        if (acoustic == null || vocoder == null || acoustic == vocoder) return null
        return OfflineVoiceModelLayout(
            root = root,
            type = OfflineVoiceModelType.Matcha,
            acousticModelFile = acoustic,
            vocoderFile = vocoder,
            tokensFile = tokens,
            dataDir = dataDir,
            lexiconFiles = lexicons,
            ruleFstFiles = ruleFsts,
            ruleFarFiles = ruleFars,
        )
    }

    private fun inspectSupertonic(root: File, files: List<File>): OfflineVoiceModelLayout? {
        val durationPredictor = files.firstByNamePart("duration_predictor", "onnx")
        val textEncoder = files.firstByNamePart("text_encoder", "onnx")
        val vectorEstimator = files.firstByNamePart("vector_estimator", "onnx")
        val vocoder = files.firstByNamePart("vocoder", "onnx")
        val ttsJson = files.firstOrNull { it.name.equals("tts.json", ignoreCase = true) }
        val unicodeIndexer = files.firstOrNull { it.name.equals("unicode_indexer.bin", ignoreCase = true) }
        val voiceStyle = files.firstOrNull { it.name.equals("voice.bin", ignoreCase = true) }
        if (
            durationPredictor == null ||
            textEncoder == null ||
            vectorEstimator == null ||
            vocoder == null ||
            ttsJson == null ||
            unicodeIndexer == null ||
            voiceStyle == null
        ) {
            return null
        }
        return OfflineVoiceModelLayout(
            root = root,
            type = OfflineVoiceModelType.Supertonic,
            durationPredictorFile = durationPredictor,
            textEncoderFile = textEncoder,
            vectorEstimatorFile = vectorEstimator,
            vocoderFile = vocoder,
            ttsJsonFile = ttsJson,
            unicodeIndexerFile = unicodeIndexer,
            voiceStyleFile = voiceStyle,
        )
    }

    private fun preferredOnnx(files: List<File>, vararg nameParts: String): File? {
        val lowerParts = nameParts.map { it.lowercase(Locale.US) }
        return files
            .sortedBy { it.name.length }
            .firstOrNull { file ->
                val name = file.name.lowercase(Locale.US)
                lowerParts.any { it in name }
            }
    }

    private fun List<File>.firstByNamePart(namePart: String, extension: String): File? =
        firstOrNull {
            it.extension.equals(extension, ignoreCase = true) &&
                it.name.lowercase(Locale.US).contains(namePart)
        }
}
