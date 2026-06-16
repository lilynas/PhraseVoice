package com.phrasevoice.ui.i18n

import androidx.compose.runtime.Composable
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.model.PhraseGroup
import com.phrasevoice.data.repository.ProviderHealthStatus
import com.phrasevoice.data.tts.EdgeForwarderStyle

private val importedPhrasesRegex =
    Regex("^已导入 (\\d+) 条常用语(，新增 (\\d+) 个分组)?(，跳过 (\\d+) 条重复/空内容)?。$")
private val exportedPhrasesRegex = Regex("^已导出 (\\d+) 条常用语。$")

@Composable
fun localizedPhraseTitle(phrase: Phrase): String =
    when (phrase.id) {
        "seed_hello" -> if (phrase.title == "问候") t(phrase.title, "Greeting") else phrase.title
        "seed_thanks" -> if (phrase.title == "感谢") t(phrase.title, "Thanks") else phrase.title
        "seed_later" -> if (phrase.title == "稍后回复") t(phrase.title, "Reply Later") else phrase.title
        else -> phrase.title
    }

@Composable
fun localizedPhraseGroupName(group: PhraseGroup): String =
    when (group.id) {
        "default" -> if (group.name == "常用") t(group.name, "Common") else group.name
        else -> group.name
    }

@Composable
fun localizedEdgeStyleName(style: EdgeForwarderStyle): String =
    when (style.id) {
        "" -> t(style.name, "Default")
        "Friendly" -> t(style.name, "Friendly")
        "Positive" -> t(style.name, "Positive")
        "Warm" -> t(style.name, "Warm")
        "Lively" -> t(style.name, "Lively")
        "Passion" -> t(style.name, "Passionate")
        "Sunshine" -> t(style.name, "Sunny")
        "Humorious" -> t(style.name, "Humorous")
        else -> style.name
    }

@Composable
fun localizedHomeErrorMessage(message: String): String {
    val disabledSuffix = " 尚未启用，请先在 Provider 页面保存配置。"
    if (message.endsWith(disabledSuffix)) {
        val providerName = message.removeSuffix(disabledSuffix)
        return t(message, "$providerName is not enabled. Save its configuration on the Provider page first.")
    }
    val notConfiguredSuffix = " 未配置，请先在 Provider 页面启用并保存。"
    if (message.endsWith(notConfiguredSuffix)) {
        val providerName = message.removeSuffix(notConfiguredSuffix)
        return t(message, "$providerName is not configured. Enable and save it on the Provider page first.")
    }
    val missingApiKeySuffix = " 缺少 API Key，请先在 Provider 页面保存 API Key。"
    if (message.endsWith(missingApiKeySuffix)) {
        val providerName = message.removeSuffix(missingApiKeySuffix)
        return t(message, "$providerName is missing an API key. Save the API key on the Provider page first.")
    }
    val missingBaseUrlSuffix = " 缺少 Base URL，请先在 Provider 页面填写服务地址。"
    if (message.endsWith(missingBaseUrlSuffix)) {
        val providerName = message.removeSuffix(missingBaseUrlSuffix)
        return t(message, "$providerName is missing a Base URL. Enter the service URL on the Provider page first.")
    }
    val missingOfflineModelSuffix = " 缺少可用离线语音包，请先在设置中导入模型。"
    if (message.endsWith(missingOfflineModelSuffix)) {
        val providerName = message.removeSuffix(missingOfflineModelSuffix)
        return t(message, "$providerName needs an offline model. Import one in Settings first.")
    }
    return localizedRuntimeMessage(message)
}

@Composable
fun localizedProviderHealthLabel(status: ProviderHealthStatus): String =
    when (status) {
        ProviderHealthStatus.Ready -> t("可用", "Ready")
        ProviderHealthStatus.Disabled -> t("未配置", "Not configured")
        ProviderHealthStatus.MissingApiKey -> t("缺少 API Key", "Missing API key")
        ProviderHealthStatus.MissingBaseUrl -> t("缺少 Base URL", "Missing Base URL")
        ProviderHealthStatus.MissingOfflineModel -> t("缺少离线语音包", "Missing offline model")
        ProviderHealthStatus.SystemUnavailable -> t("系统 TTS 不可用", "System TTS unavailable")
    }

