package com.phrasevoice.data.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomTemplateRendererTest {
    private val renderer = CustomTemplateRenderer()

    @Test
    fun render_replacesKnownVariables() {
        val result = renderer.render(
            template = """{"text":"{{text}}","voice":"{{ voice }}","speed":"{{speed}}"}""",
            variables = mapOf(
                "text" to "hello",
                "voice" to "alloy",
                "speed" to "1.0",
            ),
        )

        assertEquals("""{"text":"hello","voice":"alloy","speed":"1.0"}""", result)
    }

    @Test
    fun render_missingVariableBecomesEmptyString() {
        val result = renderer.render("Authorization: Bearer {{apiKey}}", emptyMap())

        assertEquals("Authorization: Bearer ", result)
    }
}
