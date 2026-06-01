package com.phrasevoice.data.tts

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class AndroidTtsEngine(
    val name: String,
    val label: String,
)

class AndroidSystemTtsProvider(
    context: Context,
) : TtsProvider {
    override val id: String = "android_system"
    override val displayName: String = "Android System TTS"
    override val supportsDirectPlayback: Boolean = true
    override val supportsFileOutput: Boolean = true

    private val initStatus = CompletableDeferred<Int>()
    private val fileRequests = ConcurrentHashMap<String, PendingFileRequest>()
    private val textToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (!initStatus.isCompleted) initStatus.complete(status)
    }

    init {
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
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
            },
        )
    }

    override suspend fun listVoices(): List<TtsVoice> = withContext(Dispatchers.Default) {
        if (!awaitReady()) return@withContext defaultVoice()

        val voices = textToSpeech.voices.orEmpty()
        if (voices.isEmpty()) return@withContext defaultVoice()

        voices
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
    }

    fun listEngines(): List<AndroidTtsEngine> =
        textToSpeech.engines.orEmpty().map { engine ->
            AndroidTtsEngine(name = engine.name, label = engine.label)
        }

    override suspend fun synthesize(request: TtsRequest): TtsResult = withContext(Dispatchers.Main) {
        if (!awaitReady()) {
            return@withContext TtsResult.Error("Android TextToSpeech is not ready.")
        }

        applyRequestOptions(request)
        val utteranceId = UUID.randomUUID().toString()
        val result = textToSpeech.speak(
            request.text,
            TextToSpeech.QUEUE_FLUSH,
            request.toSpeechBundle(),
            utteranceId,
        )

        if (result == TextToSpeech.SUCCESS) {
            TtsResult.LocalPlaybackStarted(utteranceId)
        } else {
            TtsResult.Error("Android TextToSpeech could not start playback.")
        }
    }

    suspend fun synthesizeToFile(
        request: TtsRequest,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult = withContext(Dispatchers.Main) {
        if (!awaitReady()) {
            return@withContext TtsResult.Error("Android TextToSpeech is not ready.")
        }
        if (request.outputFormat != AudioFormat.WAV) {
            return@withContext TtsResult.Error("Android System TTS currently exports WAV audio.")
        }

        file.parentFile?.mkdirs()
        val utteranceId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<TtsResult>()
        fileRequests[utteranceId] = PendingFileRequest(deferred, uri, mimeType)

        applyRequestOptions(request)
        val result = textToSpeech.synthesizeToFile(
            request.text,
            request.toSpeechBundle(),
            file,
            utteranceId,
        )

        if (result != TextToSpeech.SUCCESS) {
            fileRequests.remove(utteranceId)
            return@withContext TtsResult.Error("Android TextToSpeech could not create the audio file.")
        }

        withTimeoutOrNull(FILE_SYNTHESIS_TIMEOUT_MS) { deferred.await() }
            ?: run {
                fileRequests.remove(utteranceId)
                TtsResult.Error("Timed out while generating audio.")
            }
    }

    override fun stop() {
        textToSpeech.stop()
    }

    private suspend fun awaitReady(): Boolean {
        val status = withTimeoutOrNull(INIT_TIMEOUT_MS) { initStatus.await() }
        return status == TextToSpeech.SUCCESS
    }

    private fun applyRequestOptions(request: TtsRequest) {
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

    companion object {
        private const val INIT_TIMEOUT_MS = 10_000L
        private const val FILE_SYNTHESIS_TIMEOUT_MS = 60_000L
    }
}
