package com.phrasevoice.domain.tts

data class ReadingPreset(
    val id: String,
    val speed: Float,
    val pitch: Float,
    val volume: Float,
    val edgeStyleId: String? = null,
)

object ReadingPresets {
    val all: List<ReadingPreset> = listOf(
        ReadingPreset(
            id = NATURAL,
            speed = 1.0f,
            pitch = 1.0f,
            volume = 1.0f,
            edgeStyleId = "Friendly",
        ),
        ReadingPreset(
            id = GENTLE,
            speed = 0.88f,
            pitch = 0.95f,
            volume = 0.9f,
            edgeStyleId = "Warm",
        ),
        ReadingPreset(
            id = NOTICE,
            speed = 0.96f,
            pitch = 1.0f,
            volume = 1.0f,
            edgeStyleId = "Positive",
        ),
        ReadingPreset(
            id = SHORT_VIDEO,
            speed = 1.12f,
            pitch = 1.08f,
            volume = 1.0f,
            edgeStyleId = "Lively",
        ),
        ReadingPreset(
            id = ROLE_PLAY,
            speed = 0.94f,
            pitch = 1.02f,
            volume = 1.0f,
            edgeStyleId = "Lively",
        ),
        ReadingPreset(
            id = ENGLISH_PRACTICE,
            speed = 0.82f,
            pitch = 1.0f,
            volume = 0.95f,
            edgeStyleId = "Friendly",
        ),
    )

    const val NATURAL = "natural"
    const val GENTLE = "gentle"
    const val NOTICE = "notice"
    const val SHORT_VIDEO = "short_video"
    const val ROLE_PLAY = "role_play"
    const val ENGLISH_PRACTICE = "english_practice"
}
