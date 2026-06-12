package com.phrasevoice.data.repository

data class OfflineVoiceDownloadItem(
    val id: String,
    val title: String,
    val language: String,
    val engine: String,
    val speakers: String,
    val sampleRate: String,
    val descriptionZh: String,
    val descriptionEn: String,
    val downloadUrl: String,
    val docsUrl: String,
)

object OfflineVoiceDownloadCatalog {
    const val OFFICIAL_CATALOG_URL = "https://k2-fsa.github.io/sherpa/onnx/tts/all/"

    val recommended: List<OfflineVoiceDownloadItem> = listOf(
        OfflineVoiceDownloadItem(
            id = "vits-piper-zh-cn-chaowen-medium",
            title = "vits-piper-zh_CN-chaowen-medium",
            language = "zh-CN",
            engine = "Piper / VITS",
            speakers = "1",
            sampleRate = "22050 Hz",
            descriptionZh = "中文中等体积模型，适合先验证离线朗读流程。",
            descriptionEn = "Medium Chinese voice, useful for validating the offline voice flow first.",
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            docsUrl = "https://k2-fsa.github.io/sherpa/onnx/tts/all/Chinese/vits-piper-zh_CN-chaowen-medium.html",
        ),
        OfflineVoiceDownloadItem(
            id = "kokoro-multi-lang-v1-1",
            title = "kokoro-multi-lang-v1_1",
            language = "zh-CN + en",
            engine = "Kokoro",
            speakers = "103",
            sampleRate = "24000 Hz",
            descriptionZh = "中英双语多声线模型，体积更大，适合后续高质量离线语音。",
            descriptionEn = "Chinese and English multi-speaker voice, larger but better for high quality offline speech.",
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_1.tar.bz2",
            docsUrl = "https://k2-fsa.github.io/sherpa/onnx/tts/all/Chinese-English/kokoro-multi-lang-v1_1.html",
        ),
        OfflineVoiceDownloadItem(
            id = "vits-piper-en-us-lessac-medium",
            title = "vits-piper-en_US-lessac-medium",
            language = "en-US",
            engine = "Piper / VITS",
            speakers = "1",
            sampleRate = "22050 Hz",
            descriptionZh = "英文中等体积模型，适合英文短语和展示文本。",
            descriptionEn = "Medium English voice for English phrases and display text.",
            downloadUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-lessac-medium.tar.bz2",
            docsUrl = "https://k2-fsa.github.io/sherpa/onnx/tts/all/English/vits-piper-en_US-lessac-medium.html",
        ),
    )
}
