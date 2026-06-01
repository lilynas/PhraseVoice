package com.phrasevoice.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.Phrase

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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "常用语",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onAddPhrase) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("新增")
            }
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("搜索") },
            modifier = Modifier.fillMaxWidth(),
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.selectedGroupId == null,
                    onClick = { onGroupSelected(null) },
                    label = { Text("全部") },
                )
            }
            items(state.groups, key = { it.id }) { group ->
                FilterChip(
                    selected = state.selectedGroupId == group.id,
                    onClick = { onGroupSelected(group.id) },
                    label = { Text(group.name) },
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
private fun PhraseRow(
    phrase: Phrase,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = phrase.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = phrase.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (phrase.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSpeak) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Text("朗读")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text("编辑")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text("删除")
                }
            }
        }
    }
}

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
        title = {
            Text(if (state.editingPhraseId == null) "新增常用语" else "编辑常用语")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.titleDraft,
                    onValueChange = onTitleDraftChange,
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.textDraft,
                    onValueChange = onTextDraftChange,
                    label = { Text("内容") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row {
                    Text("收藏", modifier = Modifier.weight(1f))
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
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
