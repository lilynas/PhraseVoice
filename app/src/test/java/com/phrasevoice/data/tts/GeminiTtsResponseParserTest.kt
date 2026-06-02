package com.phrasevoice.data.tts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiTtsResponseParserTest {
    @Test
    fun parseAudio_extractsInlineDataFromRestResponse() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "inlineData": {
                          "mimeType": "audio/L16;codec=pcm;rate=24000",
                          "data": "AQIDBA=="
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val audio = GeminiTtsResponseParser.parseAudio(json)

        assertArrayEquals(byteArrayOf(1, 2, 3, 4), audio.pcmBytes)
        assertEquals("audio/L16;codec=pcm;rate=24000", audio.sourceMimeType)
    }

    @Test
    fun parseAudio_supportsSnakeCaseInlineData() {
        val json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "inline_data": {
                          "mime_type": "audio/L16",
                          "data": "BQY="
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val audio = GeminiTtsResponseParser.parseAudio(json)

        assertArrayEquals(byteArrayOf(5, 6), audio.pcmBytes)
        assertEquals("audio/L16", audio.sourceMimeType)
    }

    @Test
    fun parseAudio_throwsWhenAudioMissing() {
        val json = """{"candidates":[{"content":{"parts":[{"text":"no audio"}]}}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            GeminiTtsResponseParser.parseAudio(json)
        }
    }
}
