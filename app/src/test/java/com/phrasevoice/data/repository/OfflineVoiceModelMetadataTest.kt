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
            OfflineVoiceModelMetadata.statusFor("vits-piper-zh_CN-chaowen-medium.tar.bz2", 2048L),
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
        assertEquals(
            "vits piper zh CN chaowen medium",
            OfflineVoiceModelMetadata.voiceNameFor("vits-piper-zh_CN-chaowen-medium.tar.bz2"),
        )
    }

    @Test
    fun recommendedDownloadCatalog_usesImportablePackages() {
        assertEquals(3, OfflineVoiceDownloadCatalog.recommended.size)
        OfflineVoiceDownloadCatalog.recommended.forEach { item ->
            assertEquals(true, item.downloadUrl.startsWith("https://"))
            assertEquals(true, item.docsUrl.startsWith("https://"))
            assertEquals(
                OfflineVoiceModel.STATUS_AVAILABLE,
                OfflineVoiceModelMetadata.statusFor(item.downloadUrl.substringAfterLast('/'), 1024L),
            )
        }
    }
}
