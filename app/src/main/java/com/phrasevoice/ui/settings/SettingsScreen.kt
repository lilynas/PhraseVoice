package com.phrasevoice.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.debug.DebugLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDefaultProviderChange: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onAutoSaveHistoryChange: (Boolean) -> Unit,
    onKeepAudioCacheChange: (Boolean) -> Unit,
    onClearAudioCache: () -> Unit,
    onClearDebugLogs: () -> Unit,
    onRefreshAudioCache: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        onRefreshAudioCache()
    }

    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "设置", style = MaterialTheme.typography.headlineMedium)
        SettingsCard {
            Text(text = "默认 Provider", style = MaterialTheme.typography.titleMedium)
            DefaultProviderDropdown(
                providers = state.providers,
                selectedProviderId = settings.defaultProviderId,
                onDefaultProviderChange = onDefaultProviderChange,
            )
        }
        SettingsCard {
            SliderSetting("默认语速", settings.defaultSpeed, 0.5f..2.0f, onSpeedChange)
            SliderSetting("默认音调", settings.defaultPitch, 0.5f..2.0f, onPitchChange)
            SliderSetting("默认音量", settings.defaultVolume, 0.0f..1.0f, onVolumeChange)
        }
        SettingsCard {
            SwitchSetting("自动保存历史", settings.autoSaveHistory, onAutoSaveHistoryChange)
            SwitchSetting("保留音频缓存", settings.keepAudioCache, onKeepAudioCacheChange)
        }
        AudioCacheCard(
            state = state,
            onClearAudioCache = onClearAudioCache,
            onRefreshAudioCache = onRefreshAudioCache,
        )
        DebugLogCard(
            logs = state.debugLogs,
            onClearDebugLogs = onClearDebugLogs,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultProviderDropdown(
    providers: List<SettingsProviderOption>,
    selectedProviderId: String,
    onDefaultProviderChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = providers.firstOrNull { it.id == selectedProviderId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.name ?: selectedProviderId,
            onValueChange = {},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            providers.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = listOfNotNull(provider.name, provider.note).joinToString(" · "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    enabled = provider.enabled,
                    onClick = {
                        expanded = false
                        onDefaultProviderChange(provider.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, modifier = Modifier.weight(1f))
            Text(text = "%.2f".format(value))
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun AudioCacheCard(
    state: SettingsUiState,
    onClearAudioCache: () -> Unit,
    onRefreshAudioCache: () -> Unit,
) {
    SettingsCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "音频缓存",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefreshAudioCache) {
                Text("刷新")
            }
            TextButton(
                onClick = onClearAudioCache,
                enabled = state.audioCacheInfo.fileCount > 0,
            ) {
                Text("清理")
            }
        }
        Text(
            text = "${state.audioCacheInfo.fileCount} 个文件 · ${formatBytes(state.audioCacheInfo.totalBytes)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        state.cacheMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DebugLogCard(
    logs: List<DebugLogEntry>,
    onClearDebugLogs: () -> Unit,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    SettingsCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "调试日志",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClearDebugLogs) {
                Text("清空")
            }
        }
        Text(
            text = "临时诊断用。可直接截图给我，日志不会显示 API Key。",
            style = MaterialTheme.typography.bodySmall,
        )
        if (logs.isEmpty()) {
            Text(text = "暂无日志", style = MaterialTheme.typography.bodySmall)
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    logs.takeLast(30).asReversed().forEach { entry ->
                        Text(
                            text = buildString {
                                append(timeFormat.format(Date(entry.timestampMillis)))
                                append(" ")
                                append(entry.level)
                                append("/")
                                append(entry.tag)
                                append(": ")
                                append(entry.message)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }
    return "%.1f %s".format(Locale.US, value, units[unitIndex])
}
