package com.phrasevoice.data.repository

import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.model.DisplayCard
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class DisplayCardBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val app: String = APP_NAME,
    val exportedAt: Long = 0,
    val cards: List<DisplayCard> = emptyList(),
) {
    companion object {
        const val APP_NAME = "PhraseVoice"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class DisplayCardImportResult(
    val importedCards: Int,
    val skippedCards: Int,
)

object DisplayCardBackupCodec {
    fun encode(
        cards: List<DisplayCard>,
        exportedAt: Long,
    ): String =
        PhraseVoiceJson.instance.encodeToString(
            DisplayCardBackup(
                exportedAt = exportedAt,
                cards = cards.sortedBy { it.sortOrder },
            ),
        )

    fun decode(json: String): DisplayCardBackup {
        val trimmed = json.trim()
        require(trimmed.isNotBlank()) { "Display card backup JSON is empty." }

        return runCatching {
            PhraseVoiceJson.instance.decodeFromString<DisplayCardBackup>(trimmed)
        }.getOrElse { envelopeError ->
            runCatching {
                DisplayCardBackup(
                    schemaVersion = 0,
                    exportedAt = 0,
                    cards = PhraseVoiceJson.instance.decodeFromString<List<DisplayCard>>(trimmed),
                )
            }.getOrElse {
                throw IllegalArgumentException("Unsupported display card backup JSON.", envelopeError)
            }
        }
    }
}

object DisplayCardImporter {
    fun merge(
        currentCards: List<DisplayCard>,
        backup: DisplayCardBackup,
        now: Long,
        idFactory: () -> String = { UUID.randomUUID().toString() },
    ): Pair<List<DisplayCard>, DisplayCardImportResult> {
        val cards = currentCards.sortedBy { it.sortOrder }.toMutableList()
        val usedIds = cards.mapTo(mutableSetOf()) { it.id }
        val knownCards = cards.mapTo(mutableSetOf()) { it.duplicateKey() }
        var nextSortOrder = (cards.maxOfOrNull { it.sortOrder } ?: -1) + 1
        var importedCards = 0
        var skippedCards = 0

        backup.cards.forEach { imported ->
            val type = imported.type.takeIf { it in DisplayCard.TYPES } ?: DisplayCard.TYPE_TEXT
            val body = imported.body.trim()
            val qrContent = imported.qrContent.trim()
            val title = imported.title.trim().ifBlank {
                when {
                    type == DisplayCard.TYPE_QR -> "QR Card"
                    body.isNotBlank() -> body.take(24)
                    qrContent.isNotBlank() -> qrContent.take(24)
                    else -> "Display Card"
                }
            }

            if (body.isBlank() && qrContent.isBlank()) {
                skippedCards += 1
                return@forEach
            }

            val candidate = imported.copy(
                id = imported.id.trim(),
                title = title,
                body = body,
                type = type,
                qrContent = qrContent,
            )

            if (!knownCards.add(candidate.duplicateKey())) {
                skippedCards += 1
                return@forEach
            }

            val cardId = candidate.id.takeIf { it.isNotBlank() && usedIds.add(it) }
                ?: idFactory().also { usedIds += it }
            cards += candidate.copy(
                id = cardId,
                sortOrder = nextSortOrder++,
                createdAt = candidate.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = candidate.updatedAt.takeIf { it > 0 } ?: now,
            )
            importedCards += 1
        }

        return cards.sortedBy { it.sortOrder } to DisplayCardImportResult(
            importedCards = importedCards,
            skippedCards = skippedCards,
        )
    }

    private fun DisplayCard.duplicateKey(): String =
        listOf(title.trim(), body.trim(), type.trim(), qrContent.trim()).joinToString(separator = "\u001F")
}
