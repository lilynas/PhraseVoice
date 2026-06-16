package com.phrasevoice.data.tts

import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKittenModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.local.OfflineVoiceModelLayout
import com.phrasevoice.data.local.OfflineVoiceModelStore
import com.phrasevoice.data.local.OfflineVoiceModelType
import com.phrasevoice.data.model.OfflineVoiceModel
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import com.phrasevoice.domain.tts.TtsVoice
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class OfflineSherpaTtsProvider(
    private val modelStore: OfflineVoiceModelStore,
    private val audioFileStore: AudioFileStore,
) {
    private val lock = Any()
    private var loaded: LoadedOfflineTts? = null

    fun listVoices(models: List<OfflineVoiceModel>): List<TtsVoice> =
        modelStore.availableModels(models).map { model ->
            TtsVoice(
                id = model.id,
                name = model.name,
                language = model.language.ifBlank { null },
                description = listOf(model.voiceName, model.engine)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                providerId = ProviderConfigRepository.OFFLINE_SHERPA,
            )
        }

    suspend fun synthesize(
        request: TtsRequest,
        models: List<OfflineVoiceModel>,
        cache: Boolean,
    ): TtsResult = withContext(Dispatchers.IO) {
        runCatching {
            val availableModels = modelStore.availableModels(models)
            val model = request.voiceId
                ?.let { voiceId -> availableModels.firstOrNull { it.id == voiceId } }
                ?: availableModels.firstOrNull()
                ?: return@runCatching TtsResult.Error("请先在设置中导入可用的离线语音包。")
            val layout = modelStore.inspectModel(model.fileName)
                ?: return@runCatching TtsResult.Error("离线语音包不可用，请重新导入模型包。")
            val tts = loadTts(model, layout)
            val audio = tts.generateWithConfig(
                text = request.text,
                config = GenerationConfig(
                    speed = request.speed.coerceIn(0.5f, 2.0f),
                    sid = speakerIdFromVoiceId(request.voiceId),
                    extra = supertonicExtra(layout, request.language),
                ),
            )
            if (audio.samples.isEmpty()) {
                return@runCatching TtsResult.Error("离线语音没有生成音频。")
            }
            val target = audioFileStore.createTarget(AudioFormat.WAV, cache = cache)
            PcmWavWriter.writeWav(
                file = target.file,
                pcmBytes = audio.samples.toPcm16Bytes(request.volume),
                sampleRate = audio.sampleRate.takeIf { it > 0 } ?: DEFAULT_SAMPLE_RATE,
            )
            TtsResult.AudioFile(uri = target.uri, mimeType = target.mimeType)
        }.getOrElse { throwable ->
            AppLogger.e(TAG, "offline sherpa synthesize failed", throwable)
            TtsResult.Error("离线语音合成失败：${throwable.message ?: "未知错误"}", throwable)
        }
    }

    fun stop() {
        synchronized(lock) {
            loaded?.tts?.release()
            loaded = null
        }
    }

    private fun loadTts(model: OfflineVoiceModel, layout: OfflineVoiceModelLayout): OfflineTts =
        synchronized(lock) {
            val current = loaded
            if (current != null && current.modelId == model.id && current.updatedAt == model.updatedAt) {
                return@synchronized current.tts
            }
            current?.tts?.release()
            val config = configFor(layout)
            val next = LoadedOfflineTts(
                modelId = model.id,
                updatedAt = model.updatedAt,
                tts = OfflineTts(config = config),
            )
            loaded = next
            next.tts
        }

    private fun configFor(layout: OfflineVoiceModelLayout): OfflineTtsConfig {
        val model = when (layout.type) {
            OfflineVoiceModelType.Vits -> OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = layout.requireModelFile().absolutePath,
                    lexicon = layout.lexiconFiles.joinPaths(),
                    tokens = layout.requireTokensFile().absolutePath,
                    dataDir = layout.dataDir?.absolutePath.orEmpty(),
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            )

            OfflineVoiceModelType.Kokoro -> OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = layout.requireModelFile().absolutePath,
                    voices = layout.requireVoicesFile().absolutePath,
                    tokens = layout.requireTokensFile().absolutePath,
                    dataDir = layout.dataDir?.absolutePath.orEmpty(),
                    lexicon = layout.lexiconFiles.joinPaths(),
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu",
            )

            OfflineVoiceModelType.Kitten -> OfflineTtsModelConfig(
                kitten = OfflineTtsKittenModelConfig(
                    model = layout.requireModelFile().absolutePath,
                    voices = layout.requireVoicesFile().absolutePath,
                    tokens = layout.requireTokensFile().absolutePath,
                    dataDir = layout.dataDir?.absolutePath.orEmpty(),
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu",
            )

            OfflineVoiceModelType.Matcha -> OfflineTtsModelConfig(
                matcha = OfflineTtsMatchaModelConfig(
                    acousticModel = layout.requireAcousticModelFile().absolutePath,
                    vocoder = layout.requireVocoderFile().absolutePath,
                    lexicon = layout.lexiconFiles.joinPaths(),
                    tokens = layout.requireTokensFile().absolutePath,
                    dataDir = layout.dataDir?.absolutePath.orEmpty(),
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            )

            OfflineVoiceModelType.Supertonic -> OfflineTtsModelConfig(
                supertonic = OfflineTtsSupertonicModelConfig(
                    durationPredictor = layout.requireDurationPredictorFile().absolutePath,
                    textEncoder = layout.requireTextEncoderFile().absolutePath,
                    vectorEstimator = layout.requireVectorEstimatorFile().absolutePath,
                    vocoder = layout.requireVocoderFile().absolutePath,
                    ttsJson = layout.requireTtsJsonFile().absolutePath,
                    unicodeIndexer = layout.requireUnicodeIndexerFile().absolutePath,
                    voiceStyle = layout.requireVoiceStyleFile().absolutePath,
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            )
        }
        return OfflineTtsConfig(
            model = model,
            ruleFsts = layout.ruleFstFiles.joinPaths(),
            ruleFars = layout.ruleFarFiles.joinPaths(),
        )
    }

    private fun speakerIdFromVoiceId(voiceId: String?): Int =
        voiceId
            ?.substringAfter('#', missingDelimiterValue = "")
            ?.toIntOrNull()
            ?.coerceAtLeast(0)
            ?: 0

    private fun supertonicExtra(layout: OfflineVoiceModelLayout, language: String?): Map<String, String>? {
        if (layout.type != OfflineVoiceModelType.Supertonic) return null
        val lang = language
            ?.substringBefore('-')
            ?.takeIf { it.isNotBlank() }
            ?: "en"
        return mapOf("lang" to lang)
    }

    private fun FloatArray.toPcm16Bytes(volume: Float): ByteArray {
        val normalizedVolume = volume.coerceIn(0f, 1f)
        val output = ByteArray(size * 2)
        forEachIndexed { index, sample ->
            val pcm = (sample * normalizedVolume)
                .coerceIn(-1f, 1f)
                .times(Short.MAX_VALUE)
                .roundToInt()
            output[index * 2] = (pcm and 0xff).toByte()
            output[index * 2 + 1] = (pcm shr 8 and 0xff).toByte()
        }
        return output
    }

    private fun List<File>.joinPaths(): String =
        joinToString(",") { it.absolutePath }

    private fun OfflineVoiceModelLayout.requireModelFile(): File =
        requireNotNull(modelFile) { "Missing model file" }

    private fun OfflineVoiceModelLayout.requireAcousticModelFile(): File =
        requireNotNull(acousticModelFile) { "Missing acoustic model file" }

    private fun OfflineVoiceModelLayout.requireVocoderFile(): File =
        requireNotNull(vocoderFile) { "Missing vocoder file" }

    private fun OfflineVoiceModelLayout.requireVoicesFile(): File =
        requireNotNull(voicesFile) { "Missing voices file" }

    private fun OfflineVoiceModelLayout.requireTokensFile(): File =
        requireNotNull(tokensFile) { "Missing tokens file" }

    private fun OfflineVoiceModelLayout.requireDurationPredictorFile(): File =
        requireNotNull(durationPredictorFile) { "Missing duration predictor file" }

    private fun OfflineVoiceModelLayout.requireTextEncoderFile(): File =
        requireNotNull(textEncoderFile) { "Missing text encoder file" }

    private fun OfflineVoiceModelLayout.requireVectorEstimatorFile(): File =
        requireNotNull(vectorEstimatorFile) { "Missing vector estimator file" }

    private fun OfflineVoiceModelLayout.requireTtsJsonFile(): File =
        requireNotNull(ttsJsonFile) { "Missing tts.json" }

    private fun OfflineVoiceModelLayout.requireUnicodeIndexerFile(): File =
        requireNotNull(unicodeIndexerFile) { "Missing unicode indexer file" }

    private fun OfflineVoiceModelLayout.requireVoiceStyleFile(): File =
        requireNotNull(voiceStyleFile) { "Missing voice style file" }

    private data class LoadedOfflineTts(
        val modelId: String,
        val updatedAt: Long,
        val tts: OfflineTts,
    )

    companion object {
        private const val TAG = "OfflineSherpaTts"
        private const val DEFAULT_SAMPLE_RATE = 24_000
    }
}
