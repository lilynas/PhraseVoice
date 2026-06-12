package com.phrasevoice.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.ui.i18n.localizedPhraseTitle
import com.phrasevoice.ui.i18n.t

@Composable
fun QuickPhraseActionDialog(
    phrase: Phrase,
    onDismiss: () -> Unit,
    onEdit: (Phrase) -> Unit,
    onToggleFavorite: (Phrase) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = localizedPhraseTitle(phrase),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Text(
                text = phrase.text,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onEdit(phrase)
                },
            ) {
                Text(t("编辑", "Edit"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onToggleFavorite(phrase)
                },
            ) {
                Text(if (phrase.isFavorite) t("取消收藏", "Unfavorite") else t("收藏", "Favorite"))
            }
        },
    )
}
