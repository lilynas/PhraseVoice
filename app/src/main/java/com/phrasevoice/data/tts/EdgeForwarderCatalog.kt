package com.phrasevoice.data.tts

data class EdgeForwarderVoice(
    val id: String,
    val name: String,
    val locale: String,
)

data class EdgeForwarderStyle(
    val id: String,
    val name: String,
)

object EdgeForwarderCatalog {
    const val DEFAULT_VOICE_ID = "Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)"
    const val DEFAULT_STYLE_ID = ""

    val styles: List<EdgeForwarderStyle> = listOf(
        EdgeForwarderStyle(DEFAULT_STYLE_ID, "默认"),
        EdgeForwarderStyle("Friendly", "友好"),
        EdgeForwarderStyle("Positive", "乐观"),
        EdgeForwarderStyle("Warm", "温暖"),
        EdgeForwarderStyle("Lively", "活跃"),
        EdgeForwarderStyle("Passion", "热情"),
        EdgeForwarderStyle("Sunshine", "阳光"),
        EdgeForwarderStyle("Humorious", "幽默"),
    )

    val voices: List<EdgeForwarderVoice> = listOf(
        EdgeForwarderVoice(DEFAULT_VOICE_ID, "晓晓", "zh-CN"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoyiNeural)", "晓伊", "zh-CN"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN, YunjianNeural)", "云健", "zh-CN"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN, YunxiNeural)", "云希", "zh-CN"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN, YunxiaNeural)", "云夏", "zh-CN"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN, YunyangNeural)", "云扬", "zh-CN"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN-liaoning, XiaobeiNeural)", "晓北", "zh-CN-liaoning"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-CN-shaanxi, XiaoniNeural)", "晓妮", "zh-CN-shaanxi"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-HK, HiuGaaiNeural)", "晓佳", "zh-HK"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-HK, HiuMaanNeural)", "晓曼", "zh-HK"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-HK, WanLungNeural)", "云龙", "zh-HK"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-TW, HsiaoChenNeural)", "晓臻", "zh-TW"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-TW, HsiaoYuNeural)", "晓雨", "zh-TW"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (zh-TW, YunJheNeural)", "云哲", "zh-TW"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (en-US, JennyNeural)", "Jenny", "en-US"),
        EdgeForwarderVoice("Microsoft Server Speech Text to Speech Voice (en-US, GuyNeural)", "Guy", "en-US"),
    )

    fun voiceName(voiceId: String): String =
        voices.firstOrNull { it.id == voiceId }?.let { "${it.name} ${it.locale}" } ?: voiceId
}
