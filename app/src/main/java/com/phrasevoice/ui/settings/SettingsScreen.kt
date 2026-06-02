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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.phrasevoice.debug.DebugLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onAutoSaveHistoryChange: (Boolean) -> Unit,
    onKeepAudioCacheChange: (Boolean) -> Unit,
    onClearDebugLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            SliderSetting("默认语速", settings.defaultSpeed, 0.5f..2.0f, onSpeedChange)
            SliderSetting("默认音调", settings.defaultPitch, 0.5f..2.0f, onPitchChange)
            SliderSetting("默认音量", settings.defaultVolume, 0.0f..1.0f, onVolumeChange)
        }
        SettingsCard {
            SwitchSetting("自动保存历史", settings.autoSaveHistory, onAutoSaveHistoryChange)
            SwitchSetting("保留音频缓存", settings.keepAudioCache, onKeepAudioCacheChange)
        }
        DebugLogCard(
            logs = state.debugLogs,
            onClearDebugLogs = onClearDebugLogs,
        )
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
