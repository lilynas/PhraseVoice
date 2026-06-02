package com.phrasevoice.data.tts

data class GeminiTtsVoice(
    val id: String,
    val tone: String,
)

object GeminiTtsCatalog {
    const val DEFAULT_VOICE_ID = "Kore"

    val voices: List<GeminiTtsVoice> = listOf(
        GeminiTtsVoice("Zephyr", "明亮"),
        GeminiTtsVoice("Puck", "欢快"),
        GeminiTtsVoice("Charon", "信息丰富"),
        GeminiTtsVoice("Kore", "坚定"),
        GeminiTtsVoice("Fenrir", "兴奋"),
        GeminiTtsVoice("Leda", "青春"),
        GeminiTtsVoice("Orus", "公司感"),
        GeminiTtsVoice("Aoede", "轻快"),
        GeminiTtsVoice("Callirrhoe", "轻松"),
        GeminiTtsVoice("Autonoe", "明亮"),
        GeminiTtsVoice("Enceladus", "气声"),
        GeminiTtsVoice("Iapetus", "清晰"),
        GeminiTtsVoice("Umbriel", "轻松自在"),
        GeminiTtsVoice("Algieba", "平滑"),
        GeminiTtsVoice("Despina", "平滑"),
        GeminiTtsVoice("Erinome", "清澈"),
        GeminiTtsVoice("Algenib", "沙哑"),
        GeminiTtsVoice("Rasalgethi", "信息丰富"),
        GeminiTtsVoice("Laomedeia", "欢快"),
        GeminiTtsVoice("Achernar", "柔和"),
        GeminiTtsVoice("Alnilam", "坚定"),
        GeminiTtsVoice("Schedar", "平稳"),
        GeminiTtsVoice("Gacrux", "成熟"),
        GeminiTtsVoice("Pulcherrima", "前倾"),
        GeminiTtsVoice("Achird", "友好"),
        GeminiTtsVoice("Zubenelgenubi", "随意"),
        GeminiTtsVoice("Vindemiatrix", "温柔"),
        GeminiTtsVoice("Sadachbia", "活泼"),
        GeminiTtsVoice("Sadaltager", "知识渊博"),
        GeminiTtsVoice("Sulafat", "偏高"),
    )
}
