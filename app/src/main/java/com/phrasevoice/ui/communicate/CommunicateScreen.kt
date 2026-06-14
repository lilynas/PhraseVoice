package com.phrasevoice.ui.communicate

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import com.phrasevoice.data.model.AudioClip
import com.phrasevoice.data.model.DisplayCard
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.domain.text.TextOptimizationAction
import com.phrasevoice.domain.tts.ReadingPreset
import com.phrasevoice.domain.tts.ReadingPresets
import com.phrasevoice.ui.common.QuickPhraseActionDialog
import com.phrasevoice.ui.home.HomeStatus
import com.phrasevoice.ui.home.HomeUiState
import com.phrasevoice.ui.home.QUICK_PHRASE_FAVORITES_FILTER_ID
import com.phrasevoice.ui.home.TextOptimizationFeedback
import com.phrasevoice.ui.home.TtsProviderOption
import com.phrasevoice.ui.audio.AudioClipsUiState
import com.phrasevoice.ui.i18n.localizedEdgeStyleName
import com.phrasevoice.ui.i18n.localizedHomeErrorMessage
import com.phrasevoice.ui.i18n.localizedPhraseGroupName
import com.phrasevoice.ui.i18n.localizedPhraseTitle
import com.phrasevoice.ui.i18n.localizedProviderHealthDescription
import com.phrasevoice.ui.i18n.localizedProviderHealthLabel
import com.phrasevoice.ui.i18n.t
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs

data class DisplayCardUiState(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val qrContent: String,
)

data class CommunicationDisplayUiState(
    val textScale: Float = 1.0f,
    val textTone: String = "mint",
)

