package com.phrasevoice.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phrasevoice.data.model.TtsHistory
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSpeak: (TtsHistory) -> Unit,
    onSaveAsPhrase: (TtsHistory) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row {
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear, enabled = state.items.isNotEmpty()) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                Text("清空")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = { it.id }) { item ->
                HistoryRow(
                    item = item,
                    onSpeak = { onSpeak(item) },
                    onSaveAsPhrase = { onSaveAsPhrase(item) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: TtsHistory,
    onSpeak: () -> Unit,
    onSaveAsPhrase: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = DateFormat.getDateTimeInstance().format(Date(item.createdAt)),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSpeak) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Text("重播")
                }
                TextButton(onClick = onSaveAsPhrase) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null)
                    Text("转常用语")
                }
            }
        }
    }
}
