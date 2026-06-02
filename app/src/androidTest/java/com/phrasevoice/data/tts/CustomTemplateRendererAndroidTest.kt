package com.phrasevoice.data.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomTemplateRendererAndroidTest {
    @Test
    fun render_replacesVariablesOnAndroidRegexEngine() {
        val result = CustomTemplateRenderer().render(
            template = """{"text":"{{ text }}","voice":"{{voice}}"}""",
            variables = mapOf(
                "text" to "hello",
                "voice" to "alloy",
            ),
        )

        assertEquals("""{"text":"hello","voice":"alloy"}""", result)
    }
}
