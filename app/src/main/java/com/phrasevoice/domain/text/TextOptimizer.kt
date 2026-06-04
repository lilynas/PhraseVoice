package com.phrasevoice.domain.text

object TextOptimizer {
    private val horizontalWhitespace = Regex("[\\t\\x0B\\f\\r ]+")
    private val repeatedBlankLines = Regex("\\n{3,}")
    private val sentenceBreak = Regex("([。！？!?；;])\\s*([^\\n])")
    private val cjkBeforeAscii = Regex("([\\p{IsHan}])([A-Za-z0-9])")
    private val asciiBeforeCjk = Regex("([A-Za-z0-9])([\\p{IsHan}])")

    fun apply(text: String, action: TextOptimizationAction): String =
        when (action) {
            TextOptimizationAction.CleanWhitespace -> cleanWhitespace(text)
            TextOptimizationAction.AddReadingBreaks -> addReadingBreaks(text)
            TextOptimizationAction.MixedLanguageSpacing -> addMixedLanguageSpacing(text)
            TextOptimizationAction.OneTapPolish -> polish(text)
        }

    fun cleanWhitespace(text: String): String =
        text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lineSequence()
            .map { line -> horizontalWhitespace.replace(line, " ").trim() }
            .joinToString("\n")
            .let { repeatedBlankLines.replace(it, "\n\n") }
            .trim()

    fun addReadingBreaks(text: String): String {
        val cleaned = cleanWhitespace(text)
        return sentenceBreak.replace(cleaned) { match ->
            "${match.groupValues[1]}\n${match.groupValues[2]}"
        }
    }

    fun addMixedLanguageSpacing(text: String): String =
        cleanWhitespace(text)
            .let { cjkBeforeAscii.replace(it, "$1 $2") }
            .let { asciiBeforeCjk.replace(it, "$1 $2") }

    fun polish(text: String): String =
        addReadingBreaks(addMixedLanguageSpacing(text))
}
