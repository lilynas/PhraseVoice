package com.phrasevoice.data.repository

import com.phrasevoice.data.local.PhraseVoiceJson
import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.model.PhraseGroup
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class PhraseLibraryBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val app: String = APP_NAME,
    val exportedAt: Long = 0,
    val groups: List<PhraseGroup> = emptyList(),
    val phrases: List<Phrase> = emptyList(),
) {
    companion object {
        const val APP_NAME = "PhraseVoice"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class PhraseImportResult(
    val importedGroups: Int,
    val importedPhrases: Int,
    val skippedPhrases: Int,
)

data class PhraseLibraryMergeResult(
    val groups: List<PhraseGroup>,
    val phrases: List<Phrase>,
    val importResult: PhraseImportResult,
)

object PhraseLibraryBackupCodec {
    fun encode(
        groups: List<PhraseGroup>,
        phrases: List<Phrase>,
        exportedAt: Long,
    ): String =
        PhraseVoiceJson.instance.encodeToString(
            PhraseLibraryBackup(
                exportedAt = exportedAt,
                groups = groups,
                phrases = phrases,
            ),
        )

    fun decode(json: String): PhraseLibraryBackup {
        val trimmed = json.trim()
        require(trimmed.isNotBlank()) { "Phrase backup JSON is empty." }

        return runCatching {
            PhraseVoiceJson.instance.decodeFromString<PhraseLibraryBackup>(trimmed)
        }.getOrElse { envelopeError ->
            runCatching {
                PhraseLibraryBackup(
                    schemaVersion = 0,
                    exportedAt = 0,
                    phrases = PhraseVoiceJson.instance.decodeFromString<List<Phrase>>(trimmed),
                )
            }.getOrElse {
                throw IllegalArgumentException("Unsupported phrase backup JSON.", envelopeError)
            }
        }
    }
}

object PhraseLibraryImporter {
    fun merge(
        currentGroups: List<PhraseGroup>,
        currentPhrases: List<Phrase>,
        backup: PhraseLibraryBackup,
        now: Long,
        idFactory: () -> String = { UUID.randomUUID().toString() },
    ): PhraseLibraryMergeResult {
        val groups = currentGroups.toMutableList()
        if (groups.none { it.id == PhraseRepository.DEFAULT_GROUP_ID }) {
            groups += PhraseGroup(
                id = PhraseRepository.DEFAULT_GROUP_ID,
                name = "常用",
                sortOrder = (groups.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            )
        }

        val groupIdMap = mutableMapOf<String, String>()
        val usedGroupIds = groups.mapTo(mutableSetOf()) { it.id }
        var nextGroupSortOrder = (groups.maxOfOrNull { it.sortOrder } ?: -1) + 1
        var importedGroups = 0

        backup.groups.forEach { imported ->
            val sourceId = imported.id.trim()
            val name = imported.name.trim().ifBlank { "导入" }
            val existingById = groups.firstOrNull { it.id == sourceId }
            val existingByName = groups.firstOrNull { it.name.trim() == name }

            val targetId = when {
                sourceId.isNotBlank() && existingById != null && existingById.name.trim() == name -> existingById.id
                existingByName != null -> existingByName.id
                else -> {
                    val newId = sourceId.takeIf { it.isNotBlank() && usedGroupIds.add(it) }
                        ?: idFactory().also { usedGroupIds += it }
                    groups += PhraseGroup(
                        id = newId,
                        name = name,
                        sortOrder = nextGroupSortOrder++,
                    )
                    importedGroups += 1
                    newId
                }
            }

            if (sourceId.isNotBlank()) {
                groupIdMap[sourceId] = targetId
            }
        }

        val phrases = currentPhrases.toMutableList()
        val usedPhraseIds = phrases.mapTo(mutableSetOf()) { it.id }
        val knownPhrases = phrases.mapTo(mutableSetOf()) { it.duplicateKey() }
        var nextPhraseSortOrder = (phrases.maxOfOrNull { it.sortOrder } ?: -1) + 1
        var importedPhrases = 0
        var skippedPhrases = 0

        backup.phrases.forEach { imported ->
            val text = imported.text.trim()
            if (text.isBlank()) {
                skippedPhrases += 1
                return@forEach
            }

            val targetGroupId = groupIdMap[imported.groupId]
                ?: imported.groupId.takeIf { usedGroupIds.contains(it) }
                ?: PhraseRepository.DEFAULT_GROUP_ID
            val title = imported.title.trim().ifBlank { text.take(24) }
            val candidate = imported.copy(
                id = imported.id.trim(),
                title = title,
                text = text,
                groupId = targetGroupId,
            )

            if (!knownPhrases.add(candidate.duplicateKey())) {
                skippedPhrases += 1
                return@forEach
            }

            val phraseId = candidate.id.takeIf { it.isNotBlank() && usedPhraseIds.add(it) }
                ?: idFactory().also { usedPhraseIds += it }
            phrases += candidate.copy(
                id = phraseId,
                sortOrder = nextPhraseSortOrder++,
                createdAt = candidate.createdAt.takeIf { it > 0 } ?: now,
                updatedAt = candidate.updatedAt.takeIf { it > 0 } ?: now,
            )
            importedPhrases += 1
        }

        return PhraseLibraryMergeResult(
            groups = groups.sortedBy { it.sortOrder },
            phrases = phrases.sortedWith(compareBy<Phrase> { it.sortOrder }.thenByDescending { it.updatedAt }),
            importResult = PhraseImportResult(
                importedGroups = importedGroups,
                importedPhrases = importedPhrases,
                skippedPhrases = skippedPhrases,
            ),
        )
    }

    private fun Phrase.duplicateKey(): String =
        listOf(groupId.trim(), title.trim(), text.trim()).joinToString(separator = "\u001F")
}
