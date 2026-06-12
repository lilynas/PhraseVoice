package com.phrasevoice.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.domain.text.TextOptimizationAction
import com.phrasevoice.domain.tts.ReadingPreset
import com.phrasevoice.domain.tts.ReadingPresets
import com.phrasevoice.ui.common.QuickPhraseActionDialog
import com.phrasevoice.ui.communicate.CommunicationDisplayUiState
import com.phrasevoice.ui.communicate.LargeTextDisplayDialog
import com.phrasevoice.ui.i18n.localizedEdgeStyleName
import com.phrasevoice.ui.i18n.localizedHomeErrorMessage
import com.phrasevoice.ui.i18n.localizedPhraseGroupName
import com.phrasevoice.ui.i18n.localizedPhraseTitle
import com.phrasevoice.ui.i18n.localizedProviderHealthDescription
import com.phrasevoice.ui.i18n.localizedProviderHealthLabel
import com.phrasevoice.ui.i18n.t
import kotlin.math.abs

@Composable
fun VoiceWaveIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val transition = rememberInfiniteTransition(label = "wave")
        val heights = listOf(0.3f, 0.9f, 0.5f)
        val durations = listOf(500, 700, 600)

        heights.zip(durations).forEach { (initialHeight, duration) ->
            val scale by transition.animateFloat(
                initialValue = initialHeight,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            Canvas(
                modifier = Modifier
                    .size(width = 3.dp, height = 14.dp)
            ) {
                val h = size.height * scale
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, (size.height - h) / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    display: CommunicationDisplayUiState,
    onTextChange: (String) -> Unit,
    onProviderSelected: (String) -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onVoiceStyleSelected: (String?) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onReadingPresetSelected: (ReadingPreset) -> Unit,
    onTextOptimizationSelected: (TextOptimizationAction) -> Unit,
    onMimoSmartTextOptimizationChange: (Boolean) -> Unit,
    onSpeak: () -> Unit,
    onPreviewVoice: () -> Unit,
    onStop: () -> Unit,
    onSaveAudio: () -> Unit,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseEdit: (Phrase) -> Unit,
    onQuickPhraseFavoriteToggle: (Phrase) -> Unit,
    onQuickPhraseGroupSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectedProvider = state.providers.firstOrNull { it.id == state.selectedProviderId }
    val androidTtsUnavailable = state.selectedProviderId == "android_system" && !state.androidTtsReady
    val selectedProviderUnavailable = selectedProvider?.enabled == false
    val canRunTts = state.status != HomeStatus.Loading &&
            state.status != HomeStatus.Saving &&
            !androidTtsUnavailable &&
            selectedProvider?.enabled == true
    val shareTitle = t("分享音频", "Share Audio")
    var showLargeText by remember { mutableStateOf(false) }
    var phraseAction by remember { mutableStateOf<Phrase?>(null) }

    if (showLargeText) {
        LargeTextDisplayDialog(
            text = state.text,
            display = display,
            isPlaying = state.status == HomeStatus.Playing,
            canSpeak = canRunTts && state.text.isNotBlank(),
            onSpeak = onSpeak,
            onStop = onStop,
            onReplay = {
                onStop()
                onSpeak()
            },
            onDismiss = { showLargeText = false },
        )
    }
    phraseAction?.let { phrase ->
        QuickPhraseActionDialog(
            phrase = phrase,
            onDismiss = { phraseAction = null },
            onEdit = onQuickPhraseEdit,
            onToggleFavorite = onQuickPhraseFavoriteToggle,
        )
    }

    @Composable
    fun HeaderSection() {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PhraseVoice",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    @Composable
    fun ReadingTextPanel(expanded: Boolean, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = t("朗读文本", "Text to Read"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.status == HomeStatus.Playing) {
                        VoiceWaveIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = when (state.status) {
                            HomeStatus.Loading -> t("准备中", "Preparing")
                            HomeStatus.Playing -> t("正在朗读", "Speaking")
                            else -> t("待朗读", "Ready")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { showLargeText = true },
                        enabled = state.text.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fullscreen,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp).size(18.dp),
                        )
                        Text(t("全屏", "Full"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedTextField(
                value = state.text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        t("点击短语或输入文字开始朗读", "Tap a phrase or enter text to read"),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                    )
                },
                minLines = if (expanded) 4 else 5,
                maxLines = if (expanded) 8 else 10,
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (expanded) 170.dp else 190.dp),
            )
        }
    }

    @Composable
    fun QuickPhrasesSection(
        compactCards: Boolean,
        modifier: Modifier = Modifier,
    ) {
        @Composable
        fun QuickPhraseButton(
            phrase: Phrase,
            modifier: Modifier = Modifier,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = modifier
                    .heightIn(min = 66.dp)
                    .combinedClickable(
                        onClick = { onQuickPhraseClick(phrase) },
                        onLongClick = { phraseAction = phrase },
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = localizedPhraseTitle(phrase),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = phrase.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = t("快捷短语", "Quick Phrases"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
                Text(
                    text = t("${state.quickPhrases.size} 条", "${state.quickPhrases.size} shown"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item("all") {
                    FilterChip(
                        selected = state.selectedQuickPhraseGroupId == null,
                        onClick = { onQuickPhraseGroupSelected(null) },
                        label = { Text(t("全部", "All")) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                item(QUICK_PHRASE_FAVORITES_FILTER_ID) {
                    FilterChip(
                        selected = state.selectedQuickPhraseGroupId == QUICK_PHRASE_FAVORITES_FILTER_ID,
                        onClick = { onQuickPhraseGroupSelected(QUICK_PHRASE_FAVORITES_FILTER_ID) },
                        label = { Text(t("收藏", "Favorites")) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                items(state.quickPhraseGroups, key = { it.id }) { group ->
                    FilterChip(
                        selected = state.selectedQuickPhraseGroupId == group.id,
                        onClick = { onQuickPhraseGroupSelected(group.id) },
                        label = {
                            Text(
                                text = localizedPhraseGroupName(group),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            if (state.quickPhrases.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = t("这个分组还没有短语", "No phrases in this group yet"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (compactCards) {
                        state.quickPhrases.forEach { phrase ->
                            QuickPhraseButton(
                                phrase = phrase,
                                modifier = Modifier.widthIn(min = 136.dp, max = 220.dp),
                            )
                        }
                    } else {
                        state.quickPhrases.chunked(3).forEach { rowPhrases ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowPhrases.forEach { phrase ->
                                    QuickPhraseButton(
                                        phrase = phrase,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowPhrases.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ControlsSection() {
        TextOptimizationRow(
            enabled = state.text.isNotBlank(),
            feedback = state.textOptimizationFeedback,
            onTextOptimizationSelected = onTextOptimizationSelected,
        )

        if (state.mimoSmartTextOptimizationAvailable) {
            MimoSmartTextOptimizationRow(
                checked = state.mimoSmartTextOptimizationEnabled,
                enabled = state.status != HomeStatus.Loading && state.status != HomeStatus.Saving,
                onCheckedChange = onMimoSmartTextOptimizationChange,
            )
        }
    }

    @Composable
    fun PrimaryActionsSection() {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSpeak,
                        enabled = canRunTts,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (state.status == HomeStatus.Playing) {
                            VoiceWaveIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(end = 8.dp))
                        } else {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(
                            text = when (state.status) {
                                HomeStatus.Loading -> t("准备中...", "Preparing...")
                                HomeStatus.Playing -> t("播放中", "Playing")
                                else -> t("开始朗读", "Read")
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.status == HomeStatus.Playing) {
                        Button(
                            onClick = onStop,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text(t("停止", "Stop"), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSaveAudio,
                            enabled = canRunTts,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text(if (state.status == HomeStatus.Saving) t("保存中", "Saving") else t("保存", "Save"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (!state.lastAudioUri.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            shareAudio(
                                context = context,
                                uriString = state.lastAudioUri,
                                mimeType = state.lastAudioMimeType,
                                chooserTitle = shareTitle,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(t("分享生成的音频", "Share Generated Audio"), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    @Composable
    fun StatusSection() {
        if (state.status == HomeStatus.Error ||
            !state.errorMessage.isNullOrBlank() ||
            !state.noticeMessage.isNullOrBlank() ||
            androidTtsUnavailable ||
            selectedProviderUnavailable
        ) {
            val isNotice = state.status != HomeStatus.Error &&
                    !state.noticeMessage.isNullOrBlank() &&
                    !androidTtsUnavailable &&
                    !selectedProviderUnavailable
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isNotice) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                ),
                border = BorderStroke(
                    1.dp,
                    if (isNotice) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    },
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (androidTtsUnavailable) {
                            t(
                                "系统 TTS 未就绪，请在系统设置中启用或更换 Provider",
                                "System TTS is not ready. Enable it in system settings or switch Provider.",
                            )
                        } else if (selectedProviderUnavailable && selectedProvider != null) {
                            localizedProviderHealthDescription(
                                status = selectedProvider.status,
                                providerName = selectedProvider.name,
                                androidTtsMessage = state.androidTtsMessage,
                            )
                        } else {
                            state.errorMessage?.let { localizedHomeErrorMessage(it) }
                                ?: state.noticeMessage
                                ?: t("发生未知错误", "Unknown error")
                        },
                        color = if (isNotice) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Composable
    fun PresetsSection() {
        ReadingPresetRow(
            state = state,
            onReadingPresetSelected = onReadingPresetSelected,
        )
    }

    @Composable
    fun EngineSection() {
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = t("声音引擎配置", "Voice Engine"),
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

                OutlinedButton(
                    onClick = onPreviewVoice,
                    enabled = canRunTts,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(t("试听当前声音", "Preview Current Voice"), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    @Composable
    fun VoicePropertiesSection() {
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = t("声音属性设置", "Voice Properties"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                SliderItem(
                    label = t("语速", "Speed"),
                    value = state.speed,
                    range = 0.5f..2.0f,
                    startIcon = Icons.Outlined.DirectionsWalk,
                    endIcon = Icons.Outlined.DirectionsRun,
                    onChange = onSpeedChange
                )
                SliderItem(
                    label = t("音调", "Pitch"),
                    value = state.pitch,
                    range = 0.5f..2.0f,
                    startIcon = Icons.Outlined.ArrowDownward,
                    endIcon = Icons.Outlined.ArrowUpward,
                    onChange = onPitchChange
                )
                SliderItem(
                    label = t("音量", "Volume"),
                    value = state.volume,
                    range = 0.0f..1.0f,
                    startIcon = Icons.AutoMirrored.Outlined.VolumeMute,
                    endIcon = Icons.AutoMirrored.Outlined.VolumeUp,
                    onChange = onVolumeChange
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 720.dp
        if (wideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HeaderSection()
                    ReadingTextPanel(expanded = true)
                    QuickPhrasesSection(compactCards = true)
                }
                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ControlsSection()
                    PrimaryActionsSection()
                    StatusSection()
                    PresetsSection()
                    EngineSection()
                    VoicePropertiesSection()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderSection()
                ReadingTextPanel(expanded = false)
                QuickPhrasesSection(compactCards = false)
                ControlsSection()
                PrimaryActionsSection()
                StatusSection()
                PresetsSection()
                EngineSection()
                VoicePropertiesSection()
            }
        }
    }

}

@Composable
private fun TextOptimizationRow(
    enabled: Boolean,
    feedback: TextOptimizationFeedback?,
    onTextOptimizationSelected: (TextOptimizationAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = t("文本工具", "Text Tools"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(TextOptimizationAction.entries, key = { it.name }) { action ->
                SuggestionChip(
                    onClick = { onTextOptimizationSelected(action) },
                    enabled = enabled,
                    label = {
                        Text(
                            text = textOptimizationLabel(action),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }
        feedback?.let {
            Text(
                text = textOptimizationFeedbackLabel(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MimoSmartTextOptimizationRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = t("MiMo 智能文本优化", "MiMo Smart Text Optimization"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            )
            Text(
                text = t(
                    "开启后，正式朗读会让 MiMo 先优化文本；关闭时严格按原文朗读。",
                    "When enabled, real reading lets MiMo optimize the text first; when off, it reads the original text.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ReadingPresetRow(
    state: HomeUiState,
    onReadingPresetSelected: (ReadingPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = t("朗读场景", "Reading Presets"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(ReadingPresets.all, key = { it.id }) { preset ->
                FilterChip(
                    selected = state.matchesPreset(preset),
                    onClick = { onReadingPresetSelected(preset) },
                    label = {
                        Text(
                            text = readingPresetLabel(preset.id),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }
}

@Composable
private fun textOptimizationLabel(action: TextOptimizationAction): String =
    when (action) {
        TextOptimizationAction.OneTapPolish -> t("一键优化", "Polish")
        TextOptimizationAction.CleanWhitespace -> t("清理空白", "Clean Spacing")
        TextOptimizationAction.AddReadingBreaks -> t("朗读分段", "Add Pauses")
        TextOptimizationAction.MixedLanguageSpacing -> t("中英间隔", "CJK/EN Spacing")
    }

@Composable
private fun readingPresetLabel(id: String): String =
    when (id) {
        ReadingPresets.NATURAL -> t("自然播报", "Natural")
        ReadingPresets.GENTLE -> t("温柔讲解", "Gentle")
        ReadingPresets.NOTICE -> t("客服通知", "Notice")
        ReadingPresets.SHORT_VIDEO -> t("短视频旁白", "Short Video")
        ReadingPresets.ROLE_PLAY -> t("角色扮演", "Role Play")
        ReadingPresets.ENGLISH_PRACTICE -> t("英语跟读", "English Practice")
        else -> id
    }

@Composable
private fun textOptimizationFeedbackLabel(feedback: TextOptimizationFeedback): String =
    when (feedback) {
        TextOptimizationFeedback.CleanedWhitespace -> t("已清理空白", "Whitespace cleaned")
        TextOptimizationFeedback.AddedReadingBreaks -> t("已添加朗读分段", "Reading breaks added")
        TextOptimizationFeedback.AddedMixedLanguageSpacing -> t("已整理中英间隔", "CJK/English spacing updated")
        TextOptimizationFeedback.Polished -> t("已一键优化朗读文本", "Text polished for reading")
        TextOptimizationFeedback.NoChange -> t("文本已经适合朗读，无需调整", "Text already looks ready for reading")
    }

private fun HomeUiState.matchesPreset(preset: ReadingPreset): Boolean =
    abs(speed - preset.speed) < 0.01f &&
        abs(pitch - preset.pitch) < 0.01f &&
        abs(volume - preset.volume) < 0.01f

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
            state.providers.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        val note = provider.note?.let { localizedProviderHealthLabel(provider.status) }
                        Text(
                            text = listOfNotNull(provider.name, note).joinToString(" · "),
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
            value = selected?.let { localizedEdgeStyleName(it) }.orEmpty(),
            onValueChange = {},
            label = { Text(t("风格", "Style")) },
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
            state.voiceStyles.forEach { style ->
                DropdownMenuItem(
                    text = { Text(localizedEdgeStyleName(style)) },
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
private fun SliderItem(
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

        Spacer(modifier = Modifier.height(4.dp))

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
                    Canvas(
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

                        // Active track with linear gradient
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

private fun shareAudio(
    context: Context,
    uriString: String,
    mimeType: String?,
    chooserTitle: String,
) {
    val uri = Uri.parse(uriString)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType ?: "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}
