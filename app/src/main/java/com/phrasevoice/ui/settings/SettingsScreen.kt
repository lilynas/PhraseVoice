package com.phrasevoice.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.automirrored.outlined.VolumeMute
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.BuildConfig
import com.phrasevoice.data.model.DisplayCard
import com.phrasevoice.data.model.OfflineVoiceModel
import com.phrasevoice.data.repository.OfflineVoiceDownloadCatalog
import com.phrasevoice.data.repository.OfflineVoiceDownloadItem
import com.phrasevoice.debug.DebugLogEntry
import com.phrasevoice.ui.i18n.AppLanguageMode
import com.phrasevoice.ui.i18n.localizedProviderHealthLabel
import com.phrasevoice.ui.i18n.localizedSettingsStatusMessage
import com.phrasevoice.ui.i18n.t
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
    onCloudFallbackToSystemTtsChange: (Boolean) -> Unit,
    onImportOfflineVoiceModel: (Uri) -> Unit,
    onDeleteOfflineVoiceModel: (String) -> Unit,
    onClearAudioCache: () -> Unit,
    onClearDebugLogs: () -> Unit,
    onDebugLoggingEnabledChange: (Boolean) -> Unit,
    onDebugLogLevelChange: (String) -> Unit,
    onRefreshAudioCache: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onLanguageModeChange: (String) -> Unit,
    onCommunicationTextScaleChange: (Float) -> Unit,
    onCommunicationTextToneChange: (String) -> Unit,
    onLockScreenCommunicationEnabledChange: (Boolean) -> Unit,
    onAddDisplayCard: () -> Unit,
    onEditDisplayCard: (DisplayCard) -> Unit,
    onDeleteDisplayCard: (String) -> Unit,
    onMoveDisplayCard: (String, Int) -> Unit,
    onDisplayCardTitleDraftChange: (String) -> Unit,
    onDisplayCardBodyDraftChange: (String) -> Unit,
    onDisplayCardTypeDraftChange: (String) -> Unit,
    onDisplayCardQrContentDraftChange: (String) -> Unit,
    onDismissDisplayCardDialog: () -> Unit,
    onSaveDisplayCardDialog: () -> Unit,
    onBuildDisplayCardsExportJson: suspend () -> String,
    onImportDisplayCardsJson: (String) -> Unit,
    onDisplayCardsExportCompleted: () -> Unit,
    onDisplayCardFileActionMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        onRefreshAudioCache()
    }

    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingDisplayCardsExportJson by remember { mutableStateOf<String?>(null) }
    val exportCardsFailedPrefix = t("导出失败：", "Export failed: ")
    val importCardsFailedPrefix = t("导入失败：", "Import failed: ")
    val cannotWriteCardsFile = t("无法写入文件", "Unable to write file")
    val cannotReadCardsFile = t("无法读取文件", "Unable to read file")
    val cannotCreateCardsFile = t("无法生成文件", "Unable to create file")

    val displayCardsExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        val json = pendingDisplayCardsExportJson
        pendingDisplayCardsExportJson = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult

        scope.launch {
            runCatching { context.writeTextToUri(uri, json) }
                .onSuccess { onDisplayCardsExportCompleted() }
                .onFailure { throwable ->
                    onDisplayCardFileActionMessage(
                        "$exportCardsFailedPrefix${throwable.message ?: cannotWriteCardsFile}",
                    )
                }
        }
    }

    val displayCardsImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            runCatching { context.readTextFromUri(uri) }
                .onSuccess(onImportDisplayCardsJson)
                .onFailure { throwable ->
                    onDisplayCardFileActionMessage(
                        "$importCardsFailedPrefix${throwable.message ?: cannotReadCardsFile}",
                    )
                }
        }
    }

    val offlineVoiceImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) onImportOfflineVoiceModel(uri)
    }

    val settings = state.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = t("设置", "Settings"),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            LanguageSwitcher(
                languageMode = settings.languageMode,
                onLanguageModeChange = onLanguageModeChange,
            )
        }

        SettingsCard {
            Text(
                text = t("主题设置", "Theme"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("system", t("默认", "System"), Icons.Outlined.BrightnessAuto),
                    Triple("light", t("白色", "Light"), Icons.Outlined.LightMode),
                    Triple("dark", t("黑色", "Dark"), Icons.Outlined.DarkMode)
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

        CommunicationDisplaySettingsCard(
            textScale = settings.communicationTextScale,
            textTone = settings.communicationTextTone,
            onTextScaleChange = onCommunicationTextScaleChange,
            onTextToneChange = onCommunicationTextToneChange,
        )

        SettingsCard {
            Text(
                text = t("锁屏交流", "Lock Screen Talk"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            SwitchSetting(
                label = t("锁屏交流模式", "Lock Screen Talk"),
                value = settings.lockScreenCommunicationEnabled,
                onChange = onLockScreenCommunicationEnabledChange,
            )
        }

        SettingsCard {
            Text(
                text = t("默认 Provider", "Default Provider"),
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
                text = t("发音属性默认值", "Default Voice Properties"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            SliderSetting(
                label = t("默认语速", "Default Speed"),
                value = settings.defaultSpeed,
                range = 0.5f..2.0f,
                startIcon = Icons.Outlined.DirectionsWalk,
                endIcon = Icons.Outlined.DirectionsRun,
                onChange = onSpeedChange
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSetting(
                label = t("默认音调", "Default Pitch"),
                value = settings.defaultPitch,
                range = 0.5f..2.0f,
                startIcon = Icons.Outlined.ArrowDownward,
                endIcon = Icons.Outlined.ArrowUpward,
                onChange = onPitchChange
            )
            Spacer(modifier = Modifier.height(10.dp))
            SliderSetting(
                label = t("默认音量", "Default Volume"),
                value = settings.defaultVolume,
                range = 0.0f..1.0f,
                startIcon = Icons.AutoMirrored.Outlined.VolumeMute,
                endIcon = Icons.AutoMirrored.Outlined.VolumeUp,
                onChange = onVolumeChange
            )
        }

        SettingsCard {
            Text(
                text = t("存储与历史", "Storage & History"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            SwitchSetting(t("自动保存历史", "Auto-save History"), settings.autoSaveHistory, onAutoSaveHistoryChange)
            SwitchSetting(t("保留音频缓存", "Keep Audio Cache"), settings.keepAudioCache, onKeepAudioCacheChange)
        }

        OfflineVoiceSettingsCard(
            fallbackEnabled = settings.cloudFallbackToSystemTts,
            models = settings.offlineVoiceModels,
            message = state.offlineVoiceMessage,
            onFallbackChange = onCloudFallbackToSystemTtsChange,
            onImportClick = {
                offlineVoiceImportLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/zip",
                        "application/x-tar",
                        "application/x-bzip2",
                        "application/x-bzip-compressed-tar",
                        "*/*",
                    ),
                )
            },
            onDelete = onDeleteOfflineVoiceModel,
        )

        DisplayCardsSettingsCard(
            cards = settings.displayCards.sortedBy { it.sortOrder },
            fileActionMessage = state.displayCardFileActionMessage,
            onAdd = onAddDisplayCard,
            onEdit = onEditDisplayCard,
            onDelete = onDeleteDisplayCard,
            onMove = onMoveDisplayCard,
            onImportClick = {
                displayCardsImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            onExportClick = {
                scope.launch {
                    runCatching { onBuildDisplayCardsExportJson() }
                        .onSuccess { json ->
                            pendingDisplayCardsExportJson = json
                            displayCardsExportLauncher.launch("phrasevoice-display-cards.json")
                        }
                        .onFailure { throwable ->
                            onDisplayCardFileActionMessage(
                                "$exportCardsFailedPrefix${throwable.message ?: cannotCreateCardsFile}",
                            )
                        }
                }
            },
        )

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
                        text = t("关于 PhraseVoice", "About PhraseVoice"),
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
            enabled = settings.debugLoggingEnabled,
            level = settings.debugLogLevel,
            logs = state.debugLogs,
            onClearDebugLogs = onClearDebugLogs,
            onEnabledChange = onDebugLoggingEnabledChange,
            onLevelChange = onDebugLogLevelChange,
        )
    }

    if (state.displayCardEditor.isOpen) {
        DisplayCardEditorDialog(
            editor = state.displayCardEditor,
            onTitleDraftChange = onDisplayCardTitleDraftChange,
            onBodyDraftChange = onDisplayCardBodyDraftChange,
            onTypeDraftChange = onDisplayCardTypeDraftChange,
            onQrContentDraftChange = onDisplayCardQrContentDraftChange,
            onDismiss = onDismissDisplayCardDialog,
            onSave = onSaveDisplayCardDialog,
        )
    }
}

private data class CommunicationToneOption(
    val value: String,
    val label: String,
    val swatchColor: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunicationDisplaySettingsCard(
    textScale: Float,
    textTone: String,
    onTextScaleChange: (Float) -> Unit,
    onTextToneChange: (String) -> Unit,
) {
    val toneOptions = listOf(
        CommunicationToneOption("mint", t("薄荷", "Mint"), Color(0xFFD2E8DD)),
        CommunicationToneOption("sky", t("天空", "Sky"), Color(0xFFDCEEFF)),
        CommunicationToneOption("warm", t("暖光", "Warm"), Color(0xFFF7E5C8)),
        CommunicationToneOption("lavender", t("薰衣草", "Lavender"), Color(0xFFE9E2FF)),
    )

    SettingsCard {
        Text(
            text = t("交流显示", "Communication Display"),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        SliderSetting(
            label = t("大字字号", "Large Text Size"),
            value = textScale,
            range = 0.85f..1.35f,
            startIcon = Icons.Outlined.ArrowDownward,
            endIcon = Icons.Outlined.ArrowUpward,
            onChange = onTextScaleChange,
            valueFormatter = { "${(it * 100).roundToInt()}%" },
        )
        Text(
            text = t("输入区颜色", "Input Tone"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            toneOptions.forEach { option ->
                FilterChip(
                    selected = textTone == option.value,
                    onClick = { onTextToneChange(option.value) },
                    label = { Text(option.label, maxLines = 1) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(option.swatchColor, RoundedCornerShape(4.dp))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                    ),
                                    RoundedCornerShape(4.dp),
                                ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun OfflineVoiceSettingsCard(
    fallbackEnabled: Boolean,
    models: List<OfflineVoiceModel>,
    message: String?,
    onFallbackChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val openUri: (String) -> Unit = { url ->
        runCatching { uriHandler.openUri(url) }
    }

    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = t("离线语音", "Offline Voice"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = onImportClick, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("导入", "Import"))
            }
        }

        SwitchSetting(
            label = t("云端失败时使用本机语音", "Use system voice if cloud fails"),
            value = fallbackEnabled,
            onChange = onFallbackChange,
        )

        message?.takeIf { it.isNotBlank() }?.let { currentMessage ->
            Text(
                text = localizedSettingsStatusMessage(currentMessage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OfflineVoiceDownloadList(
            items = OfflineVoiceDownloadCatalog.recommended,
            onDownload = { openUri(it.downloadUrl) },
            onOpenDocs = { openUri(it.docsUrl) },
            onOpenCatalog = { openUri(OfflineVoiceDownloadCatalog.OFFICIAL_CATALOG_URL) },
        )

        if (models.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = offlineVoiceStatusLabel(OfflineVoiceModel.STATUS_NOT_INSTALLED),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = t(
                            "导入 sherpa-onnx 模型包后会显示在这里。",
                            "Imported sherpa-onnx model packages will appear here.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                models.sortedByDescending { it.importedAt }.forEach { model ->
                    OfflineVoiceModelRow(model = model, onDelete = onDelete)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineVoiceDownloadList(
    items: List<OfflineVoiceDownloadItem>,
    onDownload: (OfflineVoiceDownloadItem) -> Unit,
    onOpenDocs: (OfflineVoiceDownloadItem) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = t("模型下载", "Model Downloads"),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onOpenCatalog) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("更多", "More"))
            }
        }
        Text(
            text = t(
                "不内置模型。请在浏览器下载模型包，下载完成后回到这里导入。",
                "Models are not bundled. Download a package in the browser, then come back here to import it.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                OfflineVoiceDownloadRow(
                    item = item,
                    onDownload = { onDownload(item) },
                    onOpenDocs = { onOpenDocs(item) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineVoiceDownloadRow(
    item: OfflineVoiceDownloadItem,
    onDownload: () -> Unit,
    onOpenDocs: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = t(item.descriptionZh, item.descriptionEn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDownload) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("下载", "Download"))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text(item.language) },
                shape = RoundedCornerShape(12.dp),
            )
            SuggestionChip(
                onClick = {},
                label = { Text(item.engine) },
                shape = RoundedCornerShape(12.dp),
            )
            SuggestionChip(
                onClick = {},
                label = { Text(t("${item.speakers} 声线", "${item.speakers} voices")) },
                shape = RoundedCornerShape(12.dp),
            )
            SuggestionChip(
                onClick = {},
                label = { Text(item.sampleRate) },
                shape = RoundedCornerShape(12.dp),
            )
            TextButton(onClick = onOpenDocs) {
                Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("说明", "Docs"))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflineVoiceModelRow(
    model: OfflineVoiceModel,
    onDelete: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = offlineVoiceModelDetail(model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onDelete(model.id) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(t("删除", "Delete"))
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(offlineVoiceStatusLabel(model.status)) },
                    shape = RoundedCornerShape(12.dp),
                )
                model.language.takeIf { it.isNotBlank() }?.let { language ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(language) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(formatBytes(model.sizeBytes)) },
                    shape = RoundedCornerShape(12.dp),
                )
            }
            model.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun offlineVoiceModelDetail(model: OfflineVoiceModel): String =
    listOf(
        model.engine,
        model.voiceName.takeIf { it.isNotBlank() },
    )
        .filterNotNull()
        .joinToString(" · ")

@Composable
private fun offlineVoiceStatusLabel(status: String): String =
    when (status) {
        OfflineVoiceModel.STATUS_AVAILABLE -> t("可用", "Available")
        OfflineVoiceModel.STATUS_CORRUPT -> t("损坏", "Corrupt")
        OfflineVoiceModel.STATUS_INCOMPATIBLE -> t("不兼容", "Incompatible")
        OfflineVoiceModel.STATUS_LOAD_FAILED -> t("加载失败", "Load Failed")
        else -> t("未安装", "Not Installed")
    }

@Composable
private fun DisplayCardsSettingsCard(
    cards: List<DisplayCard>,
    fileActionMessage: String?,
    onAdd: () -> Unit,
    onEdit: (DisplayCard) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = t("展示卡片", "Display Cards"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("新增", "Add"))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("导入", "Import"))
            }
            OutlinedButton(
                onClick = onExportClick,
                enabled = cards.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(t("导出", "Export"))
            }
        }

        fileActionMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (cards.isEmpty()) {
            Text(
                text = t("还没有展示卡片。", "No display cards yet."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            cards.forEachIndexed { index, card ->
                DisplayCardSettingsRow(
                    card = card,
                    canMoveUp = index > 0,
                    canMoveDown = index < cards.lastIndex,
                    onEdit = { onEdit(card) },
                    onDelete = { onDelete(card.id) },
                    onMoveUp = { onMove(card.id, -1) },
                    onMoveDown = { onMove(card.id, 1) },
                )
            }
        }
    }
}

@Composable
private fun DisplayCardSettingsRow(
    card: DisplayCard,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(16.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.title.ifBlank { t("未命名卡片", "Untitled Card") },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = displayCardTypeLabel(card.type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (card.qrContent.isNotBlank()) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = card.body.ifBlank { card.qrContent },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(Icons.Outlined.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            TextButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(t("编辑", "Edit"))
            }
            TextButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(t("删除", "Delete"))
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DisplayCardEditorDialog(
    editor: DisplayCardEditorState,
    onTitleDraftChange: (String) -> Unit,
    onBodyDraftChange: (String) -> Unit,
    onTypeDraftChange: (String) -> Unit,
    onQrContentDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val typeOptions = listOf(
        DisplayCard.TYPE_TEXT to t("文字", "Text"),
        DisplayCard.TYPE_CONTACT to t("联系方式", "Contact"),
        DisplayCard.TYPE_QR to t("二维码", "QR"),
    )
    val canSave = editor.bodyDraft.isNotBlank() || editor.qrContentDraft.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (editor.editingCardId == null) {
                    t("新增展示卡片", "Add Display Card")
                } else {
                    t("编辑展示卡片", "Edit Display Card")
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    typeOptions.forEach { (type, label) ->
                        FilterChip(
                            selected = editor.typeDraft == type,
                            onClick = { onTypeDraftChange(type) },
                            label = { Text(label) },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
                ContactCardTextField(
                    value = editor.titleDraft,
                    onValueChange = onTitleDraftChange,
                    label = t("标题", "Title"),
                    minLines = 1,
                    maxLines = 1,
                )
                ContactCardTextField(
                    value = editor.bodyDraft,
                    onValueChange = onBodyDraftChange,
                    label = if (editor.typeDraft == DisplayCard.TYPE_QR) {
                        t("说明文字（可选）", "Caption (optional)")
                    } else {
                        t("展示内容", "Display Content")
                    },
                    minLines = 3,
                    maxLines = 5,
                )
                ContactCardTextField(
                    value = editor.qrContentDraft,
                    onValueChange = onQrContentDraftChange,
                    label = t("二维码内容（可选）", "QR Content (optional)"),
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = canSave, shape = RoundedCornerShape(14.dp)) {
                Text(t("保存", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text(t("取消", "Cancel"))
            }
        },
    )
}

@Composable
private fun displayCardTypeLabel(type: String): String =
    when (type) {
        DisplayCard.TYPE_CONTACT -> t("联系方式", "Contact")
        DisplayCard.TYPE_QR -> t("二维码", "QR")
        else -> t("文字", "Text")
    }

@Composable
private fun ContactCardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    minLines: Int,
    maxLines: Int,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LanguageSwitcher(
    languageMode: String,
    onLanguageModeChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentMode = AppLanguageMode.fromValue(languageMode)
    val options = listOf(
        AppLanguageMode.System to t("跟随系统", "System"),
        AppLanguageMode.Chinese to "中文",
        AppLanguageMode.English to "English",
    )

    Box {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 32.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    RoundedCornerShape(8.dp),
                )
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = t("切换语言", "Switch Language"),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (mode, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (mode == currentMode) "$label  ✓" else label,
                            maxLines = 1,
                        )
                    },
                    onClick = {
                        expanded = false
                        onLanguageModeChange(mode.value)
                    },
                )
            }
        }
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
                        val note = provider.note?.let { localizedProviderHealthLabel(provider.status) }
                        Text(
                            text = listOfNotNull(provider.name, note).joinToString(" · "),
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
    valueFormatter: (Float) -> String = { "%.2f".format(it) },
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
                text = valueFormatter(value),
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
                text = t("音频缓存", "Audio Cache"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRefreshAudioCache) {
                Text(t("刷新", "Refresh"))
            }
            TextButton(
                onClick = onClearAudioCache,
                enabled = state.audioCacheInfo.fileCount > 0,
            ) {
                Text(t("清理", "Clear"))
            }
        }
        Text(
            text = t(
                "${state.audioCacheInfo.fileCount} 个文件 · ${formatBytes(state.audioCacheInfo.totalBytes)}",
                "${state.audioCacheInfo.fileCount} files · ${formatBytes(state.audioCacheInfo.totalBytes)}",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        state.cacheMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(text = localizedSettingsStatusMessage(message), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebugLogCard(
    enabled: Boolean,
    level: String,
    logs: List<DebugLogEntry>,
    onClearDebugLogs: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onLevelChange: (String) -> Unit,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val logLevelOptions = listOf(
        "VERBOSE" to t("Verbose 及以上", "Verbose and above"),
        "DEBUG" to t("Debug 及以上", "Debug and above"),
        "INFO" to t("Info 及以上", "Info and above"),
        "WARN" to t("Warning 及以上", "Warning and above"),
        "ERROR" to t("仅 Error", "Error only"),
    )
    var expanded by remember { mutableStateOf(false) }
    val selectedLevel = logLevelOptions.firstOrNull { it.first.equals(level, ignoreCase = true) }
        ?: logLevelOptions[2]
    val visibleLogs = if (enabled) {
        logs.filter { entry -> debugLogPriority(entry.level) >= debugLogPriority(selectedLevel.first) }
    } else {
        emptyList()
    }

    SettingsCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = t("调试日志", "Debug Logs"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onClearDebugLogs,
                enabled = logs.isNotEmpty(),
            ) {
                Text(t("清空", "Clear"))
            }
        }
        SwitchSetting(t("启用调试日志", "Enable Debug Logs"), enabled, onEnabledChange)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = !expanded },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                readOnly = true,
                enabled = enabled,
                value = selectedLevel.second,
                onValueChange = {},
                label = { Text(t("记录等级", "Log Level")) },
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
                logLevelOptions.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onLevelChange(value)
                        },
                    )
                }
            }
        }
        Text(
            text = t(
                "临时诊断用。可直接截图给我，日志不会显示 API Key。",
                "For temporary diagnostics. You can send a screenshot; API keys are not shown.",
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        if (!enabled) {
            Text(text = t("调试日志已关闭", "Debug logs are disabled"), style = MaterialTheme.typography.bodySmall)
        } else if (visibleLogs.isEmpty()) {
            Text(text = t("暂无日志", "No logs yet"), style = MaterialTheme.typography.bodySmall)
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    visibleLogs.takeLast(30).asReversed().forEach { entry ->
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

private fun debugLogPriority(level: String): Int =
    when (level.uppercase(Locale.US)) {
        "V", "VERBOSE" -> 0
        "D", "DEBUG" -> 10
        "I", "INFO" -> 20
        "W", "WARN", "WARNING" -> 30
        "E", "ERROR" -> 40
        else -> 20
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

private fun Context.readTextFromUri(uri: Uri): String =
    contentResolver.openInputStream(uri)?.use { input ->
        input.bufferedReader(Charsets.UTF_8).readText()
    } ?: error("无法打开文件")

private fun Context.writeTextToUri(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri, "wt")?.use { output ->
        output.write(text.toByteArray(Charsets.UTF_8))
    } ?: error("无法打开文件")
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(t("确定", "OK"))
            }
        },
        title = {
            Text(
                text = t("关于 PhraseVoice", "About PhraseVoice"),
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
                    text = "PhraseVoice v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = t(
                        "一键配置，开箱即用的极简语音合成器",
                        "A minimal text-to-speech app that is ready after one quick setup.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val uriHandler = LocalUriHandler.current

                AboutLinkItem(
                    label = t("作者", "Author"),
                    value = "shirone",
                    onClick = { runCatching { uriHandler.openUri("https://github.com/lilynas") } }
                )
                AboutLinkItem(
                    label = t("项目开源地址", "Source"),
                    value = "PhraseVoice",
                    onClick = { runCatching { uriHandler.openUri("https://github.com/lilynas/PhraseVoice") } }
                )
                AboutLinkItem(
                    label = t("问题反馈", "Feedback"),
                    value = t("提交 Issue", "Submit Issue"),
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
