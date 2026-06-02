package com.phrasevoice.data.tts

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MimoChatResponseParser {
    fun parseText(json: String): String {
        val root = Json.parseToJsonElement(json).jsonObject
        val firstChoice = root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?: throw IllegalArgumentException("MiMo response did not include choices.")

        val content = firstChoice["message"]
            ?.jsonObject
            ?.get("content")
            ?.asText()
            ?: firstChoice["delta"]
                ?.jsonObject
                ?.get("content")
                ?.asText()
            ?: throw IllegalArgumentException("MiMo response did not include text content.")

        return cleanup(content).takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("MiMo text content is empty.")
    }

    private fun JsonElement.asText(): String? =
        when (this) {
            is JsonPrimitive -> contentOrNull
            is JsonArray -> mapNotNull { item ->
                (item as? JsonObject)
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.contentOrNull
            }.joinToString(separator = "\n").takeIf { it.isNotBlank() }
            else -> null
        }

    private fun cleanup(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned
                .lineSequence()
                .drop(1)
                .toList()
                .dropLastWhile { it.trim() == "```" }
                .joinToString("\n")
                .trim()
        }
        return cleaned.trim('"', '“', '”', '\'', '‘', '’')
    }
}
