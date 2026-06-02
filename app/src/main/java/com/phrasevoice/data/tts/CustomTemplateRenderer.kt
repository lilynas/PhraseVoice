package com.phrasevoice.data.tts

class CustomTemplateRenderer {
    fun render(template: String, variables: Map<String, String?>): String =
        VARIABLE_PATTERN.replace(template) { match ->
            val key = match.groupValues[1].trim()
            variables[key].orEmpty()
        }

    companion object {
        private val VARIABLE_PATTERN = Regex("\\{\\{\\s*([A-Za-z0-9_]+)\\s*\\}\\}")
    }
}
