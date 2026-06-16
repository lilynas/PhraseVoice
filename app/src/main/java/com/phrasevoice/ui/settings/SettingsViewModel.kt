package com.phrasevoice.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phrasevoice.data.local.AudioCacheInfo
import com.phrasevoice.data.local.AudioFileStore
import com.phrasevoice.data.local.OfflineVoiceModelStore
import com.phrasevoice.data.model.DisplayCard
import com.phrasevoice.data.model.OfflineVoiceModel
import com.phrasevoice.data.model.ProviderConfig
import com.phrasevoice.debug.AppLogger
import com.phrasevoice.debug.DebugLogEntry
import com.phrasevoice.data.model.UserSettings
import com.phrasevoice.data.repository.DisplayCardBackupCodec
import com.phrasevoice.data.repository.DisplayCardImporter
import com.phrasevoice.data.repository.ProviderConfigRepository
import com.phrasevoice.data.repository.ProviderHealthStatus
import com.phrasevoice.data.repository.SettingsRepository
import com.phrasevoice.data.repository.isReady
import com.phrasevoice.data.repository.providerHealthForConfig
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsProviderOption(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val status: ProviderHealthStatus = ProviderHealthStatus.Ready,
    val note: String? = null,
)

data class DisplayCardEditorState(
    val isOpen: Boolean = false,
    val editingCardId: String? = null,
    val titleDraft: String = "",
    val bodyDraft: String = "",
    val typeDraft: String = DisplayCard.TYPE_TEXT,
    val qrContentDraft: String = "",
)

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val providers: List<SettingsProviderOption> = emptyList(),
    val audioCacheInfo: AudioCacheInfo = AudioCacheInfo(fileCount = 0, totalBytes = 0),
    val cacheMessage: String? = null,
    val debugLogs: List<DebugLogEntry> = emptyList(),
    val displayCardEditor: DisplayCardEditorState = DisplayCardEditorState(),
    val displayCardFileActionMessage: String? = null,
    val offlineVoiceMessage: String? = null,
    val isLoaded: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val providerConfigRepository: ProviderConfigRepository,
    private val audioFileStore: AudioFileStore,
    private val offlineVoiceModelStore: OfflineVoiceModelStore,
) : ViewModel() {
    private val audioCacheInfo = MutableStateFlow(audioFileStore.cacheInfo())
    private val cacheMessage = MutableStateFlow<String?>(null)
    private val displayCardEditor = MutableStateFlow(DisplayCardEditorState())
    private val displayCardFileActionMessage = MutableStateFlow<String?>(null)
    private val offlineVoiceMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                AppLogger.configure(
                    enabled = settings.debugLoggingEnabled,
                    minLevel = settings.debugLogLevel,
                )
                if (!settings.displayCardsMigrated) {
                    migrateLegacyContactCard()
                }
            }
        }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.settings,
            providerConfigRepository.configs,
            audioCacheInfo,
            cacheMessage,
            AppLogger.entries,
        ) { settings, providerConfigs, cacheInfo, message, logs ->
            val hasOfflineModels = settings.offlineVoiceModels.any {
                it.status == OfflineVoiceModel.STATUS_AVAILABLE
            }
            SettingsUiState(
                settings = settings,
                providers = providerConfigs.map { toProviderOption(it, hasOfflineModels) },
                audioCacheInfo = cacheInfo,
                cacheMessage = message,
                debugLogs = logs,
                isLoaded = true,
            )
        },
        displayCardEditor,
        displayCardFileActionMessage,
        offlineVoiceMessage,
    ) { state, editor, cardMessage, offlineMessage ->
        state.copy(
            displayCardEditor = editor,
            displayCardFileActionMessage = cardMessage,
            offlineVoiceMessage = offlineMessage,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun updateDefaultProvider(providerId: String) {
        update { it.copy(defaultProviderId = providerId) }
    }

    fun updateDefaultSpeed(value: Float) {
        update { it.copy(defaultSpeed = value) }
    }

    fun updateDefaultPitch(value: Float) {
        update { it.copy(defaultPitch = value) }
    }

    fun updateDefaultVolume(value: Float) {
        update { it.copy(defaultVolume = value) }
    }

    fun updateAutoSaveHistory(value: Boolean) {
        update { it.copy(autoSaveHistory = value) }
    }

    fun updateKeepAudioCache(value: Boolean) {
        update { it.copy(keepAudioCache = value) }
    }

    fun updateCloudFallbackToSystemTts(value: Boolean) {
        update { it.copy(cloudFallbackToSystemTts = value) }
    }

    fun updateThemeMode(value: String) {
        update { it.copy(themeMode = value) }
    }

    fun updateLanguageMode(value: String) {
        update { it.copy(languageMode = value) }
    }

    fun updateCommunicationTextScale(value: Float) {
        update { it.copy(communicationTextScale = value.coerceIn(0.85f, 1.35f)) }
    }

    fun updateCommunicationTextTone(value: String) {
        val normalized = if (value in COMMUNICATION_TEXT_TONES) value else "mint"
        update { it.copy(communicationTextTone = normalized) }
    }

    fun updateLockScreenCommunicationEnabled(value: Boolean) {
        update { it.copy(lockScreenCommunicationEnabled = value) }
    }

    fun updateContactCardName(value: String) {
        update { it.copy(contactCardName = value) }
    }

    fun updateContactCardSubtitle(value: String) {
        update { it.copy(contactCardSubtitle = value) }
    }

    fun updateContactCardAccount(value: String) {
        update { it.copy(contactCardAccount = value) }
    }

    fun updateContactCardQrContent(value: String) {
        update { it.copy(contactCardQrContent = value) }
    }

    fun openAddDisplayCardDialog() {
        displayCardEditor.value = DisplayCardEditorState(isOpen = true)
    }

    fun openEditDisplayCardDialog(card: DisplayCard) {
        displayCardEditor.value = DisplayCardEditorState(
            isOpen = true,
            editingCardId = card.id,
            titleDraft = card.title,
            bodyDraft = card.body,
            typeDraft = normalizedDisplayCardType(card.type),
            qrContentDraft = card.qrContent,
        )
    }

    fun dismissDisplayCardDialog() {
        displayCardEditor.value = DisplayCardEditorState()
    }

    fun updateDisplayCardTitleDraft(value: String) {
        displayCardEditor.value = displayCardEditor.value.copy(titleDraft = value)
    }

    fun updateDisplayCardBodyDraft(value: String) {
        displayCardEditor.value = displayCardEditor.value.copy(bodyDraft = value)
    }

    fun updateDisplayCardTypeDraft(value: String) {
        displayCardEditor.value = displayCardEditor.value.copy(typeDraft = normalizedDisplayCardType(value))
    }

    fun updateDisplayCardQrContentDraft(value: String) {
        displayCardEditor.value = displayCardEditor.value.copy(qrContentDraft = value)
    }

    fun saveDisplayCardDialog() {
        val editor = displayCardEditor.value
        val body = editor.bodyDraft.trim()
        val qrContent = editor.qrContentDraft.trim()
        if (body.isBlank() && qrContent.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            settingsRepository.updateSettings { settings ->
                val cards = settings.displayCards.sortedBy { it.sortOrder }
                val editingId = editor.editingCardId
                val nextSortOrder = if (editingId == null) {
                    (cards.maxOfOrNull { it.sortOrder } ?: -1) + 1
                } else {
                    cards.firstOrNull { it.id == editingId }?.sortOrder
                        ?: ((cards.maxOfOrNull { it.sortOrder } ?: -1) + 1)
                }
                val previous = cards.firstOrNull { it.id == editingId }
                val card = DisplayCard(
                    id = editingId ?: UUID.randomUUID().toString(),
                    title = editor.titleDraft.trim().ifBlank {
                        body.take(24).ifBlank { qrContent.take(24).ifBlank { "Display Card" } }
                    },
                    body = body,
                    type = normalizedDisplayCardType(editor.typeDraft),
                    qrContent = qrContent,
                    sortOrder = nextSortOrder,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                )
                val nextCards = if (editingId == null) {
                    cards + card
                } else {
                    cards.map { if (it.id == editingId) card else it }
                }.normalizedDisplayCardOrder()

                settings.copy(
                    displayCards = nextCards,
                    displayCardsMigrated = true,
                )
            }
            dismissDisplayCardDialog()
        }
    }

    fun deleteDisplayCard(cardId: String) {
        viewModelScope.launch {
            update {
                it.copy(
                    displayCards = it.displayCards
                        .filterNot { card -> card.id == cardId }
                        .normalizedDisplayCardOrder(),
                    displayCardsMigrated = true,
                )
            }
        }
    }

    fun moveDisplayCard(cardId: String, direction: Int) {
        if (direction == 0) return
        viewModelScope.launch {
            update { settings ->
                val cards = settings.displayCards.sortedBy { it.sortOrder }.toMutableList()
                val fromIndex = cards.indexOfFirst { it.id == cardId }
                if (fromIndex < 0) {
                    settings
                } else {
                    val toIndex = (fromIndex + direction).coerceIn(0, cards.lastIndex)
                    if (fromIndex == toIndex) return@update settings
                    val moved = cards.removeAt(fromIndex)
                    cards.add(toIndex, moved)
                    settings.copy(
                        displayCards = cards.normalizedDisplayCardOrder(),
                        displayCardsMigrated = true,
                    )
                }
            }
        }
    }

    suspend fun buildDisplayCardsExportJson(): String =
        DisplayCardBackupCodec.encode(
            cards = uiState.value.settings.displayCards,
            exportedAt = System.currentTimeMillis(),
        )

    fun importDisplayCardsJson(json: String) {
        viewModelScope.launch {
            val result = runCatching {
                val backup = DisplayCardBackupCodec.decode(json)
                var importResult = DisplayCardImporter.merge(
                    currentCards = emptyList(),
                    backup = backup,
                    now = System.currentTimeMillis(),
                ).second

                settingsRepository.updateSettings { settings ->
                    val merged = DisplayCardImporter.merge(
                        currentCards = settings.displayCards,
                        backup = backup,
                        now = System.currentTimeMillis(),
                    )
                    importResult = merged.second
                    settings.copy(
                        displayCards = merged.first,
                        displayCardsMigrated = true,
                    )
                }
                importResult
            }

            displayCardFileActionMessage.value = result.fold(
                onSuccess = { importResult ->
                    when {
                        importResult.importedCards > 0 -> {
                            val skippedText = if (importResult.skippedCards > 0) {
                                "，跳过 ${importResult.skippedCards} 张重复/空卡片"
                            } else {
                                ""
                            }
                            "已导入 ${importResult.importedCards} 张展示卡片$skippedText。"
                        }

                        importResult.skippedCards > 0 -> "没有新的展示卡片可导入，已跳过重复/空卡片。"
                        else -> "导入文件里没有展示卡片。"
                    }
                },
                onFailure = { throwable ->
                    "导入失败：${throwable.message ?: "文件格式不正确"}"
                },
            )
        }
    }

    fun markDisplayCardsExportSuccess() {
        val cardCount = uiState.value.settings.displayCards.size
        displayCardFileActionMessage.value = "已导出 $cardCount 张展示卡片。"
    }

    fun showDisplayCardFileActionMessage(message: String) {
        displayCardFileActionMessage.value = message
    }

    fun importOfflineVoiceModel(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    offlineVoiceModelStore.importModel(uri)
                }
            }

            result
                .onSuccess { model ->
                    settingsRepository.updateSettings { settings ->
                        settings.copy(
                            offlineVoiceModels = (settings.offlineVoiceModels + model)
                                .distinctBy { it.id }
                                .sortedByDescending { it.importedAt },
                        )
                    }
                    offlineVoiceMessage.value = when (model.status) {
                        OfflineVoiceModel.STATUS_AVAILABLE -> "已导入离线语音模型，可在工作台选择 Offline sherpa-onnx。"
                        OfflineVoiceModel.STATUS_CORRUPT -> "模型包为空，已标记为损坏。"
                        OfflineVoiceModel.STATUS_INCOMPATIBLE -> "未找到可用 sherpa-onnx TTS 模型结构，已保留记录。"
                        else -> "已导入离线语音模型。"
                    }
                }
                .onFailure { throwable ->
                    offlineVoiceMessage.value = "导入失败：${throwable.message ?: "无法读取模型包"}"
                }
        }
    }

    fun deleteOfflineVoiceModel(modelId: String) {
        viewModelScope.launch {
            val model = uiState.value.settings.offlineVoiceModels.firstOrNull { it.id == modelId }
            if (model != null) {
                withContext(Dispatchers.IO) {
                    offlineVoiceModelStore.deleteModel(model.fileName)
                }
            }
            settingsRepository.updateSettings { settings ->
                settings.copy(
                    offlineVoiceModels = settings.offlineVoiceModels.filterNot { it.id == modelId },
                )
            }
            offlineVoiceMessage.value = "已删除离线语音模型。"
        }
    }

    fun completeOnboarding() {
        update { it.copy(hasCompletedOnboarding = true) }
    }

    fun updateDebugLoggingEnabled(value: Boolean) {
        update { it.copy(debugLoggingEnabled = value) }
    }

    fun updateDebugLogLevel(value: String) {
        update { it.copy(debugLogLevel = value) }
    }

    fun clearDebugLogs() {
        AppLogger.clear()
    }

    fun refreshAudioCacheInfo() {
        viewModelScope.launch {
            audioCacheInfo.value = withContext(Dispatchers.IO) {
                audioFileStore.cacheInfo()
            }
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch {
            val before = withContext(Dispatchers.IO) {
                val current = audioFileStore.cacheInfo()
                audioFileStore.clearCache()
                current
            }
            audioCacheInfo.value = withContext(Dispatchers.IO) {
                audioFileStore.cacheInfo()
            }
            cacheMessage.value = if (before.fileCount == 0) {
                "没有可清理的音频缓存"
            } else {
                "已清理 ${before.fileCount} 个音频文件"
            }
            AppLogger.i(TAG, "audio cache cleared files=${before.fileCount} bytes=${before.totalBytes}")
        }
    }

    private fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
        }
    }

    private suspend fun migrateLegacyContactCard() {
        val now = System.currentTimeMillis()
        settingsRepository.updateSettings { settings ->
            if (settings.displayCardsMigrated) {
                settings
            } else {
                settings.copy(
                    displayCards = settings.displayCards.ifEmpty {
                        listOf(settings.toLegacyDisplayCard(now))
                    }.normalizedDisplayCardOrder(),
                    displayCardsMigrated = true,
                )
            }
        }
    }

    private fun UserSettings.toLegacyDisplayCard(now: Long): DisplayCard {
        val name = contactCardName.ifBlank { "PhraseVoice" }
        val subtitle = contactCardSubtitle.ifBlank { "很高兴认识你" }
        val account = contactCardAccount.ifBlank { "@phrasevoice" }
        return DisplayCard(
            id = "legacy_contact_card",
            title = name,
            body = listOf(subtitle, account).filter { it.isNotBlank() }.joinToString("\n"),
            type = DisplayCard.TYPE_CONTACT,
            qrContent = contactCardQrContent.ifBlank { account.ifBlank { name } },
            sortOrder = 0,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun normalizedDisplayCardType(value: String): String =
        value.takeIf { it in DisplayCard.TYPES } ?: DisplayCard.TYPE_TEXT

    private fun List<DisplayCard>.normalizedDisplayCardOrder(): List<DisplayCard> =
        sortedBy { it.sortOrder }.mapIndexed { index, card -> card.copy(sortOrder = index) }

    private fun toProviderOption(config: ProviderConfig, hasOfflineModels: Boolean): SettingsProviderOption {
        val status = providerHealthForConfig(
            config = config,
            hasOfflineModels = hasOfflineModels,
        )
        return SettingsProviderOption(
            id = config.providerId,
            name = when (config.providerId) {
                ProviderConfigRepository.ANDROID_SYSTEM -> "Android System TTS"
                ProviderConfigRepository.OPENAI -> "OpenAI TTS"
                ProviderConfigRepository.EDGE_TTS_FORWARDER -> "Edge TTS Forwarder"
                ProviderConfigRepository.GEMINI -> "Gemini TTS"
                ProviderConfigRepository.MIMO -> "MiMo TTS"
                ProviderConfigRepository.OFFLINE_SHERPA -> "Offline sherpa-onnx"
                ProviderConfigRepository.CUSTOM_HTTP -> "Custom TTS API"
                else -> config.providerId
            },
            enabled = status.isReady,
            status = status,
            note = when (status) {
                ProviderHealthStatus.Ready -> null
                ProviderHealthStatus.Disabled -> "未配置"
                ProviderHealthStatus.MissingApiKey -> "缺少 API Key"
                ProviderHealthStatus.MissingBaseUrl -> "缺少 Base URL"
                ProviderHealthStatus.MissingOfflineModel -> "缺少离线语音包"
                ProviderHealthStatus.SystemUnavailable -> "系统 TTS 不可用"
            },
        )
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        private val COMMUNICATION_TEXT_TONES = setOf("mint", "sky", "warm", "lavender")
    }
}
