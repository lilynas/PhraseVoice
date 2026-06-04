package com.phrasevoice.ui.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.CustomHttpResponseType
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.tts.EdgeForwarderCatalog
import com.phrasevoice.data.tts.GeminiTtsCatalog
import com.phrasevoice.data.tts.MimoTtsCatalog
import com.phrasevoice.ui.i18n.localizedProviderStatusMessage
import com.phrasevoice.ui.i18n.t

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
    onApplyTemplate: (String) -> Unit,
    onMimoOptimizeTextPreviewChange: (Boolean) -> Unit,
    onMimoPromptOptimizerModelChange: (String) -> Unit,
    onMimoUseStreamingChange: (Boolean) -> Unit,
    onMimoVoiceDesignPresetSelected: (String) -> Unit,
    onMimoVoiceDesignPresetNameChange: (String) -> Unit,
    onAddMimoVoiceDesignPreset: () -> Unit,
    onSaveMimoVoiceDesignPreset: () -> Unit,
    onDeleteMimoVoiceDesignPreset: () -> Unit,
    onOptimizeMimoVoiceDesign: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Provider",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        ProviderSelectorDropdown(
            state = state,
            onProviderSelected = onProviderSelected,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = providerLabel(state.selectedProviderId),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.enabledDraft,
                        onCheckedChange = onEnabledChange,
                        enabled = state.selectedProviderId != ProviderConfigRepository.ANDROID_SYSTEM,
                    )
                }

                when (state.selectedProviderId) {
                    ProviderConfigRepository.ANDROID_SYSTEM -> {
                        Text(t("系统 TTS 使用手机已安装的语音服务，无需 API Key。", "System TTS uses voice services installed on this device. No API key is required."))
                    }

                    else -> {
                        StyledOutlinedTextField(
                            value = state.apiKeyDraft,
                            onValueChange = onApiKeyChange,
                            label = {
                                Text(
                                    when {
                                        state.isEdgeForwarder && state.hasSavedApiKey -> t("Token（已保存，留空不改）", "Token (saved, leave blank to keep)")
                                        state.isEdgeForwarder -> t("Token（可选）", "Token (optional)")
                                        state.hasSavedApiKey -> t("API Key（已保存，留空不改）", "API Key (saved, leave blank to keep)")
                                        else -> "API Key"
                                    },
                                )
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (state.isEdgeForwarder) {
                            Text(t("使用 ms-ra-forwarder 的 /api/text-to-speech 接口。Base URL 可填站点根地址或完整接口地址；如果实例启用了 TOKEN，这里填 Token。", "Uses the ms-ra-forwarder /api/text-to-speech endpoint. Base URL can be the site root or full endpoint. If TOKEN is enabled, enter it here."))
                        }

                        if (state.isGemini) {
                            Text(t("使用 Gemini generateContent TTS 接口。Gemini 返回 PCM 音频，App 会自动封装为 WAV。", "Uses the Gemini generateContent TTS API. PCM audio is automatically wrapped as WAV."))
                        }

                        if (state.isMimo) {
                            Text(t("使用 MiMo V2.5 TTS。预置音色可直接选择；VoiceDesign 模式会用文本描述生成专属角色声音。", "Uses MiMo V2.5 TTS. Preset voices are selectable; VoiceDesign generates a character voice from your description."))
                        }

                        StyledOutlinedTextField(
                            value = state.baseUrlDraft,
                            onValueChange = onBaseUrlChange,
                            label = {
                                Text(
                                    when {
                                        state.isEdgeForwarder -> "Forwarder URL"
                                        state.isGemini -> "Gemini Base URL"
                                        state.isMimo -> "MiMo Base URL"
                                        else -> "Base URL"
                                    },
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (state.isMimo) {
                            MimoModelDropdown(
                                modelDraft = state.modelDraft,
                                onModelChange = onModelChange,
                            )
                            MimoStreamingFields(
                                state = state,
                                onMimoUseStreamingChange = onMimoUseStreamingChange,
                            )
                        } else if (!state.isEdgeForwarder) {
                            val modelOptions = when (state.selectedProviderId) {
                                ProviderConfigRepository.OPENAI -> listOf("tts-1", "tts-1-hd")
                                ProviderConfigRepository.GEMINI -> listOf("gemini-1.5-flash", "gemini-1.5-pro")
                                ProviderConfigRepository.CUSTOM_HTTP -> listOf("tts-1", "tts-1-hd", "gpt-4o")
                                else -> emptyList()
                            }
                            GenericCombobox(
                                label = t("模型", "Model"),
                                value = state.modelDraft,
                                options = modelOptions,
                                onValueChange = onModelChange,
                                readOnly = false
                            )
                        }

                        if (state.isEdgeForwarder) {
                            EdgeForwarderVoiceDropdown(
                                voiceDraft = state.voiceDraft,
                                onVoiceChange = onVoiceChange,
                            )
                        } else if (state.isGemini) {
                            GeminiVoiceDropdown(
                                voiceDraft = state.voiceDraft,
                                onVoiceChange = onVoiceChange,
                            )
                        } else if (state.isMimoVoiceDesign) {
                            MimoVoiceDesignFields(
                                state = state,
                                onVoiceChange = onVoiceChange,
                                onMimoOptimizeTextPreviewChange = onMimoOptimizeTextPreviewChange,
                                onMimoPromptOptimizerModelChange = onMimoPromptOptimizerModelChange,
                                onMimoVoiceDesignPresetSelected = onMimoVoiceDesignPresetSelected,
                                onMimoVoiceDesignPresetNameChange = onMimoVoiceDesignPresetNameChange,
                                onAddMimoVoiceDesignPreset = onAddMimoVoiceDesignPreset,
                                onSaveMimoVoiceDesignPreset = onSaveMimoVoiceDesignPreset,
                                onDeleteMimoVoiceDesignPreset = onDeleteMimoVoiceDesignPreset,
                                onOptimizeMimoVoiceDesign = onOptimizeMimoVoiceDesign,
                            )
                        } else if (state.isMimo) {
                            MimoVoiceDropdown(
                                voiceDraft = state.voiceDraft,
                                onVoiceChange = onVoiceChange,
                            )
                        } else {
                            val voiceOptions = when (state.selectedProviderId) {
                                ProviderConfigRepository.OPENAI -> listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer")
                                ProviderConfigRepository.CUSTOM_HTTP -> listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer")
                                else -> emptyList()
                            }
                            GenericCombobox(
                                label = t("默认 Voice", "Default Voice"),
                                value = state.voiceDraft,
                                options = voiceOptions,
                                onValueChange = onVoiceChange,
                                readOnly = false
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
                                onApplyTemplate = onApplyTemplate,
                            )
                        }

                        state.savedMessage?.let { msg ->
                            val localizedMessage = localizedProviderStatusMessage(msg)
                            val isError = msg.contains("失败") || msg.contains("错误") ||
                                localizedMessage.contains("failed", ignoreCase = true) ||
                                localizedMessage.contains("error", ignoreCase = true)
                            val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            Text(
                                text = localizedMessage,
                                color = color,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = onSave,
                            enabled = !state.isTesting && !state.isOptimizingVoiceDesign,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null)
                            Text(t("保存配置", "Save Config"))
                        }
                        OutlinedButton(
                            onClick = onTestVoice,
                            enabled = !state.isTesting && !state.isOptimizingVoiceDesign,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Text(if (state.isTesting) t("试听中", "Testing") else t("保存并试听", "Save & Test"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MimoStreamingFields(
    state: ProviderSettingsUiState,
    onMimoUseStreamingChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(t("MiMo 流式合成", "MiMo Streaming Synthesis"))
            Text(
                t("使用 pcm16 流式响应，完成后自动封装为 WAV。", "Uses pcm16 streaming responses and wraps the result as WAV."),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = state.mimoUseStreamingDraft,
            onCheckedChange = onMimoUseStreamingChange,
            enabled = !state.isTesting && !state.isOptimizingVoiceDesign,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MimoVoiceDesignPresetDropdown(
    state: ProviderSettingsUiState,
    onMimoVoiceDesignPresetSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = state.mimoVoiceDesignPresetsDraft
        .firstOrNull { it.id == state.mimoSelectedVoiceDesignPresetIdDraft }
        ?.name
        ?: t("默认角色", "Default Character")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        StyledOutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text(t("角色声音", "Character Voice")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.mimoVoiceDesignPresetsDraft.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(preset.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                preset.description,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onMimoVoiceDesignPresetSelected(preset.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun MimoVoiceDesignFields(
    state: ProviderSettingsUiState,
    onVoiceChange: (String) -> Unit,
    onMimoOptimizeTextPreviewChange: (Boolean) -> Unit,
    onMimoPromptOptimizerModelChange: (String) -> Unit,
    onMimoVoiceDesignPresetSelected: (String) -> Unit,
    onMimoVoiceDesignPresetNameChange: (String) -> Unit,
    onAddMimoVoiceDesignPreset: () -> Unit,
    onSaveMimoVoiceDesignPreset: () -> Unit,
    onDeleteMimoVoiceDesignPreset: () -> Unit,
    onOptimizeMimoVoiceDesign: () -> Unit,
) {
    MimoVoiceDesignPresetDropdown(
        state = state,
        onMimoVoiceDesignPresetSelected = onMimoVoiceDesignPresetSelected,
    )
    StyledOutlinedTextField(
        value = state.mimoVoiceDesignPresetNameDraft,
        onValueChange = onMimoVoiceDesignPresetNameChange,
        label = { Text(t("角色名称", "Character Name")) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    StyledOutlinedTextField(
        value = state.voiceDraft,
        onValueChange = onVoiceChange,
        label = { Text(t("VoiceDesign 音色描述", "VoiceDesign Description")) },
        minLines = 4,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 136.dp),
    )
    StyledOutlinedTextField(
        value = state.mimoPromptOptimizerModelDraft,
        onValueChange = onMimoPromptOptimizerModelChange,
        label = { Text(t("描述优化模型", "Prompt Optimizer Model")) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = onOptimizeMimoVoiceDesign,
        enabled = !state.isTesting &&
            !state.isOptimizingVoiceDesign &&
            state.voiceDraft.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Tune, contentDescription = null)
        Text(if (state.isOptimizingVoiceDesign) t("优化中", "Optimizing") else t("优化音色描述", "Optimize Voice Description"))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onAddMimoVoiceDesignPreset,
            enabled = !state.isTesting && !state.isOptimizingVoiceDesign,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text(t("新增角色", "New Character"))
        }
        OutlinedButton(
            onClick = onSaveMimoVoiceDesignPreset,
            enabled = !state.isTesting && !state.isOptimizingVoiceDesign && state.voiceDraft.isNotBlank(),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Outlined.Save, contentDescription = null)
            Text(t("暂存角色", "Save Character"))
        }
    }
    OutlinedButton(
        onClick = onDeleteMimoVoiceDesignPreset,
        enabled = !state.isTesting &&
            !state.isOptimizingVoiceDesign &&
            state.mimoVoiceDesignPresetsDraft.size > 1,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Delete, contentDescription = null)
        Text(t("删除当前角色", "Delete Current Character"))
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(t("试听文本智能优化", "Smart Preview Text Optimization"))
            Text(
                t(
                    "开启后 MiMo 会在 VoiceDesign 试听/朗读时优化目标文本；音色描述仍以上方内容为准。",
                    "When enabled, MiMo optimizes the target text for VoiceDesign preview/reading; the voice description above remains unchanged.",
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = state.mimoOptimizeTextPreviewDraft,
            onCheckedChange = onMimoOptimizeTextPreviewChange,
            enabled = !state.isTesting && !state.isOptimizingVoiceDesign,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderSelectorDropdown(
    state: ProviderSettingsUiState,
    onProviderSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedConfig = state.configs.firstOrNull { it.providerId == state.selectedProviderId }
    val selectedLabel = selectedConfig?.let { providerLabel(it.providerId) }.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedLabel,
            onValueChange = {},
            label = { Text(t("选择 Provider 声音引擎", "Select Provider Voice Engine")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            leadingIcon = {
                selectedConfig?.let { ProviderIcon(it.providerId) }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            state.configs.forEach { config ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProviderIcon(config.providerId)
                                Text(
                                    text = providerLabel(config.providerId),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                            Text(
                                text = if (config.enabled) t("已启用", "Enabled") else t("未启用", "Disabled"),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.enabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    }
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onProviderSelected(config.providerId)
                    },
                )
            }
        }
    }
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
        StyledOutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text(t("默认发音人", "Default Speaker")) },
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
private fun GeminiVoiceDropdown(
    voiceDraft: String,
    onVoiceChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val knownVoices = GeminiTtsCatalog.voices
    val selectedName = knownVoices
        .firstOrNull { it.id == voiceDraft }
        ?.let { "${it.id} - ${it.tone}" }
        ?: voiceDraft

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        StyledOutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text(t("默认 Voice", "Default Voice")) },
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
                            Text(voice.id, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(voice.tone, style = MaterialTheme.typography.bodySmall)
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
private fun MimoModelDropdown(
    modelDraft: String,
    onModelChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val models = MimoTtsCatalog.models
    val selectedName = models
        .firstOrNull { it.id == modelDraft }
        ?.let { "${it.name} (${it.id})" }
        ?: modelDraft

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        StyledOutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text(t("模型", "Model")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(model.description, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        expanded = false
                        onModelChange(model.id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MimoVoiceDropdown(
    voiceDraft: String,
    onVoiceChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val voices = MimoTtsCatalog.presetVoices
    val selectedName = voices
        .firstOrNull { it.id == voiceDraft }
        ?.let { voice ->
            listOfNotNull(voice.name, voice.language, voice.gender).joinToString(" ")
        }
        ?: voiceDraft

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        StyledOutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selectedName,
            onValueChange = {},
            label = { Text(t("默认 Voice", "Default Voice")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            voices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(voice.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(voice.language, voice.gender).joinToString(" ").ifBlank { voice.id },
                                style = MaterialTheme.typography.bodySmall,
                            )
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomHttpFields(
    state: ProviderSettingsUiState,
    onMethodChange: (String) -> Unit,
    onHeadersChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onResponseTypeChange: (CustomHttpResponseType) -> Unit,
    onResponseFieldChange: (String) -> Unit,
    onApplyTemplate: (String) -> Unit,
) {
    var showAdvanced by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }
    var responseExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = t("一键填充常用接口模板", "One-tap Common API Templates"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AssistChip(
                onClick = { onApplyTemplate("OpenAI") },
                label = { Text(t("OpenAI / 兼容", "OpenAI / Compatible"), maxLines = 1) }
            )
            AssistChip(
                onClick = { onApplyTemplate("MiniMax") },
                label = { Text("MiniMax", maxLines = 1) }
            )
            AssistChip(
                onClick = { onApplyTemplate("Volcengine") },
                label = { Text(t("火山引擎", "Volcengine"), maxLines = 1) }
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                ) {
                    Text(
                        text = t("高级 HTTP 协议参数设置", "Advanced HTTP Protocol Settings"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (showAdvanced) t("收起", "Collapse") else t("展开", "Expand")
                    )
                }

                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(4.dp))

                    ExposedDropdownMenuBox(
                        expanded = methodExpanded,
                        onExpandedChange = { methodExpanded = !methodExpanded },
                    ) {
                        StyledOutlinedTextField(
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

                    StyledOutlinedTextField(
                        value = state.headersDraft,
                        onValueChange = onHeadersChange,
                        label = { Text(t("Headers 模板", "Headers Template")) },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 104.dp),
                    )
                    StyledOutlinedTextField(
                        value = state.bodyDraft,
                        onValueChange = onBodyChange,
                        label = { Text(t("JSON Body 模板", "JSON Body Template")) },
                        minLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 144.dp),
                    )

                    ExposedDropdownMenuBox(
                        expanded = responseExpanded,
                        onExpandedChange = { responseExpanded = !responseExpanded },
                    ) {
                        StyledOutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            readOnly = true,
                            value = responseTypeLabel(state.responseTypeDraft),
                            onValueChange = {},
                            label = { Text(t("Response 类型", "Response Type")) },
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
                        StyledOutlinedTextField(
                            value = state.responseFieldDraft,
                            onValueChange = onResponseFieldChange,
                            label = { Text(t("JSON 字段路径", "JSON Field Path")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private fun providerLabel(providerId: String): String =
    when (providerId) {
        ProviderConfigRepository.ANDROID_SYSTEM -> "Android System TTS"
        ProviderConfigRepository.OPENAI -> "OpenAI TTS"
        ProviderConfigRepository.EDGE_TTS_FORWARDER -> "Edge TTS Forwarder"
        ProviderConfigRepository.GEMINI -> "Gemini TTS"
        ProviderConfigRepository.MIMO -> "MiMo TTS"
        ProviderConfigRepository.CUSTOM_HTTP -> "Custom TTS API"
        else -> providerId
    }

private fun responseTypeLabel(type: CustomHttpResponseType): String =
    when (type) {
        CustomHttpResponseType.RAW_AUDIO -> "Raw audio"
        CustomHttpResponseType.JSON_BASE64_FIELD -> "JSON base64 field"
        CustomHttpResponseType.JSON_URL_FIELD -> "JSON URL field"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyledOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenericCombobox(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    readOnly: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        StyledOutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = readOnly,
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        if (options.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            expanded = false
                            onValueChange(option)
                        },
                    )
                }
            }
        }
    }
}