@Composable
fun localizedProviderHealthDescription(
    status: ProviderHealthStatus,
    providerName: String,
    androidTtsMessage: String? = null,
): String =
    when (status) {
        ProviderHealthStatus.Ready -> t(
            "$providerName 已准备好，可以保存并试听。",
            "$providerName is ready. Save and test when you want to confirm the voice.",
        )
        ProviderHealthStatus.Disabled -> t(
            "打开开关并保存后，$providerName 才会出现在工作台里。",
            "Turn it on and save before using $providerName in Studio.",
        )
        ProviderHealthStatus.MissingApiKey -> t(
            "请填写并保存 API Key，然后再试听 $providerName。",
            "Enter and save the API key before testing $providerName.",
        )
        ProviderHealthStatus.MissingBaseUrl -> t(
            "请填写服务地址，然后再试听 $providerName。",
            "Enter the service URL before testing $providerName.",
        )
        ProviderHealthStatus.MissingOfflineModel -> t(
            "请先在设置的离线语音包管理中导入可用模型，然后再使用 $providerName。",
            "Import an available model in Offline Voice settings before using $providerName.",
        )
        ProviderHealthStatus.SystemUnavailable -> if (!androidTtsMessage.isNullOrBlank()) {
            localizedRuntimeMessage(androidTtsMessage)
        } else {
            t(
                "请在系统设置中启用语音引擎，或切换到云端 Provider。",
                "Enable a speech engine in system settings, or switch to a cloud Provider.",
            )
        }
    }

