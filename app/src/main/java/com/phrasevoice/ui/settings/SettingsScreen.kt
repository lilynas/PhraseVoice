package com.phrasevoice.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    onThemeModeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        onRefreshAudioCache()
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        SettingsCard {
            Text(
                text = "主题设置",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("system", "默认", Icons.Outlined.BrightnessAuto),
                    Triple("light", "白色", Icons.Outlined.LightMode),
                    Triple("dark", "黑色", Icons.Outlined.DarkMode)
                ).forEach { (mode, label, icon) ->
                    val isSelected = settings.themeMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SettingsCard {
            Text(
                text = "默认 Provider",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            DefaultProviderDropdown(
                providers = state.providers,
                selectedProviderId = settings.defaultProviderId,
                onDefaultProviderChange = onDefaultProviderChange,
            )
        }

        SettingsCard {
            Text(
                text = "发音属性默认值",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            SliderSetting(
                label = "默认语速",
                value = settings.defaultSpeed,
                range = 0.5f..2.0f,
                startIcon = Icons.Outlined.DirectionsWalk,
                endIcon = Icons.Outlined.DirectionsRun,
                onChange = onSpeedChange
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSetting(
                label = "默认音调",
                value = settings.defaultPitch,
                range = 0.5f..2.0f,
                startIcon = Icons.Outlined.ArrowDownward,
                endIcon = Icons.Outlined.ArrowUpward,
                onChange = onPitchChange
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSetting(
                label = "默认音量",
                value = settings.defaultVolume,
                range = 0.0f..1.0f,
                startIcon = Icons.AutoMirrored.Outlined.VolumeMute,
                endIcon = Icons.AutoMirrored.Outlined.VolumeUp,
                onChange = onVolumeChange
            )
        }

        SettingsCard {
            Text(
                text = "存储与历史",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            SwitchSetting("自动保存历史", settings.autoSaveHistory, onAutoSaveHistoryChange)
            SwitchSetting("保留音频缓存", settings.keepAudioCache, onKeepAudioCacheChange)
        }

        AudioCacheCard(
            state = state,
            onClearAudioCache = onClearAudioCache,
            onRefreshAudioCache = onRefreshAudioCache,
        )

        // 关于卡片
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutDialog = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "关于 PhraseVoice",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

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
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    startIcon: ImageVector,
    endIcon: ImageVector,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = startIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val thumbSizeAnim by animateDpAsState(
                targetValue = if (isPressed) 22.dp else 16.dp,
                label = "thumbSize"
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                interactionSource = interactionSource,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                thumb = { _ ->
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(thumbColor = primaryColor),
                        modifier = Modifier.size(thumbSizeAnim)
                    )
                },
                track = { sliderState ->
                    val fraction = (sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        val centerY = height / 2f
                        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f)

                        // Inactive track
                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 3.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(width, 6.dp.toPx()),
                            cornerRadius = cornerRadius
                        )

                        // Active track with linear gradient!
                        drawRoundRect(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(primaryColor, tertiaryColor)
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 3.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(width * fraction, 6.dp.toPx()),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            )

            Icon(
                imageVector = endIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
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

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        title = {
            Text(
                text = "关于 PhraseVoice",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PhraseVoice v1.0.0",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "一键配置，开箱即用的极简语音合成器",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val uriHandler = LocalUriHandler.current

                AboutLinkItem(
                    label = "作者",
                    value = "shirone",
                    onClick = { runCatching { uriHandler.openUri("https://github.com/lilynas") } }
                )
                AboutLinkItem(
                    label = "项目开源地址",
                    value = "PhraseVoice",
                    onClick = { runCatching { uriHandler.openUri("https://github.com/lilynas/PhraseVoice") } }
                )
                AboutLinkItem(
                    label = "问题反馈",
                    value = "提交 Issue",
                    onClick = { runCatching { uriHandler.openUri("https://github.com/lilynas/PhraseVoice/issues") } }
                )
            }
        }
    )
}

@Composable
private fun AboutLinkItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline
            )
        )
    }
}
