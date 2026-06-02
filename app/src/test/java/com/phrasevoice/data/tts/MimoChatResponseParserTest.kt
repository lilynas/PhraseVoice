package com.phrasevoice.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MimoChatResponseParserTest {
    @Test
    fun parseText_extractsMessageContent() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "content": "一位清澈温柔的年轻女性声音。"
                  }
                }
              ]
            }
        """.trimIndent()

        val text = MimoChatResponseParser.parseText(json)

        assertEquals("一位清澈温柔的年轻女性声音。", text)
    }

    @Test
    fun parseText_extractsTextFromContentArray() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "content": [
                      {"type": "text", "text": "第一句。"},
                      {"type": "text", "text": "第二句。"}
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val text = MimoChatResponseParser.parseText(json)

        assertEquals("第一句。\n第二句。", text)
    }

    @Test
    fun parseText_cleansCodeFenceAndQuotes() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "content": "```\n“低沉、稳定、带有故事感的男性旁白。”\n```"
                  }
                }
              ]
            }
        """.trimIndent()

        val text = MimoChatResponseParser.parseText(json)

        assertEquals("低沉、稳定、带有故事感的男性旁白。", text)
    }

    @Test
    fun parseText_throwsWhenContentMissing() {
        val json = """{"choices":[{"message":{"role":"assistant"}}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            MimoChatResponseParser.parseText(json)
        }
    }
}
