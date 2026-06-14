package com.phrasevoice.ui.library

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.ui.i18n.localizedPhraseLibraryStatusMessage
import com.phrasevoice.ui.i18n.localizedPhraseGroupName
import com.phrasevoice.ui.i18n.localizedPhraseTitle
import com.phrasevoice.ui.i18n.t
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseLibraryScreen(
    state: PhraseLibraryUiState,
    onQueryChange: (String) -> Unit,
    onGroupSelected: (String?) -> Unit,
    onAddPhrase: () -> Unit,
    onEditPhrase: (Phrase) -> Unit,
    onDeletePhrase: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onTitleDraftChange: (String) -> Unit,
    onTextDraftChange: (String) -> Unit,
    onFavoriteDraftChange: (Boolean) -> Unit,
    onDismissDialog: () -> Unit,
    onSaveDialog: () -> Unit,
    onSpeakPhrase: (Phrase) -> Unit,
    onBuildExportJson: suspend () -> String,
    onImportJson: (String) -> Unit,
    onExportCompleted: () -> Unit,
    onFileActionMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val exportFailedPrefix = t("导出失败：", "Export failed: ")
    val importFailedPrefix = t("导入失败：", "Import failed: ")
    val cannotWriteFile = t("无法写入文件", "Unable to write file")
    val cannotReadFile = t("无法读取文件", "Unable to read file")
    val cannotCreateFile = t("无法生成文件", "Unable to create file")

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult

        scope.launch {
            runCatching { context.writeTextToUri(uri, json) }
                .onSuccess { onExportCompleted() }
                .onFailure { throwable ->
                    onFileActionMessage("$exportFailedPrefix${throwable.message ?: cannotWriteFile}")
                }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        scope.launch {
            runCatching { context.readTextFromUri(uri) }
                .onSuccess(onImportJson)
                .onFailure { throwable ->
                    onFileActionMessage("$importFailedPrefix${throwable.message ?: cannotReadFile}")
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t("常用语库", "Phrase Library"),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddPhrase,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(t("新增", "Add"), fontWeight = FontWeight.Bold)
                }

                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = t("更多选项", "More Options")
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(t("导入 JSON", "Import JSON")) },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(t("导出 JSON", "Export JSON")) },
                            leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    runCatching { onBuildExportJson() }
                                        .onSuccess { json ->
                                            pendingExportJson = json
                                            exportLauncher.launch("phrasevoice-phrases.json")
                                        }
                                        .onFailure { throwable ->
                                            onFileActionMessage("$exportFailedPrefix${throwable.message ?: cannotCreateFile}")
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }

        state.fileActionMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = localizedPhraseLibraryStatusMessage(message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Capsule search bar
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text(t("搜索常用语...", "Search phrases..."), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = t("清除搜索", "Clear Search"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Filter group chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = state.selectedGroupId == null,
                    onClick = { onGroupSelected(null) },
                    label = { Text(t("全部", "All")) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(state.groups, key = { it.id }) { group ->
                FilterChip(
                    selected = state.selectedGroupId == group.id,
                    onClick = { onGroupSelected(group.id) },
                    label = { Text(localizedPhraseGroupName(group)) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Phrase Cards List with Swipe to Delete
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(state.filteredPhrases, key = { it.id }) { phrase ->
                PhraseRow(
                    phrase = phrase,
                    onSpeak = { onSpeakPhrase(phrase) },
                    onEdit = { onEditPhrase(phrase) },
                    onDelete = { onDeletePhrase(phrase.id) },
                    onToggleFavorite = { onToggleFavorite(phrase.id) },
                )
            }
        }
    }

    if (state.isDialogOpen) {
        PhraseEditorDialog(
            state = state,
            onTitleDraftChange = onTitleDraftChange,
            onTextDraftChange = onTextDraftChange,
            onFavoriteDraftChange = onFavoriteDraftChange,
            onDismiss = onDismissDialog,
            onSave = onSaveDialog,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseRow(
    phrase: Phrase,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = t("删除", "Delete"),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Card(
            onClick = onSpeak,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = t("播放", "Play"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedPhraseTitle(phrase),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phrase.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = t("编辑", "Edit"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (phrase.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = t("收藏", "Favorite"),
                        tint = if (phrase.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhraseEditorDialog(
    state: PhraseLibraryUiState,
    onTitleDraftChange: (String) -> Unit,
    onTextDraftChange: (String) -> Unit,
    onFavoriteDraftChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (state.editingPhraseId == null) t("新增常用语", "Add Phrase") else t("编辑常用语", "Edit Phrase"),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = state.titleDraft,
                    onValueChange = onTitleDraftChange,
                    label = { Text(t("标题", "Title")) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.textDraft,
                    onValueChange = onTextDraftChange,
                    label = { Text(t("内容", "Content")) },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = t("收藏常用语", "Favorite Phrase"),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.favoriteDraft,
                        onCheckedChange = onFavoriteDraftChange,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = state.textDraft.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(t("保存", "Save"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(t("取消", "Cancel"))
            }
        },
    )
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
