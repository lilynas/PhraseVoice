package com.phrasevoice.data.tts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MimoTtsResponseParserTest {
    @Test
    fun parseAudio_extractsMessageAudioData() {
        val json = """
            {
              "choices": [
                {
                  "message": {
                    "audio": {
                      "data": "AQIDBA=="
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val audio = MimoTtsResponseParser.parseAudio(json)

        assertArrayEquals(byteArrayOf(1, 2, 3, 4), audio.audioBytes)
    }

    @Test
    fun parseAudio_supportsDeltaAudioData() {
        val json = """
            {
              "choices": [
                {
                  "delta": {
                    "audio": {
                      "data": "BQY="
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val audio = MimoTtsResponseParser.parseAudio(json)

        assertArrayEquals(byteArrayOf(5, 6), audio.audioBytes)
    }

    @Test
    fun parseAudio_throwsWhenAudioMissing() {
        val json = """{"choices":[{"message":{"content":"no audio"}}]}"""

        assertThrows(IllegalArgumentException::class.java) {
            MimoTtsResponseParser.parseAudio(json)
        }
    }

    @Test
    fun parseStreamPcm_concatenatesDeltaAudioChunks() {
        val stream = """
            data: {"choices":[{"delta":{"audio":{"data":"AQI="}}}]}
            data: {"choices":[{"delta":{"content":"ignored"}}]}
            data: {"choices":[{"delta":{"audio":{"data":"AwQ="}}}]}
            data: [DONE]
        """.trimIndent()

        val pcm = MimoTtsResponseParser.parseStreamPcm(stream)

        assertArrayEquals(byteArrayOf(1, 2, 3, 4), pcm)
    }

    @Test
    fun parseStreamPcm_throwsWhenAudioMissing() {
        val stream = """
            data: {"choices":[{"delta":{"content":"ignored"}}]}
            data: [DONE]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            MimoTtsResponseParser.parseStreamPcm(stream)
        }
    }
}