@Composable
private fun VoiceWaveIndicator(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
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
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scale",
            )
            Canvas(
                modifier = Modifier.size(width = 3.dp, height = 14.dp),
            ) {
                val barHeight = size.height * scale
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, (size.height - barHeight) / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width / 2),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunicateScreen(
    state: HomeUiState,
    displayCards: List<DisplayCardUiState>,
    display: CommunicationDisplayUiState,
    audioClipsState: AudioClipsUiState,
    phraseActionsEditable: Boolean = true,
    showReadingControls: Boolean = true,
    onTextChange: (String) -> Unit,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseEdit: (Phrase) -> Unit,
    onQuickPhraseFavoriteToggle: (Phrase) -> Unit,
    onQuickPhraseGroupSelected: (String?) -> Unit,
    onAudioClipImport: (Uri) -> Unit,
    onAudioClipClick: (AudioClip) -> Unit,
    onAudioClipDelete: (String) -> Unit,
    onProviderSelected: (String) -> Unit = {},
    onVoiceSelected: (String?) -> Unit = {},
    onVoiceStyleSelected: (String?) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onReadingPresetSelected: (ReadingPreset) -> Unit = {},
    onTextOptimizationSelected: (TextOptimizationAction) -> Unit = {},
    onMimoSmartTextOptimizationChange: (Boolean) -> Unit = {},
    onSpeak: () -> Unit,
    onPreviewVoice: () -> Unit = {},
    onStop: () -> Unit,
    onReplay: () -> Unit,
    onSaveAudio: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selectedProvider = state.providers.firstOrNull { it.id == state.selectedProviderId }
    val androidTtsUnavailable = state.selectedProviderId == ProviderConfigRepository.ANDROID_SYSTEM && !state.androidTtsReady
    val selectedProviderUnavailable = selectedProvider?.enabled == false
    val canRunTts = state.status != HomeStatus.Loading &&
        state.status != HomeStatus.Saving &&
        !androidTtsUnavailable &&
        selectedProvider?.enabled == true
    val hasText = state.text.isNotBlank()
    val shareTitle = t("分享音频", "Share Audio")
    var selectedDisplayCard by remember { mutableStateOf<DisplayCardUiState?>(null) }
    var fullScreenQrCard by remember { mutableStateOf<DisplayCardUiState?>(null) }
    var showLargeText by remember { mutableStateOf(false) }
    var phraseAction by remember { mutableStateOf<Phrase?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    var showTuningSheet by remember { mutableStateOf(false) }
    val audioImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) onAudioClipImport(uri)
    }

    selectedDisplayCard?.let { card ->
        DisplayCardDialog(
            card = card,
            onShowQrFullScreen = { fullScreenQrCard = card },
            onDismiss = { selectedDisplayCard = null },
        )
    }
    fullScreenQrCard?.let { card ->
        FullScreenQrDialog(
            card = card,
            onDismiss = { fullScreenQrCard = null },
        )
    }
    if (showLargeText) {
        LargeTextDisplayDialog(
            text = state.text,
            display = display,
            isPlaying = state.status == HomeStatus.Playing,
            canSpeak = canRunTts && hasText,
            onSpeak = onSpeak,
            onStop = onStop,
            onReplay = onReplay,
            onDismiss = { showLargeText = false },
        )
    }
    phraseAction?.let { phrase ->
        QuickPhraseActionDialog(
            phrase = phrase,
            canEdit = phraseActionsEditable,
            onDismiss = { phraseAction = null },
            onEdit = onQuickPhraseEdit,
            onToggleFavorite = onQuickPhraseFavoriteToggle,
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 820.dp
        if (wideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CommunicateHeader(
                        state = state,
                        providerName = selectedProvider?.name,
                        displayCardCount = displayCards.size,
                        onDisplayCardsClick = { selectedDisplayCard = displayCards.firstOrNull() },
                    )
                    TalkingTextField(
                        text = state.text,
                        onTextChange = onTextChange,
                        expanded = true,
                        display = display,
                        modifier = Modifier.weight(1f),
                    )
                    CommunicateActionRow(
                        status = state.status,
                        canRunTts = canRunTts,
                        hasText = hasText,
                        onSpeak = onSpeak,
                        onStop = onStop,
                        onReplay = onReplay,
                        onShowLargeText = { showLargeText = true },
                        onTuneClick = { showTuningSheet = true },
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
                    StudioTabsPanel(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        state = state,
                        displayCards = displayCards,
                        audioClipsState = audioClipsState,
                        compactCards = true,
                        onQuickPhraseClick = onQuickPhraseClick,
                        onQuickPhraseLongClick = { phraseAction = it },
                        onQuickPhraseGroupSelected = onQuickPhraseGroupSelected,
                        onDisplayCardClick = { selectedDisplayCard = it },
                        onImportAudioClick = { audioImportLauncher.launch(arrayOf("audio/*")) },
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
                    displayCardCount = displayCards.size,
                    onDisplayCardsClick = { selectedDisplayCard = displayCards.firstOrNull() },
                )
                TalkingTextField(
                    text = state.text,
                    onTextChange = onTextChange,
                    expanded = false,
                    display = display,
                )
                CommunicateActionRow(
                    status = state.status,
                    canRunTts = canRunTts,
                    hasText = hasText,
                    onSpeak = onSpeak,
                    onStop = onStop,
                    onReplay = onReplay,
                    onShowLargeText = { showLargeText = true },
                    onTuneClick = { showTuningSheet = true },
                )
                CommunicateStatusMessage(
                    state = state,
                    selectedProvider = selectedProvider,
                    androidTtsUnavailable = androidTtsUnavailable,
                    selectedProviderUnavailable = selectedProviderUnavailable,
                )
                StudioTabsPanel(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    state = state,
                    displayCards = displayCards,
                    audioClipsState = audioClipsState,
                    compactCards = false,
                    onQuickPhraseClick = onQuickPhraseClick,
                    onQuickPhraseLongClick = { phraseAction = it },
                    onQuickPhraseGroupSelected = onQuickPhraseGroupSelected,
                    onDisplayCardClick = { selectedDisplayCard = it },
                    onImportAudioClick = { audioImportLauncher.launch(arrayOf("audio/*")) },
                    onAudioClipClick = onAudioClipClick,
                    onAudioClipDelete = onAudioClipDelete,
                )
            }
        }
    }

    if (showTuningSheet && showReadingControls) {
        ModalBottomSheet(
            onDismissRequest = { showTuningSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (showReadingControls) {
                    TextToolsPanel(
                        state = state,
                        enabled = hasText,
                        onTextOptimizationSelected = onTextOptimizationSelected,
                        onMimoSmartTextOptimizationChange = onMimoSmartTextOptimizationChange,
                    )
                    ReadingControlsPanel(
                        state = state,
                        canRunTts = canRunTts,
                        hasText = hasText,
                        context = context,
                        shareTitle = shareTitle,
                        onProviderSelected = onProviderSelected,
                        onVoiceSelected = onVoiceSelected,
                        onVoiceStyleSelected = onVoiceStyleSelected,
                        onSpeedChange = onSpeedChange,
                        onPitchChange = onPitchChange,
                        onVolumeChange = onVolumeChange,
                        onReadingPresetSelected = onReadingPresetSelected,
                        onPreviewVoice = onPreviewVoice,
                        onSaveAudio = onSaveAudio,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunicateHeader(
    state: HomeUiState,
    providerName: String?,
    displayCardCount: Int,
    onDisplayCardsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = t("工作台", "Studio"),
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
                onClick = onDisplayCardsClick,
                enabled = displayCardCount > 0,
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
                Text(t("卡片", "Cards"), fontWeight = FontWeight.SemiBold)
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
private fun DisplayCardDialog(
    card: DisplayCardUiState,
    onShowQrFullScreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = card.title.ifBlank { t("展示卡片", "Display Card") }
    val body = card.body.ifBlank { card.qrContent }
    val qrContent = displayCardQrContent(card)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text(t("收起", "Close"))
            }
        },
        dismissButton = if (qrContent.isNotBlank()) {
            {
                TextButton(onClick = onShowQrFullScreen, shape = RoundedCornerShape(14.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(t("全屏二维码", "Full QR"))
                }
            }
        } else {
            null
        },
        title = {
            Text(
                text = title,
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
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (body.isNotBlank()) {
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (qrContent.isNotBlank()) {
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
            }
        },
    )
}

@Composable
private fun FullScreenQrDialog(
    card: DisplayCardUiState,
    onDismiss: () -> Unit,
) {
    val qrContent = displayCardQrContent(card)
    if (qrContent.isBlank()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = card.title.ifBlank { t("二维码", "QR") },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Text(t("关闭", "Close"))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        QrCodeImage(
                            content = qrContent,
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                        )
                    }
                }

                card.body.takeIf { it.isNotBlank() }?.let { body ->
                    Text(
                        text = body,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun displayCardQrContent(card: DisplayCardUiState): String =
    card.qrContent.ifBlank {
        if (card.type == DisplayCard.TYPE_QR) card.body else ""
    }

@Composable
private fun TalkingTextField(
    text: String,
    onTextChange: (String) -> Unit,
    expanded: Boolean,
    display: CommunicationDisplayUiState,
    modifier: Modifier = Modifier,
) {
    val textScale = display.textScale.coerceIn(0.85f, 1.35f)
    val toneColors = communicationTextToneColors(display.textTone)
    val baseTextStyle = MaterialTheme.typography.headlineMedium
    val textStyle = baseTextStyle.copy(
        fontSize = (baseTextStyle.fontSize.value * textScale).sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
    )

    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        placeholder = {
            Text(
                text = t("点按短语，或直接输入要说的话", "Tap a phrase or type what you want to say"),
                color = toneColors.contentColor.copy(alpha = 0.58f),
            )
        },
        minLines = if (expanded) 9 else 5,
        maxLines = if (expanded) 16 else 9,
        shape = RoundedCornerShape(22.dp),
        textStyle = textStyle,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = toneColors.containerColor,
            unfocusedContainerColor = toneColors.containerColor,
            focusedTextColor = toneColors.contentColor,
            unfocusedTextColor = toneColors.contentColor,
            cursorColor = toneColors.contentColor,
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
    onShowLargeText: () -> Unit,
    onTuneClick: () -> Unit,
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

        OutlinedButton(
            onClick = onShowLargeText,
            enabled = hasText,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .weight(0.82f)
                .height(54.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Fullscreen,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(t("全屏", "Full"), fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onTuneClick,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(height = 54.dp, width = 54.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = t("语音设置", "Voice Settings"),
                tint = MaterialTheme.colorScheme.primary,
            )
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
        !state.noticeMessage.isNullOrBlank() -> state.noticeMessage
        else -> null
    }

    if (message != null) {
        val isNotice = !state.noticeMessage.isNullOrBlank() &&
            state.status != HomeStatus.Error &&
            !androidTtsUnavailable &&
            !selectedProviderUnavailable
        Surface(
            color = if (isNotice) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            contentColor = if (isNotice) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (isNotice) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
                },
            ),
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
private fun TextToolsPanel(
    state: HomeUiState,
    enabled: Boolean,
    onTextOptimizationSelected: (TextOptimizationAction) -> Unit,
    onMimoSmartTextOptimizationChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = t("文本工具", "Text Tools"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                state.textOptimizationFeedback?.let { feedback ->
                    Text(
                        text = textOptimizationFeedbackLabel(feedback),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

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

            if (state.mimoSmartTextOptimizationAvailable) {
                MimoSmartTextOptimizationRow(
                    checked = state.mimoSmartTextOptimizationEnabled,
                    enabled = state.status != HomeStatus.Loading && state.status != HomeStatus.Saving,
                    onCheckedChange = onMimoSmartTextOptimizationChange,
                )
            }
        }
    }
}

@Composable
private fun ReadingControlsPanel(
    state: HomeUiState,
    canRunTts: Boolean,
    hasText: Boolean,
    context: Context,
    shareTitle: String,
    onProviderSelected: (String) -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onVoiceStyleSelected: (String?) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onReadingPresetSelected: (ReadingPreset) -> Unit,
    onPreviewVoice: () -> Unit,
    onSaveAudio: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = t("朗读控制台", "Reading Studio"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = t("声音、语速、保存和分享都在这里", "Voice, pacing, saving, and sharing live here."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ReadingPresetRow(
                state = state,
                onReadingPresetSelected = onReadingPresetSelected,
            )

            VoiceEngineControls(
                state = state,
                canRunTts = canRunTts,
                onProviderSelected = onProviderSelected,
                onVoiceSelected = onVoiceSelected,
                onVoiceStyleSelected = onVoiceStyleSelected,
                onPreviewVoice = onPreviewVoice,
            )

            VoicePropertiesControls(
                state = state,
                onSpeedChange = onSpeedChange,
                onPitchChange = onPitchChange,
                onVolumeChange = onVolumeChange,
            )

            AudioOutputActions(
                state = state,
                canRunTts = canRunTts,
                hasText = hasText,
                context = context,
                shareTitle = shareTitle,
                onSaveAudio = onSaveAudio,
            )
        }
    }
}

@Composable
private fun VoiceEngineControls(
    state: HomeUiState,
    canRunTts: Boolean,
    onProviderSelected: (String) -> Unit,
    onVoiceSelected: (String?) -> Unit,
    onVoiceStyleSelected: (String?) -> Unit,
    onPreviewVoice: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = t("声音引擎", "Voice Engine"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
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
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(t("试听当前声音", "Preview Current Voice"), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VoicePropertiesControls(
    state: HomeUiState,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = t("声音属性", "Voice Properties"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
        )
        SliderItem(
            label = t("语速", "Speed"),
            value = state.speed,
            range = 0.5f..2.0f,
            startIcon = Icons.Outlined.DirectionsWalk,
            endIcon = Icons.Outlined.DirectionsRun,
            onChange = onSpeedChange,
        )
        SliderItem(
            label = t("音调", "Pitch"),
            value = state.pitch,
            range = 0.5f..2.0f,
            startIcon = Icons.Outlined.ArrowDownward,
            endIcon = Icons.Outlined.ArrowUpward,
            onChange = onPitchChange,
        )
        SliderItem(
            label = t("音量", "Volume"),
            value = state.volume,
            range = 0.0f..1.0f,
            startIcon = Icons.AutoMirrored.Outlined.VolumeMute,
            endIcon = Icons.AutoMirrored.Outlined.VolumeUp,
            onChange = onVolumeChange,
        )
    }
}

@Composable
private fun AudioOutputActions(
    state: HomeUiState,
    canRunTts: Boolean,
    hasText: Boolean,
    context: Context,
    shareTitle: String,
    onSaveAudio: () -> Unit,
) {
    val canSaveAudio = canRunTts && hasText && state.status != HomeStatus.Playing
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onSaveAudio,
            enabled = canSaveAudio,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = if (state.status == HomeStatus.Saving) t("保存中", "Saving") else t("保存为音频", "Save Audio"),
                fontWeight = FontWeight.SemiBold,
            )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(t("分享生成的音频", "Share Generated Audio"), fontWeight = FontWeight.SemiBold)
            }
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
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
            ),
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
            ),
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
            ),
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
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = startIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val thumbSizeAnim by animateDpAsState(
                targetValue = if (isPressed) 24.dp else 18.dp,
                label = "thumbSize",
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
                        modifier = Modifier.size(thumbSizeAnim),
                    )
                },
                track = { sliderState ->
                    val fraction = (sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    ) {
                        val trackWidth = size.width
                        val trackHeight = size.height
                        val centerY = trackHeight / 2f
                        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackHeight / 2f, trackHeight / 2f)

                        drawRoundRect(
                            color = inactiveColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(trackWidth, 8.dp.toPx()),
                            cornerRadius = cornerRadius,
                        )

                        drawRoundRect(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(primaryColor, tertiaryColor),
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, centerY - 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(trackWidth * fraction, 8.dp.toPx()),
                            cornerRadius = cornerRadius,
                        )
                    }
                },
            )

            Icon(
                imageVector = endIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickPhrasePanel(
    state: HomeUiState,
    compactCards: Boolean,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseLongClick: (Phrase) -> Unit,
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
                            onLongClick = { onQuickPhraseLongClick(phrase) },
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
                                    onLongClick = { onQuickPhraseLongClick(phrase) },
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
private fun DisplayCardsPanel(
    cards: List<DisplayCardUiState>,
    compactCards: Boolean,
    onDisplayCardClick: (DisplayCardUiState) -> Unit,
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
                text = t("展示卡片", "Display Cards"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
            )
            Text(
                text = t("${cards.size} 张", "${cards.size} cards"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (cards.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = t("可以在设置里添加常用展示卡片。", "Add display cards in Settings."),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                )
            }
        } else if (compactCards) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cards.forEach { card ->
                    DisplayCardButton(
                        card = card,
                        onClick = { onDisplayCardClick(card) },
                        modifier = Modifier.widthIn(min = 152.dp, max = 240.dp),
                    )
                }
            }
        } else {
            cards.chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowCards.forEach { card ->
                        DisplayCardButton(
                            card = card,
                            onClick = { onDisplayCardClick(card) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(2 - rowCards.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayCardButton(
    card: DisplayCardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qrContent = displayCardQrContent(card)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 70.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.title.ifBlank { t("展示卡片", "Display Card") },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                if (qrContent.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = card.body.ifBlank { qrContent },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPhraseButton(
    phrase: Phrase,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .heightIn(min = 68.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
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

@Composable
private fun StudioTabsPanel(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    state: HomeUiState,
    displayCards: List<DisplayCardUiState>,
    audioClipsState: AudioClipsUiState,
    compactCards: Boolean,
    onQuickPhraseClick: (Phrase) -> Unit,
    onQuickPhraseLongClick: (Phrase) -> Unit,
    onQuickPhraseGroupSelected: (String?) -> Unit,
    onDisplayCardClick: (DisplayCardUiState) -> Unit,
    onImportAudioClick: () -> Unit,
    onAudioClipClick: (AudioClip) -> Unit,
    onAudioClipDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        t("快捷短语", "Phrases"),
        t("展示卡片", "Display Cards"),
        t("快捷音频", "Audio Clips")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        when (selectedTab) {
            0 -> QuickPhrasePanel(
                state = state,
                compactCards = compactCards,
                onQuickPhraseClick = onQuickPhraseClick,
                onQuickPhraseLongClick = onQuickPhraseLongClick,
                onQuickPhraseGroupSelected = onQuickPhraseGroupSelected,
            )
            1 -> DisplayCardsPanel(
                cards = displayCards,
                compactCards = compactCards,
                onDisplayCardClick = onDisplayCardClick,
            )
            2 -> AudioClipsPanel(
                state = audioClipsState,
                compactCards = compactCards,
                onImportClick = onImportAudioClick,
                onAudioClipClick = onAudioClipClick,
                onAudioClipDelete = onAudioClipDelete,
            )
        }
    }
}
