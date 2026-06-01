package com.phrasevoice.ui.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProviderSettingsScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Provider",
            style = MaterialTheme.typography.headlineMedium,
        )
        ProviderCard(
            title = "Android System TTS",
            description = "系统内置语音引擎，支持直接朗读和 WAV 导出。",
            enabled = true,
            icon = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
        )
        ProviderCard(
            title = "OpenAI TTS",
            description = "默认模型将使用 gpt-4o-mini-tts，第二阶段接入。",
            enabled = false,
            icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
        )
        ProviderCard(
            title = "Gemini TTS",
            description = "保留模型名、voiceName 和风格提示入口，第二阶段接入。",
            enabled = false,
            icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
        )
        ProviderCard(
            title = "Custom HTTP",
            description = "面向 MiMo、OpenAI-compatible、自建 Edge TTS Server 等配置化服务。",
            enabled = false,
            icon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
        )
    }
}

@Composable
private fun ProviderCard(
    title: String,
    description: String,
    enabled: Boolean,
    icon: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon()
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}
