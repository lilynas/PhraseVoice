package com.phrasevoice.data.tts

import android.net.Uri
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.model.CustomHttpResponseType
import com.phrasevoice.data.model.CustomHttpSettings
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.RuntimeProviderConfig
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.domain.model.AudioFormat
import com.phrasevoice.domain.tts.TtsRequest
import com.phrasevoice.domain.tts.TtsResult
import java.io.File
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.roundToInt

class CloudTtsService(
    private val audioFileStore: AudioFileStore,
    private val httpClient: OkHttpClient = OkHttpClient(),
) {
    private val renderer = CustomTemplateRenderer()

    suspend fun synthesize(
        request: TtsRequest,
        runtimeConfig: RuntimeProviderConfig,
        cache: Boolean = false,
    ): TtsResult = withContext(Dispatchers.IO) {
        AppLogger.i(
            TAG,
            "cloud synthesize provider=${runtimeConfig.config.providerId} model=${runtimeConfig.config.model.orEmpty()} voice=${request.voiceId.orEmpty()} length=${request.text.length}",
        )
        when (runtimeConfig.config.providerId) {
            ProviderConfigRepository.OPENAI -> synthesizeOpenAiCompatible(request, runtimeConfig, cache)
            ProviderConfigRepository.EDGE_TTS_FORWARDER -> synthesizeEdgeTtsForwarder(request, runtimeConfig, cache)
            ProviderConfigRepository.GEMINI -> synthesizeGeminiTts(request, runtimeConfig, cache)
            ProviderConfigRepository.MIMO -> synthesizeMimoTts(request, runtimeConfig, cache)
            ProviderConfigRepository.CUSTOM_HTTP -> synthesizeCustomHttp(request, runtimeConfig, cache)
            else -> TtsResult.Error("该 Provider 暂未接入。")
        }
    }

    private fun synthesizeOpenAiCompatible(
        request: TtsRequest,
        runtimeConfig: RuntimeProviderConfig,
        cache: Boolean,
    ): TtsResult {
        val apiKey = runtimeConfig.apiKey
        if (apiKey.isNullOrBlank()) return TtsResult.Error("请先在 Provider 页面保存 API Key。")
        val baseUrl = runtimeConfig.config.baseUrl?.takeIf { it.isNotBlank() }
            ?: return TtsResult.Error("请先配置 OpenAI-compatible Base URL。")
        AppLogger.i(TAG, "openai request url=${baseUrl.safeUrlForLog()}")
        val format = AudioFormat.MP3
        val target = audioFileStore.createTarget(format, cache = cache)
        val body = JsonObject(
            buildMap {
                put("model", JsonPrimitive(runtimeConfig.config.model ?: "gpt-4o-mini-tts"))
                put("input", JsonPrimitive(request.text))
                put("voice", JsonPrimitive(request.voiceId ?: runtimeConfig.config.defaultVoice ?: "alloy"))
                put("response_format", JsonPrimitive(format.extension))
                put("speed", JsonPrimitive(request.speed.coerceIn(0.25f, 4.0f)))
                request.stylePrompt?.takeIf { it.isNotBlank() }?.let {
                    put("instructions", JsonPrimitive(it))
                }
            },
        ).toString()
        val httpRequest = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return executeAudioRequest(httpRequest, target.file, target.uri, target.mimeType)
    }

    private fun synthesizeEdgeTtsForwarder(
        request: TtsRequest,
        runtimeConfig: RuntimeProviderConfig,
        cache: Boolean,
    ): TtsResult {
        val config = runtimeConfig.config
        val baseUrl = config.baseUrl?.takeIf { it.isNotBlank() }
            ?: return TtsResult.Error("请先配置 Edge TTS Forwarder Base URL。")
        val endpoint = edgeForwarderEndpoint(baseUrl)
            ?: return TtsResult.Error("Edge TTS Forwarder Base URL 无效。")
        val format = AudioFormat.MP3
        val target = audioFileStore.createTarget(format, cache = cache)
        val url = endpoint.newBuilder()
            .addQueryParameter("voice", request.voiceId ?: config.defaultVoice ?: EdgeForwarderCatalog.DEFAULT_VOICE_ID)
            .addQueryParameter("volume", edgeVolume(request.volume).toString())
            .addQueryParameter("rate", edgeProsodyPercent(request.speed).toString())
            .addQueryParameter("pitch", edgeProsodyPercent(request.pitch).toString())
            .addQueryParameter("text", request.text)
            .apply {
                request.stylePrompt
                    ?.takeIf { it.isNotBlank() }
                    ?.let { style -> addQueryParameter("personality", style) }
            }
            .build()
        AppLogger.i(TAG, "edge forwarder request url=${url.toString().safeUrlForLog()}")

        val builder = Request.Builder().url(url).get()
        runtimeConfig.apiKey
            ?.takeIf { it.isNotBlank() }
            ?.let { token -> builder.addHeader("Authorization", "Bearer $token") }

        return executeAudioRequest(builder.build(), target.file, target.uri, target.mimeType)
    }

    private fun synthesizeGeminiTts(
        request: TtsRequest,
        runtimeConfig: RuntimeProviderConfig,
        cache: Boolean,
    ): TtsResult {
        val apiKey = runtimeConfig.apiKey
        if (apiKey.isNullOrBlank()) return TtsResult.Error("请先在 Provider 页面保存 Gemini API Key。")
        val config = runtimeConfig.config
        val model = config.model?.takeIf { it.isNotBlank() } ?: "gemini-3.1-flash-tts-preview"
        val baseUrl = config.baseUrl?.takeIf { it.isNotBlank() }
            ?: return TtsResult.Error("请先配置 Gemini Base URL。")
        val endpoint = geminiGenerateContentEndpoint(baseUrl, model)
            ?: return TtsResult.Error("Gemini Base URL 无效。")
        val voice = request.voiceId ?: config.defaultVoice ?: GeminiTtsCatalog.DEFAULT_VOICE_ID
        val target = audioFileStore.createTarget(AudioFormat.WAV, cache = cache)
        val prompt = request.stylePrompt
            ?.takeIf { it.isNotBlank() }
            ?.let { "朗读下面文字。风格要求：$it\n\n${request.text}" }
            ?: request.text
        val body = JsonObject(
            mapOf(
                "contents" to JsonArray(
                    listOf(
                        JsonObject(
                            mapOf(
                                "parts" to JsonArray(
                                    listOf(
                                        JsonObject(mapOf("text" to JsonPrimitive(prompt))),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                "generationConfig" to JsonObject(
                    mapOf(
                        "responseModalities" to JsonArray(listOf(JsonPrimitive("AUDIO"))),
                        "speechConfig" to JsonObject(
                            mapOf(
                                "voiceConfig" to JsonObject(
                                    mapOf(
                                        "prebuiltVoiceConfig" to JsonObject(
                                            mapOf("voiceName" to JsonPrimitive(voice)),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                "model" to JsonPrimitive(model),
            ),
        ).toString()
        AppLogger.i(TAG, "gemini request url=${endpoint.toString().safeUrlForLog()} voice=$voice")
        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return executeGeminiAudioRequest(httpRequest, target.file, target.uri, target.mimeType)
    }

    private fun synthesizeMimoTts(
        request: TtsRequest,
        runtimeConfig: RuntimeProviderConfig,
        cache: Boolean,
    ): TtsResult {
        val apiKey = runtimeConfig.apiKey
        if (apiKey.isNullOrBlank()) return TtsResult.Error("请先在 Provider 页面保存 MiMo API Key。")
        val config = runtimeConfig.config
        val model = config.model?.takeIf { it.isNotBlank() } ?: MimoTtsCatalog.PRESET_MODEL_ID
        val baseUrl = config.baseUrl?.takeIf { it.isNotBlank() }
            ?: return TtsResult.Error("请先配置 MiMo Base URL。")
        val endpoint = mimoChatCompletionsEndpoint(baseUrl)
            ?: return TtsResult.Error("MiMo Base URL 无效。")
        val target = audioFileStore.createTarget(AudioFormat.WAV, cache = cache)
        val voiceDesign = MimoTtsCatalog.isVoiceDesignModel(model)
        val voiceOrDescription = request.voiceId ?: config.defaultVoice
        val userInstruction = if (voiceDesign) {
            voiceOrDescription?.takeIf { it.isNotBlank() }
                ?: return TtsResult.Error("请先填写 MiMo VoiceDesign 音色描述。")
        } else {
            request.stylePrompt.orEmpty()
        }
        val messages = buildList {
            userInstruction.takeIf { it.isNotBlank() }?.let { instruction ->
                add(
                    JsonObject(
                        mapOf(
                            "role" to JsonPrimitive("user"),
                            "content" to JsonPrimitive(instruction),
                        ),
                    ),
                )
            }
            add(
                JsonObject(
                    mapOf(
                        "role" to JsonPrimitive("assistant"),
                        "content" to JsonPrimitive(request.text),
                    ),
                ),
            )
        }
        val audio = if (voiceDesign) {
            JsonObject(
                mapOf(
                    "format" to JsonPrimitive(AudioFormat.WAV.extension),
                    "optimize_text_preview" to JsonPrimitive(false),
                ),
            )
        } else {
            JsonObject(
                mapOf(
                    "format" to JsonPrimitive(AudioFormat.WAV.extension),
                    "voice" to JsonPrimitive(
                        voiceOrDescription?.takeIf { it.isNotBlank() } ?: MimoTtsCatalog.DEFAULT_VOICE_ID,
                    ),
                ),
            )
        }
        val body = JsonObject(
            mapOf(
                "model" to JsonPrimitive(model),
                "messages" to JsonArray(messages),
                "audio" to audio,
            ),
        ).toString()
        AppLogger.i(TAG, "mimo request url=${endpoint.toString().safeUrlForLog()} model=$model voiceDesign=$voiceDesign")
        val httpRequest = Request.Builder()
            .url(endpoint)
            .addHeader("api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return executeMimoAudioRequest(httpRequest, target.file, target.uri, target.mimeType)
    }

    private fun synthesizeCustomHttp(
        request: TtsRequest,
        runtimeConfig: RuntimeProviderConfig,
        cache: Boolean,
    ): TtsResult {
        val config = runtimeConfig.config
        val baseUrl = config.baseUrl?.takeIf { it.isNotBlank() }
            ?: return TtsResult.Error("请先配置 Custom HTTP Base URL。")
        AppLogger.i(TAG, "custom request url=${baseUrl.safeUrlForLog()}")
        val settings = com.phrasevoice.data.local.PhraseVoiceJson.decode(
            config.extraJson,
            CustomHttpSettings(),
        )
        val format = request.outputFormat
        val target = audioFileStore.createTarget(format, cache = cache)
        val variables = mapOf(
            "apiKey" to runtimeConfig.apiKey.orEmpty(),
            "text" to request.text,
            "voice" to (request.voiceId ?: config.defaultVoice.orEmpty()),
            "speed" to "%.2f".format(request.speed),
            "pitch" to "%.2f".format(request.pitch),
            "stylePrompt" to request.stylePrompt.orEmpty(),
            "format" to format.extension,
            "model" to config.model.orEmpty(),
        )
        val bodyText = renderer.render(settings.bodyTemplate, variables)
        val builder = Request.Builder().url(baseUrl)
        settings.headersTemplate
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.contains(":") }
            .forEach { line ->
                val name = line.substringBefore(":").trim()
                val value = renderer.render(line.substringAfter(":").trim(), variables)
                if (!name.equals("Authorization", ignoreCase = true) || value.isNotBlank()) {
                    builder.addHeader(name, value)
                }
            }
        val method = settings.method.uppercase()
        if (method == "GET") {
            builder.get()
        } else {
            builder.method(method, bodyText.toRequestBody(JSON_MEDIA_TYPE))
        }

        return when (settings.responseType) {
            CustomHttpResponseType.RAW_AUDIO -> executeAudioRequest(
                builder.build(),
                target.file,
                target.uri,
                target.mimeType,
            )

            CustomHttpResponseType.JSON_BASE64_FIELD -> executeJsonBase64Request(
                builder.build(),
                settings.responseField,
                target.file,
                target.uri,
                target.mimeType,
            )

            CustomHttpResponseType.JSON_URL_FIELD -> executeJsonUrlRequest(
                builder.build(),
                settings.responseField,
                target.file,
                target.uri,
                target.mimeType,
            )
        }
    }

    private fun executeAudioRequest(
        request: Request,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult =
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                AppLogger.i(
                    TAG,
                    "audio response code=${response.code} contentType=${response.body?.contentType()} url=${request.url.toString().safeUrlForLog()}",
                )
                if (!response.isSuccessful) return response.toTtsError()
                val body = response.body ?: return TtsResult.Error("服务没有返回音频内容。")
                file.outputStream().use { output -> body.byteStream().copyTo(output) }
                AppLogger.i(TAG, "audio saved bytes=${file.length()} uri=$uri")
                TtsResult.AudioFile(uri = uri, mimeType = response.body?.contentType()?.toString() ?: mimeType)
            }
        }.getOrElse { throwable ->
            AppLogger.e(TAG, "audio request failed url=${request.url.toString().safeUrlForLog()}", throwable)
            TtsResult.Error("网络请求失败：${throwable.message ?: "未知错误"}", throwable)
        }

    private fun executeJsonBase64Request(
        request: Request,
        fieldPath: String,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult =
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                AppLogger.i(TAG, "json base64 response code=${response.code} url=${request.url.toString().safeUrlForLog()}")
                if (!response.isSuccessful) return response.toTtsError()
                val json = response.body?.string() ?: return TtsResult.Error("服务没有返回 JSON。")
                val base64Audio = findJsonString(json, fieldPath)
                    ?: return TtsResult.Error("JSON 中没有找到字段：$fieldPath")
                file.writeBytes(Base64.getDecoder().decode(base64Audio))
                AppLogger.i(TAG, "json base64 audio saved bytes=${file.length()} uri=$uri")
                TtsResult.AudioFile(uri = uri, mimeType = mimeType)
            }
        }.getOrElse { throwable ->
            TtsResult.Error("解析音频响应失败：${throwable.message ?: "未知错误"}", throwable)
        }

    private fun executeJsonUrlRequest(
        request: Request,
        fieldPath: String,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult =
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                AppLogger.i(TAG, "json url response code=${response.code} url=${request.url.toString().safeUrlForLog()}")
                if (!response.isSuccessful) return response.toTtsError()
                val json = response.body?.string() ?: return TtsResult.Error("服务没有返回 JSON。")
                val audioUrl = findJsonString(json, fieldPath)
                    ?: return TtsResult.Error("JSON 中没有找到字段：$fieldPath")
                val downloadRequest = Request.Builder().url(audioUrl).get().build()
                return executeAudioRequest(downloadRequest, file, uri, mimeType)
            }
        }.getOrElse { throwable ->
            TtsResult.Error("下载音频失败：${throwable.message ?: "未知错误"}", throwable)
        }

    private fun executeGeminiAudioRequest(
        request: Request,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult =
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                AppLogger.i(TAG, "gemini response code=${response.code} url=${request.url.toString().safeUrlForLog()}")
                if (!response.isSuccessful) return response.toTtsError()
                val json = response.body?.string() ?: return TtsResult.Error("Gemini 没有返回 JSON。")
                val audio = GeminiTtsResponseParser.parseAudio(json)
                PcmWavWriter.writeWav(file, audio.pcmBytes)
                AppLogger.i(
                    TAG,
                    "gemini wav saved bytes=${file.length()} sourceMime=${audio.sourceMimeType.orEmpty()} uri=$uri",
                )
                TtsResult.AudioFile(uri = uri, mimeType = mimeType)
            }
        }.getOrElse { throwable ->
            AppLogger.e(TAG, "gemini request failed url=${request.url.toString().safeUrlForLog()}", throwable)
            TtsResult.Error("解析 Gemini 音频响应失败：${throwable.message ?: "未知错误"}", throwable)
        }

    private fun executeMimoAudioRequest(
        request: Request,
        file: File,
        uri: Uri,
        mimeType: String,
    ): TtsResult =
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                AppLogger.i(TAG, "mimo response code=${response.code} url=${request.url.toString().safeUrlForLog()}")
                if (!response.isSuccessful) return response.toTtsError()
                val json = response.body?.string() ?: return TtsResult.Error("MiMo 没有返回 JSON。")
                val audio = MimoTtsResponseParser.parseAudio(json)
                file.writeBytes(audio.audioBytes)
                AppLogger.i(TAG, "mimo wav saved bytes=${file.length()} uri=$uri")
                TtsResult.AudioFile(uri = uri, mimeType = mimeType)
            }
        }.getOrElse { throwable ->
            AppLogger.e(TAG, "mimo request failed url=${request.url.toString().safeUrlForLog()}", throwable)
            TtsResult.Error("解析 MiMo 音频响应失败：${throwable.message ?: "未知错误"}", throwable)
        }

    private fun findJsonString(json: String, path: String): String? {
        var current: JsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
        path.split(".").filter { it.isNotBlank() }.forEach { key ->
            current = (current as? JsonObject)?.get(key) ?: return null
        }
        return current.jsonPrimitive.contentOrNull
    }

    private fun edgeForwarderEndpoint(baseUrl: String): HttpUrl? {
        val parsed = baseUrl.trim().toHttpUrlOrNull() ?: return null
        if (parsed.encodedPath.trimEnd('/').endsWith("/api/text-to-speech")) {
            return parsed
        }
        val rootPath = parsed.encodedPath.trimEnd('/')
        val apiPath = if (rootPath.isBlank()) {
            "/api/text-to-speech"
        } else {
            "$rootPath/api/text-to-speech"
        }
        return parsed.newBuilder().encodedPath(apiPath).build()
    }

    private fun geminiGenerateContentEndpoint(baseUrl: String, model: String): HttpUrl? {
        val parsed = baseUrl.trim().toHttpUrlOrNull() ?: return null
        val cleanModel = model.trim().trim('/')
        if (cleanModel.isBlank()) return null
        val currentPath = parsed.encodedPath.trimEnd('/')
        val generatePath = when {
            currentPath.endsWith(":generateContent") -> currentPath
            currentPath.contains("/models/") -> "$currentPath:generateContent"
            currentPath.endsWith("/models") -> "$currentPath/$cleanModel:generateContent"
            currentPath.isBlank() || currentPath == "/" -> "/models/$cleanModel:generateContent"
            else -> "$currentPath/models/$cleanModel:generateContent"
        }
        return parsed.newBuilder().encodedPath(generatePath).build()
    }

    private fun mimoChatCompletionsEndpoint(baseUrl: String): HttpUrl? {
        val parsed = baseUrl.trim().toHttpUrlOrNull() ?: return null
        if (parsed.encodedPath.trimEnd('/').endsWith("/chat/completions")) {
            return parsed
        }
        val rootPath = parsed.encodedPath.trimEnd('/')
        val apiPath = if (rootPath.isBlank()) {
            "/chat/completions"
        } else {
            "$rootPath/chat/completions"
        }
        return parsed.newBuilder().encodedPath(apiPath).build()
    }

    private fun edgeProsodyPercent(value: Float): Int =
        ((value.coerceIn(0.5f, 2.0f) - 1.0f) * 100).roundToInt().coerceIn(-100, 100)

    private fun edgeVolume(value: Float): Int =
        (value.coerceIn(0.0f, 1.0f) * 100).roundToInt().coerceIn(0, 100)

    private fun okhttp3.Response.toTtsError(): TtsResult.Error {
        val message = when (code) {
            401 -> "认证失败：请检查 API Key 或 Token。"
            402 -> "账户余额或配额不足。"
            404 -> "接口地址不存在，请检查 Base URL。"
            429 -> "请求过于频繁或额度耗尽。"
            in 500..599 -> "服务端暂时不可用：HTTP $code。"
            else -> "请求失败：HTTP $code。"
        }
        AppLogger.w(TAG, "request error code=$code url=${request.url.toString().safeUrlForLog()}")
        return TtsResult.Error(message)
    }

    companion object {
        private const val TAG = "CloudTtsService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun String.safeUrlForLog(): String =
    runCatching {
        val uri = java.net.URI(this)
        buildString {
            append(uri.scheme ?: "https")
            append("://")
            append(uri.host ?: "unknown-host")
            uri.path?.takeIf { it.isNotBlank() }?.let(::append)
        }
    }.getOrDefault("<invalid-url>")
