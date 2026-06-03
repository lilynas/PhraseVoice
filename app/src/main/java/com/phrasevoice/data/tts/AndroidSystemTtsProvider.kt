package com.phrasevoice.data.tts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.TtsProvider
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import com.phrasevoice.domain.tts.TtsVoice
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class AndroidTtsEngine(
    val name: String,
    val label: String,
)

data class AndroidTtsReadiness(
    val ready: Boolean,
    val message: String? = null,
    val engines: List<AndroidTtsEngine> = emptyList(),
)

class AndroidSystemTtsProvider(
    context: Context,
) : TtsProvider {
    override val id: String = "android_system"
    override val displayName: String = "Android System TTS"
    override val supportsDirectPlayback: Boolean = true
    override val supportsFileOutput: Boolean = true

    private val appContext = context.applicationContext
    private val initializationMutex = Mutex()
    private val fileRequests = ConcurrentHashMap<String, PendingFileRequest>()
    private var activeTts: TtsHandle? = null
    private val utteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            val pending = utteranceId?.let(fileRequests::remove) ?: return
            pending.deferred.complete(
                TtsResult.AudioFile(uri = pending.uri, mimeType = pending.mimeType),
            )
        }

        @Deprecated("Deprecated in Android framework")
        override fun onError(utteranceId: String?) {
            completeWithError(utteranceId, "Android TTS failed while generating audio.")
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            completeWithError(
                utteranceId,
                "Android TTS failed while generating audio. Code: $errorCode",
            )
        }
    }

    override suspend fun listVoices(): List<TtsVoice> {
        if (!readiness().ready) return defaultVoice()

        return withContext(Dispatchers.Main) {
            val voices = activeTts?.textToSpeech?.voices.orEmpty()
            if (voices.isEmpty()) return@withContext defaultVoice()

            val engineVoices = voices
                .sortedWith(compareBy({ it.locale?.toLanguageTag().orEmpty() }, { it.name }))
                .map { voice ->
                    TtsVoice(
                        id = voice.name,
                        name = voice.name,
                        language = voice.locale?.toLanguageTag(),
                        description = if (voice.isNetworkConnectionRequired) {
                            "Network voice"
                        } else {
                            "System voice"
                        },
                        providerId = id,
                    )
                }
            defaultVoice() + engineVoices
        }
    }

    fun listEngines(): List<AndroidTtsEngine> {
        val packageManager = appContext.packageManager
        val services = runCatching {
            @Suppress("DEPRECATION")
            packageManager.queryIntentServices(
                Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),
                0,
            )
        }.getOrDefault(emptyList())

        val visibleEngines = services.mapNotNull { resolveInfo ->
            val serviceInfo = resolveInfo.serviceInfo ?: return@mapNotNull null
            AndroidTtsEngine(
                name = serviceInfo.packageName,
                label = resolveInfo.loadLabel(packageManager)?.toString() ?: serviceInfo.packageName,
            )
        }.distinctBy { it.name }
        if (visibleEngines.isNotEmpty()) return visibleEngines

        return activeTts?.textToSpeech?.engines.orEmpty().map { engine ->
            AndroidTtsEngine(name = engine.name, label = engine.label)
        }
    }

    suspend fun readiness(): AndroidTtsReadiness =
        initializationMutex.withLock {
            withContext(Dispatchers.Main) {
                ensureReadyLocked()
            }
        }

    override suspend fun synthesize(request: TtsRequest): TtsResult = withContext(Dispatchers.Main) {
        val readiness = readiness()
        if (!readiness.ready) {
            return@withContext TtsResult.Error(readiness.message ?: "Android 系统 TTS 暂不可用。")
        }

        val textToSpeech = activeTts?.textToSpeech
            ?: return@withContext TtsResult.Error(readiness.message ?: "Android 系统 TTS 暂不可用。")
        applyRequestOptions(textToSpeech, request)
        val utteranceId = UUID.randomUUID().toString()
        AppLogger.i(TAG, "speak request voice=${request.voiceId.orEmpty()} language=${request.language.orEmpty()} length=${request.text.length}")
        val result = textToSpeech.speak(
            request.text,
            TextToSpeech.QUEUE_FLUSH,
            request.toSpeechBundle(),
            utteranceId,
        )

        if (result == TextToSpeech.SUCCESS) {
            AppLogger.i(TAG, "speak started utteranceId=$utteranceId")
            TtsResult.LocalPlaybackStarted(utteranceId)
        } else {
            AppLogger.w(TAG, "speak start failed result=$result")
            TtsResult.Error("Android TextToSpeech could not start playback.")
        }
    }

    suspend fun synthesizeToFile(
        request: TtsRequest,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult = withContext(Dispatchers.Main) {
        val readiness = readiness()
        if (!readiness.ready) {
            return@withContext TtsResult.Error(readiness.message ?: "Android 系统 TTS 暂不可用。")
        }
        if (request.outputFormat != AudioFormat.WAV) {
            return@withContext TtsResult.Error("Android System TTS currently exports WAV audio.")
        }
        val textToSpeech = activeTts?.textToSpeech
            ?: return@withContext TtsResult.Error(readiness.message ?: "Android 系统 TTS 暂不可用。")

        file.parentFile?.mkdirs()
        val utteranceId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<TtsResult>()
        fileRequests[utteranceId] = PendingFileRequest(deferred, uri, mimeType)

        applyRequestOptions(textToSpeech, request)
        val result = textToSpeech.synthesizeToFile(
            request.text,
            request.toSpeechBundle(),
            file,
            utteranceId,
        )

        if (result != TextToSpeech.SUCCESS) {
            fileRequests.remove(utteranceId)
            AppLogger.w(TAG, "synthesizeToFile start failed result=$result")
            return@withContext TtsResult.Error("Android TextToSpeech could not create the audio file.")
        }

        withTimeoutOrNull(FILE_SYNTHESIS_TIMEOUT_MS) { deferred.await() }
            ?: run {
                fileRequests.remove(utteranceId)
                TtsResult.Error("Timed out while generating audio.")
            }
    }

    override fun stop() {
        activeTts?.textToSpeech?.stop()
    }

    private fun applyRequestOptions(textToSpeech: TextToSpeech, request: TtsRequest) {
        textToSpeech.setSpeechRate(request.speed.coerceIn(0.5f, 2.0f))
        textToSpeech.setPitch(request.pitch.coerceIn(0.5f, 2.0f))

        request.language
            ?.takeIf { it.isNotBlank() }
            ?.let(Locale::forLanguageTag)
            ?.let(textToSpeech::setLanguage)

        val matchingVoice = request.voiceId
            ?.takeIf { it.isNotBlank() }
            ?.let { voiceId -> textToSpeech.voices?.firstOrNull { it.name == voiceId } }
        if (matchingVoice != null) {
            textToSpeech.voice = matchingVoice
        }
    }

    private suspend fun ensureReadyLocked(): AndroidTtsReadiness {
        val engines = listEngines()
        val active = activeTts
        if (active != null) {
            val status = withTimeoutOrNull(ENGINE_INIT_TIMEOUT_MS) { active.initStatus.await() }
            when (status) {
                TextToSpeech.SUCCESS -> {
                    AppLogger.i(TAG, "readiness ready engine=${active.label} engines=${engines.map { it.name }}")
                    return AndroidTtsReadiness(ready = true, engines = engines)
                }
                null -> {
                    AppLogger.w(TAG, "active engine timeout engine=${active.label} engines=${engines.map { it.name }}")
                    shutdownHandle(active)
                }
                else -> {
                    AppLogger.w(TAG, "active engine failed engine=${active.label} status=$status engines=${engines.map { it.name }}")
                    shutdownHandle(active)
                }
            }
        }

        val attempted = mutableListOf<String>()
        engineCandidates(engines).forEach { engineName ->
            val label = engineName ?: "default"
            attempted += label
            AppLogger.i(TAG, "init attempt engine=$label engines=${engines.map { it.name }}")
            val handle = createTextToSpeech(engineName)
            val status = withTimeoutOrNull(ENGINE_INIT_TIMEOUT_MS) { handle.initStatus.await() }
            when (status) {
                TextToSpeech.SUCCESS -> {
                    activeTts = handle
                    AppLogger.i(TAG, "readiness ready engine=$label engines=${engines.map { it.name }}")
                    return AndroidTtsReadiness(ready = true, engines = engines)
                }
                null -> {
                    AppLogger.w(TAG, "init timeout engine=$label")
                    shutdownHandle(handle)
                }
                else -> {
                    AppLogger.w(TAG, "init failed engine=$label status=$status")
                    shutdownHandle(handle)
                }
            }
        }

        AppLogger.w(TAG, "readiness failed attempts=$attempted engines=${engines.map { it.name }}")
        return AndroidTtsReadiness(
            ready = false,
            message = "Android 系统 TTS 初始化失败/超时。已尝试：${attempted.joinToString()}。请在系统设置中确认默认语音引擎可用，或切换到 OpenAI TTS/Custom TTS API。",
            engines = engines,
        )
    }

    private fun createTextToSpeech(engineName: String?): TtsHandle {
        val initStatus = CompletableDeferred<Int>()
        val textToSpeech = if (engineName.isNullOrBlank()) {
            TextToSpeech(appContext) { status ->
                if (!initStatus.isCompleted) initStatus.complete(status)
            }
        } else {
            TextToSpeech(
                appContext,
                { status -> if (!initStatus.isCompleted) initStatus.complete(status) },
                engineName,
            )
        }
        textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener)
        return TtsHandle(
            textToSpeech = textToSpeech,
            engineName = engineName,
            initStatus = initStatus,
        )
    }

    private fun engineCandidates(engines: List<AndroidTtsEngine>): List<String?> {
        val preferredEngines = listOf("com.google.android.tts", "com.xiaomi.mibrain.speech")
        val engineNames = engines
            .map { it.name }
            .distinct()
            .sortedWith(
                compareBy<String> { name ->
                    preferredEngines.indexOf(name).takeIf { it >= 0 } ?: Int.MAX_VALUE
                }.thenBy { it },
            )
        return engineNames + listOf(null)
    }

    private fun shutdownHandle(handle: TtsHandle) {
        if (activeTts === handle) activeTts = null
        runCatching {
            handle.textToSpeech.stop()
            handle.textToSpeech.shutdown()
        }
    }

    private fun TtsRequest.toSpeechBundle(): Bundle =
        Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
        }

    private fun completeWithError(utteranceId: String?, message: String) {
        val pending = utteranceId?.let(fileRequests::remove) ?: return
        pending.deferred.complete(TtsResult.Error(message))
    }

    private fun defaultVoice(): List<TtsVoice> =
        listOf(
            TtsVoice(
                id = "",
                name = "System default",
                language = Locale.getDefault().toLanguageTag(),
                providerId = id,
            ),
        )

    private data class PendingFileRequest(
        val deferred: CompletableDeferred<TtsResult>,
        val uri: Uri,
        val mimeType: String,
    )

    private data class TtsHandle(
        val textToSpeech: TextToSpeech,
        val engineName: String?,
        val initStatus: CompletableDeferred<Int>,
    ) {
        val label: String
            get() = engineName ?: "default"
    }

    companion object {
        private const val TAG = "AndroidSystemTts"
        private const val ENGINE_INIT_TIMEOUT_MS = 5_000L
        private const val FILE_SYNTHESIS_TIMEOUT_MS = 60_000L
    }
}
