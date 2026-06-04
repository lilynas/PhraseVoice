package com.phrasevoice.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

enum class AppLanguageMode(
    val value: String,
) {
    System("system"),
    Chinese("zh"),
    English("en");

    companion object {
        fun fromValue(value: String?): AppLanguageMode =
            entries.firstOrNull { it.value == value } ?: System
    }
}

enum class AppLanguage {
    Chinese,
    English;

    companion object {
        fun fromLocale(locale: Locale): AppLanguage =
            if (locale.language.equals("zh", ignoreCase = true)) Chinese else English
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.Chinese }

@Composable
fun resolveAppLanguage(languageMode: String): AppLanguage {
    val mode = AppLanguageMode.fromValue(languageMode)
    if (mode == AppLanguageMode.Chinese) return AppLanguage.Chinese
    if (mode == AppLanguageMode.English) return AppLanguage.English

    val configuration = LocalConfiguration.current
    val locale = configuration.locales.get(0) ?: Locale.getDefault()
    return AppLanguage.fromLocale(locale)
}

@Composable
fun t(zh: String, en: String): String =
    if (LocalAppLanguage.current == AppLanguage.English) en else zh

fun translate(language: AppLanguage, zh: String, en: String): String =
    if (language == AppLanguage.English) en else zh
