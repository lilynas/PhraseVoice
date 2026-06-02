package com.phrasevoice.ui.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.CustomHttpResponseType
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.tts.EdgeForwarderCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    state: ProviderSettingsUiState,
    onProviderSelected: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onVoiceChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onResponseTypeChange: (CustomHttpResponseType) -> Unit,
    onResponseFieldChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Provider",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            state.savedMessage?.let {
                AssistChip(onClick = {}, label = { Text(it) })
            }
        }

        state.configs.forEach { config ->
            ProviderSummaryCard(
                config = config,
                selected = config.providerId == state.selectedProviderId,
                onClick = { onProviderSelected(config.providerId) },
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row {
                    Text(
                        text = providerLabel(state.selectedProviderId),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.enabledDraft,
                        onCheckedChange = onEnabledChange,
                        enabled = state.selectedProviderId != ProviderConfigRepository.ANDROID_SYSTEM &&
                            state.selectedProviderId != ProviderConfigRepository.GEMINI &&
                            state.selectedProviderId != ProviderConfigRepository.MIMO,
                    )
                }

                when (state.selectedProviderId) {
                    ProviderConfigRepository.ANDROID_SYSTEM -> {
                        Text("系统 TTS 使用手机已安装的语音服务，无需 API Key。")
                    }

                    ProviderConfigRepository.GEMINI -> {
                        Text("Gemini TTS 会在下一步接入。当前可先用 Custom HTTP 配置兼容服务。")
                    }

                    ProviderConfigRepository.MIMO -> {
                        Text("MiMo TTS 已加入计划。后续会接入预置音色和 VoiceDesign 角色声音：用文本描述生成专属角色音色，试听后保存为本地角色声音预设。")
                    }

                    else -> {
                        OutlinedTextField(
                            value = state.apiKeyDraft,
                            onValueChange = onApiKeyChange,
                            label = {
                                Text(
                                    when {
                                        state.isEdgeForwarder && state.hasSavedApiKey -> "Token（已保存，留空不改）"
                                        state.isEdgeForwarder -> "Token（可选）"
                                        state.hasSavedApiKey -> "API Key（已保存，留空不改）"
                                        else -> "API Key"
                                    },
                                )
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (state.isEdgeForwarder) {
                            Text("使用 ms-ra-forwarder 的 /api/text-to-speech 接口。Base URL 可填站点根地址或完整接口地址；如果实例启用了 TOKEN，这里填 Token。")
                        }

                        OutlinedTextField(
                            value = state.baseUrlDraft,
                            onValueChange = onBaseUrlChange,
                            label = { Text(if (state.isEdgeForwarder) "Forwarder URL" else "Base URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (!state.isEdgeForwarder) {
                            OutlinedTextField(
                                value = state.modelDraft,
                                onValueChange = onModelChange,
                                label = { Text("模型") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (state.isEdgeForwarder) {
                            EdgeForwarderVoiceDropdown(
                                voiceDraft = state.voiceDraft,
                                onVoiceChange = onVoiceChange,
                            )
                        } else {
                            OutlinedTextField(
                                value = state.voiceDraft,
                                onValueChange = onVoiceChange,
                                label = { Text("默认 Voice") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (state.isCustomHttp) {
                            CustomHttpFields(
                                state = state,
                                onMethodChange = onMethodChange,
                                onHeadersChange = onHeadersChange,
                                onBodyChange = onBodyChange,
                                onResponseTypeChange = onResponseTypeChange,
                                onResponseFieldChange = onResponseFieldChange,
                            )
                        }

                        Button(
                            onClick = onSave,
                            enabled = !state.isTesting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null)
                            Text("保存配置")
                        }
                        OutlinedButton(
                            onClick = onTestVoice,
                            enabled = !state.isTesting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Text(if (state.isTesting) "试听中" else "保存并试听")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderSummaryCard(
    config: ProviderConfig,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProviderIcon(config.providerId)
                    Text(providerLabel(config.providerId), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = when {
                        config.providerId == ProviderConfigRepository.GEMINI -> "后续接入"
                        config.providerId == ProviderConfigRepository.MIMO -> "角色声音计划"
                        config.enabled -> "已启用"
                        else -> "未启用"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProviderIcon(providerId: String) {
    val icon = when (providerId) {
        ProviderConfigRepository.ANDROID_SYSTEM -> Icons.Outlined.PhoneAndroid
        ProviderConfigRepository.EDGE_TTS_FORWARDER -> Icons.Outlined.Cloud
        ProviderConfigRepository.CUSTOM_HTTP -> Icons.Outlined.Tune
        else -> Icons.Outlined.Cloud
    }
    Icon(icon, contentDescription = null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EdgeForwarderVoiceDropdown(
    voiceDraft: String,
    onVoiceChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val knownVoices = EdgeForwarderCatalog.voices
    val selectedName = knownVoices
        .firstOrNull { it.id == voiceDraft }
        ?.let { "${it.name} ${it.locale}" }
        ?: voiceDraft

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text("默认发音人") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            knownVoices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(voice.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(voice.locale, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        expanded = false
                        onVoiceChange(voice.id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomHttpFields(
    state: ProviderSettingsUiState,
    onMethodChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onResponseTypeChange: (CustomHttpResponseType) -> Unit,
    onResponseFieldChange: (String) -> Unit,
) {
    var methodExpanded by remember { mutableStateOf(false) }
    var responseExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = methodExpanded,
        onExpandedChange = { methodExpanded = !methodExpanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = state.methodDraft,
            onValueChange = {},
            label = { Text("HTTP Method") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
        )
        ExposedDropdownMenu(
            expanded = methodExpanded,
            onDismissRequest = { methodExpanded = false },
        ) {
            listOf("POST", "GET", "PUT").forEach { method ->
                DropdownMenuItem(
                    text = { Text(method) },
                    onClick = {
                        methodExpanded = false
                        onMethodChange(method)
                    },
                )
            }
        }
    }

    OutlinedTextField(
        value = state.headersDraft,
        onValueChange = onHeadersChange,
        label = { Text("Headers 模板") },
        minLines = 3,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp),
    )
    OutlinedTextField(
        value = state.bodyDraft,
        onValueChange = onBodyChange,
        label = { Text("JSON Body 模板") },
        minLines = 5,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 144.dp),
    )

    ExposedDropdownMenuBox(
        expanded = responseExpanded,
        onExpandedChange = { responseExpanded = !responseExpanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = responseTypeLabel(state.responseTypeDraft),
            onValueChange = {},
            label = { Text("Response 类型") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = responseExpanded) },
        )
        ExposedDropdownMenu(
            expanded = responseExpanded,
            onDismissRequest = { responseExpanded = false },
        ) {
            CustomHttpResponseType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(responseTypeLabel(type)) },
                    onClick = {
                        responseExpanded = false
                        onResponseTypeChange(type)
                    },
                )
            }
        }
    }

    if (state.responseTypeDraft != CustomHttpResponseType.RAW_AUDIO) {
        OutlinedTextField(
            value = state.responseFieldDraft,
            onValueChange = onResponseFieldChange,
            label = { Text("JSON 字段路径") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun providerLabel(providerId: String): String =
    when (providerId) {
        ProviderConfigRepository.ANDROID_SYSTEM -> "Android System TTS"
        ProviderConfigRepository.OPENAI -> "OpenAI-compatible TTS"
        ProviderConfigRepository.EDGE_TTS_FORWARDER -> "Edge TTS Forwarder"
        ProviderConfigRepository.GEMINI -> "Gemini TTS"
        ProviderConfigRepository.MIMO -> "MiMo TTS"
        ProviderConfigRepository.CUSTOM_HTTP -> "Custom HTTP TTS"
        else -> providerId
    }

private fun responseTypeLabel(type: CustomHttpResponseType): String =
    when (type) {
        CustomHttpResponseType.RAW_AUDIO -> "Raw audio"
        CustomHttpResponseType.JSON_BASE64_FIELD -> "JSON base64 field"
        CustomHttpResponseType.JSON_URL_FIELD -> "JSON URL field"
    }