@Composable
fun localizedProviderStatusMessage(message: String): String {
    val appliedPrefix = "已应用 "
    val appliedSuffix = " 模板"
    if (message.startsWith(appliedPrefix) && message.endsWith(appliedSuffix)) {
        val presetName = message.removePrefix(appliedPrefix).removeSuffix(appliedSuffix)
        return t(message, "Applied $presetName template")
    }

    if (message.startsWith("优化失败：")) {
        val detail = message.removePrefix("优化失败：")
        return t(message, "Optimization failed: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("试听失败：")) {
        val detail = message.removePrefix("试听失败：")
        return t(message, "Test failed: ${localizedRuntimeMessage(detail)}")
    }

    return when (message) {
        "请先填写音色描述" -> t(message, "Enter a voice description first.")
        "角色声音已暂存，请保存配置" -> t(message, "Character voice staged. Save the configuration to apply it.")
        "至少保留一个角色声音" -> t(message, "Keep at least one character voice.")
        "角色声音已删除，请保存配置" -> t(message, "Character voice deleted. Save the configuration to apply it.")
        "请先切换到 MiMo VoiceDesign 模式" -> t(message, "Switch to MiMo VoiceDesign mode first.")
        "正在优化音色描述" -> t(message, "Optimizing voice description...")
        "已优化音色描述，请保存配置" -> t(message, "Voice description optimized. Save the configuration to apply it.")
        "已保存" -> t(message, "Saved.")
        "当前 Provider 不需要云端试听" -> t(message, "The current Provider does not need a cloud test.")
        "正在保存并试听" -> t(message, "Saving and testing...")
        "试听已播放" -> t(message, "Test audio played.")
        else -> localizedRuntimeMessage(message)
    }
}

@Composable
fun localizedSettingsStatusMessage(message: String): String {
    if (message == "没有可清理的音频缓存") {
        return t(message, "No audio cache to clear.")
    }

    val clearedPrefix = "已清理 "
    val clearedSuffix = " 个音频文件"
    if (message.startsWith(clearedPrefix) && message.endsWith(clearedSuffix)) {
        val count = message.removePrefix(clearedPrefix).removeSuffix(clearedSuffix)
        return t(message, "Cleared $count audio files.")
    }

    if (message.startsWith("导入失败：")) {
        val detail = message.removePrefix("导入失败：")
        return t(message, "Import failed: ${localizedRuntimeMessage(detail)}")
    }

    return when (message) {
        "已导入离线语音模型，可在工作台选择 Offline sherpa-onnx。" ->
            t(message, "Offline voice model imported. Select Offline sherpa-onnx in Studio.")
        "模型包为空，已标记为损坏。" -> t(message, "The model package is empty and was marked corrupt.")
        "未找到可用 sherpa-onnx TTS 模型结构，已保留记录。" ->
            t(message, "No usable sherpa-onnx TTS model layout was found. The record was kept.")
        "已删除离线语音模型。" -> t(message, "Offline voice model deleted.")
        else -> localizedRuntimeMessage(message)
    }
}

@Composable
fun localizedPhraseLibraryStatusMessage(message: String): String {
    importedPhrasesRegex.matchEntire(message)?.let { match ->
        val phraseCount = match.groupValues[1]
        val groupCount = match.groupValues[3].takeIf { it.isNotBlank() }
        val skippedCount = match.groupValues[5].takeIf { it.isNotBlank() }
        val extras = buildList {
            if (groupCount != null) add("$groupCount groups added")
            if (skippedCount != null) add("$skippedCount duplicate or empty items skipped")
        }
        val suffix = extras.takeIf { it.isNotEmpty() }?.joinToString(prefix = ", ") ?: ""
        return t(message, "Imported $phraseCount phrases$suffix.")
    }

    exportedPhrasesRegex.matchEntire(message)?.let { match ->
        val phraseCount = match.groupValues[1]
        return t(message, "Exported $phraseCount phrases.")
    }

    if (message.startsWith("导入失败：")) {
        val detail = message.removePrefix("导入失败：")
        return t(message, "Import failed: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("Import failed: ")) {
        val detail = message.removePrefix("Import failed: ")
        return "Import failed: ${localizedRuntimeMessage(detail)}"
    }

    if (message.startsWith("导出失败：")) {
        val detail = message.removePrefix("导出失败：")
        return t(message, "Export failed: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("Export failed: ")) {
        val detail = message.removePrefix("Export failed: ")
        return "Export failed: ${localizedRuntimeMessage(detail)}"
    }

    return when (message) {
        "没有新的常用语可导入，已跳过重复/空内容。" -> {
            t(message, "No new phrases to import. Duplicate or empty items were skipped.")
        }
        "导入文件里没有常用语。" -> t(message, "No phrases were found in the import file.")
        "文件格式不正确" -> t(message, "Invalid file format")
        else -> localizedRuntimeMessage(message)
    }
}

@Composable
fun localizedRuntimeMessage(message: String): String {
    if (message.startsWith("网络请求失败：")) {
        val detail = message.removePrefix("网络请求失败：")
        return t(message, "Network request failed: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("解析音频响应失败：")) {
        val detail = message.removePrefix("解析音频响应失败：")
        return t(message, "Failed to parse audio response: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("下载音频失败：")) {
        val detail = message.removePrefix("下载音频失败：")
        return t(message, "Failed to download audio: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("解析 Gemini 音频响应失败：")) {
        val detail = message.removePrefix("解析 Gemini 音频响应失败：")
        return t(message, "Failed to parse Gemini audio response: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("解析 MiMo 音频响应失败：")) {
        val detail = message.removePrefix("解析 MiMo 音频响应失败：")
        return t(message, "Failed to parse MiMo audio response: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("解析 MiMo 流式音频响应失败：")) {
        val detail = message.removePrefix("解析 MiMo 流式音频响应失败：")
        return t(message, "Failed to parse MiMo streaming audio response: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("服务端暂时不可用：HTTP ")) {
        val code = message.removePrefix("服务端暂时不可用：HTTP ").removeSuffix("。")
        return t(message, "Server is temporarily unavailable: HTTP $code.")
    }

    if (message.startsWith("请求失败：HTTP ")) {
        val code = message.removePrefix("请求失败：HTTP ").removeSuffix("。")
        return t(message, "Request failed: HTTP $code.")
    }

    if (message.startsWith("离线语音合成失败：")) {
        val detail = message.removePrefix("离线语音合成失败：")
        return t(message, "Offline voice synthesis failed: ${localizedRuntimeMessage(detail)}")
    }

    if (message.startsWith("JSON 中没有找到字段：")) {
        val fieldPath = message.removePrefix("JSON 中没有找到字段：")
        return t(message, "JSON field not found: $fieldPath")
    }

    if (message.startsWith("Android 系统 TTS 初始化失败/超时。")) {
        return t(
            message,
            "Android System TTS initialization failed or timed out. Check the default speech engine in system settings, or switch to OpenAI TTS / Custom TTS API.",
        )
    }

    return when (message) {
        "请输入要朗读的文字。" -> t(message, "Enter text to read.")
        "请输入要保存为音频的文字。" -> t(message, "Enter text to save as audio.")
        "Android 系统 TTS 暂不可用。" -> t(message, "Android System TTS is currently unavailable.")
        "请先在设置中导入可用的离线语音包。" -> t(message, "Import an available offline voice model in Settings first.")
        "离线语音包不可用，请重新导入模型包。" -> t(message, "The offline voice model is unavailable. Re-import the model package.")
        "离线语音没有生成音频。" -> t(message, "The offline voice model did not generate audio.")
        "云端失败，已切换到离线语音包。" -> t(message, "Cloud voice failed. Switched to the offline voice model.")
        "该 Provider 暂未接入。" -> t(message, "This Provider is not supported yet.")
        "请先在 Provider 页面启用并保存该 Provider。" -> t(message, "Enable and save this Provider on the Provider page first.")
        "请先在 Provider 页面保存 API Key。" -> t(message, "Save the API key on the Provider page first.")
        "请先配置 OpenAI Base URL。" -> t(message, "Configure the OpenAI Base URL first.")
        "请先配置 Edge TTS Forwarder Base URL。" -> t(message, "Configure the Edge TTS Forwarder Base URL first.")
        "Edge TTS Forwarder Base URL 无效。" -> t(message, "The Edge TTS Forwarder Base URL is invalid.")
        "请先在 Provider 页面保存 Gemini API Key。" -> t(message, "Save the Gemini API key on the Provider page first.")
        "请先配置 Gemini Base URL。" -> t(message, "Configure the Gemini Base URL first.")
        "Gemini Base URL 无效。" -> t(message, "The Gemini Base URL is invalid.")
        "请先填写或保存 MiMo API Key。" -> t(message, "Enter or save the MiMo API key first.")
        "请先在 Provider 页面保存 MiMo API Key。" -> t(message, "Save the MiMo API key on the Provider page first.")
        "请先配置 MiMo Base URL。" -> t(message, "Configure the MiMo Base URL first.")
        "MiMo Base URL 无效。" -> t(message, "The MiMo Base URL is invalid.")
        "请先填写 MiMo VoiceDesign 音色描述。" -> t(message, "Enter a MiMo VoiceDesign voice description first.")
        "请先配置 Custom TTS API Base URL。" -> t(message, "Configure the Custom TTS API Base URL first.")
        "服务没有返回音频内容。" -> t(message, "The service did not return audio content.")
        "服务没有返回 JSON。" -> t(message, "The service did not return JSON.")
        "Gemini 没有返回 JSON。" -> t(message, "Gemini did not return JSON.")
        "MiMo 没有返回 JSON。" -> t(message, "MiMo did not return JSON.")
        "MiMo 没有返回流式响应。" -> t(message, "MiMo did not return a streaming response.")
        "认证失败：请检查 API Key 或 Token。" -> t(message, "Authentication failed. Check the API key or token.")
        "账户余额或配额不足。" -> t(message, "Account balance or quota is insufficient.")
        "接口地址不存在，请检查 Base URL。" -> t(message, "Endpoint not found. Check the Base URL.")
        "请求过于频繁或额度耗尽。" -> t(message, "Too many requests or quota exhausted.")
        "无法打开文件" -> t(message, "Unable to open file")
        "无法写入文件" -> t(message, "Unable to write file")
        "无法读取文件" -> t(message, "Unable to read file")
        "无法生成文件" -> t(message, "Unable to create file")
        "未知错误" -> t(message, "Unknown error")
        else -> message
    }
}
