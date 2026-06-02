package com.phrasevoice.data.repository

import com.phrasevoice.data.model.Phrase
import com.phrasevoice.data.model.PhraseGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class PhraseLibraryBackupCodecTest {
    @Test
    fun encodeAndDecode_roundTripsBackupEnvelope() {
        val groups = listOf(PhraseGroup(id = "default", name = "常用", sortOrder = 0))
        val phrases = listOf(testPhrase(id = "p1", title = "问候", text = "你好"))

        val json = PhraseLibraryBackupCodec.encode(
            groups = groups,
            phrases = phrases,
            exportedAt = 123L,
        )
        val backup = PhraseLibraryBackupCodec.decode(json)

        assertEquals(PhraseLibraryBackup.CURRENT_SCHEMA_VERSION, backup.schemaVersion)
        assertEquals(PhraseLibraryBackup.APP_NAME, backup.app)
        assertEquals(123L, backup.exportedAt)
        assertEquals(groups, backup.groups)
        assertEquals(phrases, backup.phrases)
    }

    @Test
    fun decode_supportsLegacyPhraseArray() {
        val json = """
            [
              {
                "id": "p1",
                "text": "谢谢",
                "title": "感谢",
                "groupId": "default",
                "sortOrder": 0,
                "createdAt": 1,
                "updatedAt": 2,
                "lastUsedAt": null,
                "isFavorite": true
              }
            ]
        """.trimIndent()

        val backup = PhraseLibraryBackupCodec.decode(json)

        assertEquals(0, backup.schemaVersion)
        assertEquals(1, backup.phrases.size)
        assertEquals("感谢", backup.phrases.first().title)
    }

    @Test
    fun merge_mapsGroupsAndSkipsDuplicatePhrases() {
        val currentGroups = listOf(PhraseGroup(id = "default", name = "常用", sortOrder = 0))
        val currentPhrases = listOf(testPhrase(id = "p1", title = "问候", text = "你好"))
        val backup = PhraseLibraryBackup(
            exportedAt = 10L,
            groups = listOf(PhraseGroup(id = "travel", name = "出行", sortOrder = 0)),
            phrases = listOf(
                testPhrase(id = "p1", title = "问候", text = "你好"),
                testPhrase(id = "p2", title = "到站", text = "我到了", groupId = "travel"),
            ),
        )
        val ids = listOf("new-phrase")
        var nextId = 0

        val merged = PhraseLibraryImporter.merge(
            currentGroups = currentGroups,
            currentPhrases = currentPhrases,
            backup = backup,
            now = 99L,
            idFactory = { ids[nextId++] },
        )

        assertEquals(1, merged.importResult.importedGroups)
        assertEquals(1, merged.importResult.importedPhrases)
        assertEquals(1, merged.importResult.skippedPhrases)
        assertEquals("出行", merged.groups.last().name)
        assertEquals("new-phrase", merged.phrases.last().id)
        assertEquals(merged.groups.last().id, merged.phrases.last().groupId)
    }

    private fun testPhrase(
        id: String,
        title: String,
        text: String,
        groupId: String = "default",
    ): Phrase =
        Phrase(
            id = id,
            title = title,
            text = text,
            groupId = groupId,
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 2L,
        )
}
