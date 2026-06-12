package com.phrasevoice.data.repository

import com.phrasevoice.data.model.OfflineVoiceModel
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineVoiceModelMetadataTest {
    @Test
    fun statusFor_acceptsSupportedModelPackages() {
        assertEquals(
            OfflineVoiceModel.STATUS_AVAILABLE,
            OfflineVoiceModelMetadata.statusFor("vits-melo-tts-zh_en.onnx", 1024L),
        )
        assertEquals(
            OfflineVoiceModel.STATUS_AVAILABLE,
            OfflineVoiceModelMetadata.statusFor("kokoro-model.tar.gz", 2048L),
        )
        assertEquals(
            OfflineVoiceModel.STATUS_AVAILABLE,
            OfflineVoiceModelMetadata.statusFor("piper-voice.zip", 2048L),
        )
    }

    @Test
    fun statusFor_marksEmptyOrUnsupportedPackages() {
        assertEquals(
            OfflineVoiceModel.STATUS_CORRUPT,
            OfflineVoiceModelMetadata.statusFor("voice.onnx", 0L),
        )
        assertEquals(
            OfflineVoiceModel.STATUS_INCOMPATIBLE,
            OfflineVoiceModelMetadata.statusFor("voice.txt", 120L),
        )
    }

    @Test
    fun metadataInfersUsefulLabelsFromFileName() {
        assertEquals("zh-CN", OfflineVoiceModelMetadata.languageFor("vits-melo-tts-zh_en.onnx"))
        assertEquals("en", OfflineVoiceModelMetadata.languageFor("kokoro-en-us.zip"))
        assertEquals("vits melo tts zh en", OfflineVoiceModelMetadata.voiceNameFor("vits-melo-tts-zh_en.onnx"))
        assertEquals("kokoro model", OfflineVoiceModelMetadata.voiceNameFor("kokoro-model.tar.gz"))
    }
}
