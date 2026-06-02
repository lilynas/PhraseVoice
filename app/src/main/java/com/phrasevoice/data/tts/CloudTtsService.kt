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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

    private fun findJsonString(json: String, path: String): String? {
        var current: JsonElement = kotlinx.serialization.json.Json.parseToJsonElement(json)
        path.split(".").filter { it.isNotBlank() }.forEach { key ->
            current = (current as? JsonObject)?.get(key) ?: return null
        }
        return current.jsonPrimitive.contentOrNull
    }

    private fun okhttp3.Response.toTtsError(): TtsResult.Error {
        val message = when (code) {
            401 -> "认证失败：请检查 API Key。"
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
