package com.phrasevoice.ui.communicate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.ui.home.HomeStatus
import com.phrasevoice.ui.home.HomeUiState
import com.phrasevoice.ui.home.QUICK_PHRASE_FAVORITES_FILTER_ID
import com.phrasevoice.ui.home.TtsProviderOption
import com.phrasevoice.ui.home.VoiceWaveIndicator
import com.phrasevoice.ui.i18n.localizedHomeErrorMessage
import com.phrasevoice.ui.i18n.localizedPhraseGroupName
import com.phrasevoice.ui.i18n.localizedPhraseTitle
import com.phrasevoice.ui.i18n.localizedProviderHealthDescription
import com.phrasevoice.ui.i18n.t

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunicateScreen(
    state: HomeUiState,
    onTextChange: (String) -> Unit,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseGroupSelected: (String?) -> Unit,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedProvider = state.providers.firstOrNull { it.id == state.selectedProviderId }
    val androidTtsUnavailable = state.selectedProviderId == ProviderConfigRepository.ANDROID_SYSTEM && !state.androidTtsReady
    val selectedProviderUnavailable = selectedProvider?.enabled == false
    val canRunTts = state.status != HomeStatus.Loading &&
        state.status != HomeStatus.Saving &&
        !androidTtsUnavailable &&
        selectedProvider?.enabled == true
    val hasText = state.text.isNotBlank()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 760.dp
        if (wideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CommunicateHeader(
                        state = state,
                        providerName = selectedProvider?.name,
                    )
                    TalkingTextField(
                        text = state.text,
                        onTextChange = onTextChange,
                        expanded = true,
                        modifier = Modifier.weight(1f),
                    )
                    CommunicateActionRow(
                        status = state.status,
                        canRunTts = canRunTts,
                        hasText = hasText,
                        onSpeak = onSpeak,
                        onStop = onStop,
                        onReplay = onReplay,
                    )
                    CommunicateStatusMessage(
                        state = state,
                        selectedProvider = selectedProvider,
                        androidTtsUnavailable = androidTtsUnavailable,
                        selectedProviderUnavailable = selectedProviderUnavailable,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QuickPhrasePanel(
                        state = state,
                        compactCards = true,
                        onQuickPhraseClick = onQuickPhraseClick,
                        onQuickPhraseGroupSelected = onQuickPhraseGroupSelected,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CommunicateHeader(
                    state = state,
                    providerName = selectedProvider?.name,
                )
                TalkingTextField(
                    text = state.text,
                    onTextChange = onTextChange,
                    expanded = false,
                )
                CommunicateActionRow(
                    status = state.status,
                    canRunTts = canRunTts,
                    hasText = hasText,
                    onSpeak = onSpeak,
                    onStop = onStop,
                    onReplay = onReplay,
                )
                CommunicateStatusMessage(
                    state = state,
                    selectedProvider = selectedProvider,
                    androidTtsUnavailable = androidTtsUnavailable,
                    selectedProviderUnavailable = selectedProviderUnavailable,
                )
                QuickPhrasePanel(
                    state = state,
                    compactCards = false,
                    onQuickPhraseClick = onQuickPhraseClick,
                    onQuickPhraseGroupSelected = onQuickPhraseGroupSelected,
                )
            }
        }
    }
}

@Composable
private fun CommunicateHeader(
    state: HomeUiState,
    providerName: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = t("交流", "Talk"),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = providerName ?: t("正在载入声音", "Loading voice"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

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
                    HomeStatus.Saving -> t("保存中", "Saving")
                    HomeStatus.Error -> t("需要处理", "Needs attention")
                    else -> t("待朗读", "Ready")
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TalkingTextField(
    text: String,
    onTextChange: (String) -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = {
            Text(
                text = t("点按短语，或直接输入要说的话", "Tap a phrase or type what you want to say"),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.58f),
            )
        },
        minLines = if (expanded) 9 else 5,
        maxLines = if (expanded) 16 else 9,
        shape = RoundedCornerShape(22.dp),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
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
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (expanded) 360.dp else 220.dp),
    )
}

@Composable
private fun CommunicateActionRow(
    status: HomeStatus,
    canRunTts: Boolean,
    hasText: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
) {
    val isPlaying = status == HomeStatus.Playing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = if (isPlaying) onStop else onSpeak,
            enabled = isPlaying || (canRunTts && hasText),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1.35f)
                .height(54.dp),
            colors = if (isPlaying) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            },
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = when {
                    isPlaying -> t("停止", "Stop")
                    status == HomeStatus.Loading -> t("准备中", "Preparing")
                    else -> t("朗读", "Speak")
                },
                fontWeight = FontWeight.Bold,
            )
        }

        OutlinedButton(
            onClick = onReplay,
            enabled = canRunTts && hasText,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Replay,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(t("重播", "Replay"), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CommunicateStatusMessage(
    state: HomeUiState,
    selectedProvider: TtsProviderOption?,
    androidTtsUnavailable: Boolean,
    selectedProviderUnavailable: Boolean,
) {
    val message = when {
        androidTtsUnavailable -> t(
            "系统 TTS 还没准备好，请在系统设置中启用或切换 Provider",
            "System TTS is not ready. Enable it in system settings or switch Provider.",
        )
        selectedProviderUnavailable && selectedProvider != null -> localizedProviderHealthDescription(
            status = selectedProvider.status,
            providerName = selectedProvider.name,
            androidTtsMessage = state.androidTtsMessage,
        )
        state.status == HomeStatus.Error && !state.errorMessage.isNullOrBlank() ->
            localizedHomeErrorMessage(state.errorMessage)
        else -> null
    }

    if (message != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickPhrasePanel(
    state: HomeUiState,
    compactCards: Boolean,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseGroupSelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
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
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = t("这个分组还没有短语", "No phrases in this group yet"),
                    style = MaterialTheme.typography.bodyMedium,
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
                            onClick = { onQuickPhraseClick(phrase) },
                            modifier = Modifier.widthIn(min = 152.dp, max = 230.dp),
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
                                    onClick = { onQuickPhraseClick(phrase) },
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
private fun QuickPhraseButton(
    phrase: Phrase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
        modifier = modifier.heightIn(min = 68.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
