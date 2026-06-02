package com.phrasevoice.data.tts

import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class MimoTtsAudio(
    val audioBytes: ByteArray,
)

object MimoTtsResponseParser {
    fun parseAudio(json: String): MimoTtsAudio {
        val root = Json.parseToJsonElement(json).jsonObject
        val audioObject = root["choices"]
            ?.jsonArray
            ?.asSequence()
            ?.mapNotNull { choice ->
                val choiceObject = choice.jsonObject
                choiceObject.audioObject("message") ?: choiceObject.audioObject("delta")
            }
            ?.firstOrNull { audio ->
                !audio["data"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()
            }
            ?: throw IllegalArgumentException("MiMo response did not include message audio data.")

        val base64Audio = audioObject["data"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("MiMo message audio data is empty.")

        return MimoTtsAudio(audioBytes = Base64.getDecoder().decode(base64Audio))
    }

    fun parseStreamPcm(stream: String): ByteArray {
        val output = ByteArrayOutputStream()
        stream.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                when {
                    line.startsWith("data:", ignoreCase = true) -> line.substringAfter(":").trim()
                    line.startsWith("{") -> line
                    else -> null
                }
            }
            .filterNot { it == "[DONE]" }
            .forEach { payload ->
                runCatching { parseAudio(payload).audioBytes }
                    .getOrNull()
                    ?.let { output.write(it) }
            }

        if (output.size() == 0) {
            throw IllegalArgumentException("MiMo stream did not include audio data.")
        }
        return output.toByteArray()
    }

    private fun JsonObject.audioObject(messageKey: String): JsonObject? =
        this[messageKey]
            ?.jsonObject
            ?.get("audio")
            ?.jsonObject
}
