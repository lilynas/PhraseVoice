package com.phrasevoice.data.tts

data class MimoTtsModel(
    val id: String,
    val name: String,
    val description: String,
)

data class MimoTtsVoice(
    val id: String,
    val name: String,
    val language: String?,
    val gender: String?,
)

object MimoTtsCatalog {
    const val PRESET_MODEL_ID = "mimo-v2.5-tts"
    const val VOICE_DESIGN_MODEL_ID = "mimo-v2.5-tts-voicedesign"
    const val DEFAULT_PROMPT_OPTIMIZER_MODEL_ID = "mimo-v2.5"
    const val DEFAULT_VOICE_ID = "mimo_default"
    const val DEFAULT_VOICE_DESIGN_PROMPT =
        "一位温暖、清晰、亲切的中文旁白，语速自然，声音干净，有轻微的治愈感。"

    val models: List<MimoTtsModel> = listOf(
        MimoTtsModel(
            id = PRESET_MODEL_ID,
            name = "预置音色",
            description = "使用 MiMo 精品预置音色。",
        ),
        MimoTtsModel(
            id = VOICE_DESIGN_MODEL_ID,
            name = "VoiceDesign",
            description = "通过文本描述生成专属角色声音。",
        ),
    )

    val presetVoices: List<MimoTtsVoice> = listOf(
        MimoTtsVoice(DEFAULT_VOICE_ID, "MiMo-默认", null, null),
        MimoTtsVoice("冰糖", "冰糖", "中文", "女性"),
        MimoTtsVoice("茉莉", "茉莉", "中文", "女性"),
        MimoTtsVoice("苏打", "苏打", "中文", "男性"),
        MimoTtsVoice("白桦", "白桦", "中文", "男性"),
        MimoTtsVoice("Mia", "Mia", "英文", "女性"),
        MimoTtsVoice("Chloe", "Chloe", "英文", "女性"),
        MimoTtsVoice("Milo", "Milo", "英文", "男性"),
        MimoTtsVoice("Dean", "Dean", "英文", "男性"),
    )

    fun isVoiceDesignModel(model: String?): Boolean =
        model?.trim() == VOICE_DESIGN_MODEL_ID

    fun isPresetVoice(voice: String?): Boolean =
        presetVoices.any { it.id == voice?.trim() }
}
