package com.phrasevoice.ui.communicate

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.AudioClip
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.ui.home.HomeStatus
import com.phrasevoice.ui.home.HomeUiState
import com.phrasevoice.ui.home.QUICK_PHRASE_FAVORITES_FILTER_ID
import com.phrasevoice.ui.home.TtsProviderOption
import com.phrasevoice.ui.home.VoiceWaveIndicator
import com.phrasevoice.ui.audio.AudioClipsUiState
import com.phrasevoice.ui.i18n.localizedHomeErrorMessage
import com.phrasevoice.ui.i18n.localizedPhraseGroupName
import com.phrasevoice.ui.i18n.localizedPhraseTitle
import com.phrasevoice.ui.i18n.localizedProviderHealthDescription
import com.phrasevoice.ui.i18n.t

data class ContactCardUiState(
    val name: String,
    val subtitle: String,
    val account: String,
    val qrContent: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunicateScreen(
    state: HomeUiState,
    contactCard: ContactCardUiState,
    audioClipsState: AudioClipsUiState,
    onTextChange: (String) -> Unit,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseGroupSelected: (String?) -> Unit,
    onAudioClipImport: (Uri) -> Unit,
    onAudioClipClick: (AudioClip) -> Unit,
    onAudioClipDelete: (String) -> Unit,
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
    var showContactCard by remember { mutableStateOf(false) }
    val audioImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) onAudioClipImport(uri)
    }

    if (showContactCard) {
        ContactCardDialog(
            contactCard = contactCard,
            onDismiss = { showContactCard = false },
        )
    }

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
                        onContactCardClick = { showContactCard = true },
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
                    AudioClipsPanel(
                        state = audioClipsState,
                        compactCards = true,
                        onImportClick = { audioImportLauncher.launch(arrayOf("audio/*")) },
                        onAudioClipClick = onAudioClipClick,
                        onAudioClipDelete = onAudioClipDelete,
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
                    onContactCardClick = { showContactCard = true },
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
                AudioClipsPanel(
                    state = audioClipsState,
                    compactCards = false,
                    onImportClick = { audioImportLauncher.launch(arrayOf("audio/*")) },
                    onAudioClipClick = onAudioClipClick,
                    onAudioClipDelete = onAudioClipDelete,
                )
            }
        }
    }
}

@Composable
private fun CommunicateHeader(
    state: HomeUiState,
    providerName: String?,
    onContactCardClick: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onContactCardClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(t("扩列", "Card"), fontWeight = FontWeight.SemiBold)
            }
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
private fun ContactCardDialog(
    contactCard: ContactCardUiState,
    onDismiss: () -> Unit,
) {
    val displayName = contactCard.name.ifBlank { t("PhraseVoice", "PhraseVoice") }
    val subtitle = contactCard.subtitle.ifBlank { t("很高兴认识你", "Nice to meet you") }
    val account = contactCard.account.ifBlank { t("未填写账号", "No account set") }
    val qrContent = contactCard.qrContent
        .ifBlank { contactCard.account }
        .ifBlank { displayName }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text(t("收起", "Close"))
            }
        },
        title = {
            Text(
                text = t("扩列名片", "Contact Card"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = account,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    QrCodeImage(
                        content = qrContent,
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp),
                    )
                }
            }
        },
    )
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
@OptIn(ExperimentalLayoutApi::class)
private fun AudioClipsPanel(
    state: AudioClipsUiState,
    compactCards: Boolean,
    onImportClick: () -> Unit,
    onAudioClipClick: (AudioClip) -> Unit,
    onAudioClipDelete: (String) -> Unit,
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
            Column {
                Text(
                    text = t("快捷音频", "Audio Clips"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                )
                state.message?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(
                onClick = onImportClick,
                enabled = !state.isImporting,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = if (state.isImporting) t("导入中", "Importing") else t("导入", "Import"),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (state.clips.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = t("导入常用音效或语音文件后，会显示在这里。", "Imported sound effects and voice files will appear here."),
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
                state.clips.forEach { clip ->
                    AudioClipButton(
                        clip = clip,
                        onClick = { onAudioClipClick(clip) },
                        onDelete = { onAudioClipDelete(clip.id) },
                        modifier = if (compactCards) {
                            Modifier.widthIn(min = 152.dp, max = 240.dp)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioClipButton(
    clip: AudioClip,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Audiotrack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = clip.mimeType.substringAfter('/').uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
