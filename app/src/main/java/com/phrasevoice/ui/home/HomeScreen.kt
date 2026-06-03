package com.phrasevoice.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.Phrase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onTextChange: (String) -> Unit,
    onProviderSelected: (String) -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onVoiceStyleSelected: (String?) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onSaveAudio: () -> Unit,
    onQuickPhraseClick: (Phrase) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val androidTtsUnavailable = state.selectedProviderId == "android_system" && !state.androidTtsReady
    val canRunTts = state.status != HomeStatus.Loading &&
        state.status != HomeStatus.Saving &&
        !androidTtsUnavailable

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "PhraseVoice",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "声音引擎配置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                ProviderDropdown(
                    state = state,
                    onProviderSelected = onProviderSelected,
                )

                VoiceDropdown(
                    state = state,
                    onVoiceSelected = onVoiceSelected,
                )

                if (state.voiceStyles.isNotEmpty()) {
                    VoiceStyleDropdown(
                        state = state,
                        onVoiceStyleSelected = onVoiceStyleSelected,
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            label = { Text("朗读文本") },
            minLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )

        SliderRow(
            label = "语速",
            value = state.speed,
            range = 0.5f..2.0f,
            startIcon = Icons.Outlined.DirectionsWalk,
            endIcon = Icons.Outlined.DirectionsRun,
            onChange = onSpeedChange
        )
        SliderRow(
            label = "音调",
            value = state.pitch,
            range = 0.5f..2.0f,
            startIcon = Icons.Outlined.ArrowDownward,
            endIcon = Icons.Outlined.ArrowUpward,
            onChange = onPitchChange
        )
        SliderRow(
            label = "音量",
            value = state.volume,
            range = 0.0f..1.0f,
            startIcon = Icons.AutoMirrored.Outlined.VolumeMute,
            endIcon = Icons.AutoMirrored.Outlined.VolumeUp,
            onChange = onVolumeChange
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSpeak,
                enabled = canRunTts,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Text(if (state.status == HomeStatus.Loading) "准备中" else "朗读")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                    Text("停止")
                }
                OutlinedButton(
                    onClick = onSaveAudio,
                    enabled = canRunTts,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Text(if (state.status == HomeStatus.Saving) "保存中" else "保存")
                }
            }
            if (!state.lastAudioUri.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        shareAudio(
                            context = context,
                            uriString = state.lastAudioUri,
                            mimeType = state.lastAudioMimeType,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Text("分享音频")
                }
            }
        }

        StatusPanel(state = state)

        Text(text = "常用语", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.quickPhrases, key = { it.id }) { phrase ->
                FilterChip(
                    selected = phrase.isFavorite,
                    onClick = { onQuickPhraseClick(phrase) },
                    label = {
                        Text(
                            text = phrase.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDropdown(
    state: HomeUiState,
    onProviderSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.providers.firstOrNull { it.id == state.selectedProviderId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.name.orEmpty(),
            onValueChange = {},
            label = { Text("Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.providers.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = listOfNotNull(provider.name, provider.note).joinToString(" · "),
                        )
                    },
                    onClick = {
                        expanded = false
                        onProviderSelected(provider.id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceStyleDropdown(
    state: HomeUiState,
    onVoiceStyleSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.voiceStyles.firstOrNull { it.id == state.selectedVoiceStyleId }
        ?: state.voiceStyles.firstOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.name.orEmpty(),
            onValueChange = {},
            label = { Text("风格") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.voiceStyles.forEach { style ->
                DropdownMenuItem(
                    text = { Text(style.name) },
                    onClick = {
                        expanded = false
                        onVoiceStyleSelected(style.id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceDropdown(
    state: HomeUiState,
    onVoiceSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = state.voices.firstOrNull { it.id == state.selectedVoiceId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            value = selected?.let { "${it.name} ${it.language.orEmpty()}" }.orEmpty(),
            onValueChange = {},
            label = { Text("Voice") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            state.voices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(voice.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = listOfNotNull(voice.language, voice.description).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onVoiceSelected(voice.id)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    startIcon: ImageVector,
    endIcon: ImageVector,
    onChange: (Float) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "%.2f".format(value),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = startIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val thumbSizeAnim by animateDpAsState(
                    targetValue = if (isPressed) 24.dp else 18.dp,
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
                        .padding(horizontal = 10.dp),
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
                                .height(8.dp)
                        ) {
                            val width = size.width
                            val height = size.height
                            val centerY = height / 2f
                            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2f, height / 2f)

                            // Inactive track
                            drawRoundRect(
                                color = inactiveColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 4.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(width, 8.dp.toPx()),
                                cornerRadius = cornerRadius
                            )

                            // Active track with linear gradient!
                            drawRoundRect(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(primaryColor, tertiaryColor)
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 4.dp.toPx()),
                                size = androidx.compose.ui.geometry.Size(width * fraction, 8.dp.toPx()),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                )

                Icon(
                    imageVector = endIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusPanel(state: HomeUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state.status) {
                HomeStatus.Error -> MaterialTheme.colorScheme.errorContainer
                HomeStatus.Playing -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AssistChip(
                onClick = {},
                label = { Text(statusLabel(state.status)) },
            )
            if (!state.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            if (!state.lastAudioUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "音频已生成，可分享",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun statusLabel(status: HomeStatus): String =
    when (status) {
        HomeStatus.Idle -> "空闲"
        HomeStatus.Loading -> "准备中"
        HomeStatus.Playing -> "播放中"
        HomeStatus.Saving -> "保存中"
        HomeStatus.Error -> "出错"
    }

private fun shareAudio(
    context: Context,
    uriString: String,
    mimeType: String?,
) {
    val uri = Uri.parse(uriString)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType ?: "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "分享音频"))
}
