package com.phrasevoice.data.tts

import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class GeminiTtsAudio(
    val pcmBytes: ByteArray,
    val sourceMimeType: String?,
)

object GeminiTtsResponseParser {
    fun parseAudio(json: String): GeminiTtsAudio {
        val root = Json.parseToJsonElement(json).jsonObject
        val inlineData = root["candidates"]
            ?.jsonArray
            ?.asSequence()
            ?.mapNotNull { candidate ->
                candidate.jsonObject["content"]
                    ?.jsonObject
                    ?.get("parts")
                    ?.jsonArray
            }
            ?.flatMap { parts -> parts.asSequence() }
            ?.mapNotNull { part ->
                part.jsonObject["inlineData"]?.jsonObject ?: part.jsonObject["inline_data"]?.jsonObject
            }
            ?.firstOrNull { data ->
                !data["data"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()
            }
            ?: throw IllegalArgumentException("Gemini response did not include inline audio data.")

        val base64Audio = inlineData["data"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("Gemini inline audio data is empty.")
        val mimeType = inlineData.stringOrNull("mimeType") ?: inlineData.stringOrNull("mime_type")

        return GeminiTtsAudio(
            pcmBytes = Base64.getDecoder().decode(base64Audio),
            sourceMimeType = mimeType,
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
