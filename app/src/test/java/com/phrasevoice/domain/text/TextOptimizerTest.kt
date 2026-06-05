package com.phrasevoice.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextOptimizerTest {
    @Test
    fun cleanWhitespace_trimsLinesAndCollapsesBlankLines() {
        val input = "  你好   世界 \r\n\r\n\r\n  谢谢\t你  "

        val result = TextOptimizer.cleanWhitespace(input)

        assertEquals("你好 世界\n\n谢谢 你", result)
    }

    @Test
    fun addReadingBreaks_breaksAfterSentencePunctuation() {
        val input = "你好！很高兴见到你。请稍等"

        val result = TextOptimizer.addReadingBreaks(input)

        assertEquals("你好！\n很高兴见到你。\n请稍等", result)
    }

    @Test
    fun addMixedLanguageSpacing_addsReadableBoundaries() {
        val input = "欢迎使用PhraseVoice生成TTS音频"

        val result = TextOptimizer.addMixedLanguageSpacing(input)

        assertEquals("欢迎使用 PhraseVoice 生成 TTS 音频", result)
    }

    @Test
    fun addReadablePauseSpacing_addsShortPauseSpaces() {
        val input = "角色A：你好，欢迎使用PhraseVoice"

        val result = TextOptimizer.addReadablePauseSpacing(input)

        assertEquals("角色A： 你好， 欢迎使用PhraseVoice", result)
    }

    @Test
    fun addReadablePauseSpacing_preservesTimesAndUrls() {
        val input = "请在12:30打开https://example.com，开始试听"

        val result = TextOptimizer.addReadablePauseSpacing(input)

        assertEquals("请在12:30打开https://example.com， 开始试听", result)
    }

    @Test
    fun oneTapPolish_combinesSpacingAndBreaks() {
        val input = "角色A：你好，欢迎使用PhraseVoice！  现在开始生成音频。"

        val result = TextOptimizer.apply(input, TextOptimizationAction.OneTapPolish)

        assertEquals("角色 A： 你好， 欢迎使用 PhraseVoice！\n现在开始生成音频。", result)
    }
}
