package com.phrasevoice.data.repository

import com.phrasevoice.data.model.DisplayCard
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayCardBackupCodecTest {
    @Test
    fun encodeAndDecode_roundTripsBackupEnvelope() {
        val cards = listOf(testCard(id = "card-1", title = "联系我"))

        val json = DisplayCardBackupCodec.encode(cards = cards, exportedAt = 123L)
        val backup = DisplayCardBackupCodec.decode(json)

        assertEquals(DisplayCardBackup.CURRENT_SCHEMA_VERSION, backup.schemaVersion)
        assertEquals(DisplayCardBackup.APP_NAME, backup.app)
        assertEquals(123L, backup.exportedAt)
        assertEquals(cards, backup.cards)
    }

    @Test
    fun decode_supportsLegacyCardArray() {
        val json = """
            [
              {
                "id": "card-1",
                "title": "扫码",
                "body": "扫码加我",
                "type": "qr",
                "qrContent": "https://example.com",
                "sortOrder": 0,
                "createdAt": 1,
                "updatedAt": 2
              }
            ]
        """.trimIndent()

        val backup = DisplayCardBackupCodec.decode(json)

        assertEquals(0, backup.schemaVersion)
        assertEquals(1, backup.cards.size)
        assertEquals("扫码", backup.cards.first().title)
    }

    @Test
    fun merge_skipsDuplicateCardsAndBlankCards() {
        val currentCards = listOf(testCard(id = "card-1", title = "联系我"))
        val backup = DisplayCardBackup(
            exportedAt = 10L,
            cards = listOf(
                testCard(id = "card-1", title = "联系我"),
                testCard(id = "card-2", title = "扫码", type = DisplayCard.TYPE_QR, qrContent = "https://example.com"),
                testCard(id = "blank", title = "", body = "", qrContent = ""),
            ),
        )

        val (cards, result) = DisplayCardImporter.merge(
            currentCards = currentCards,
            backup = backup,
            now = 99L,
            idFactory = { "new-card" },
        )

        assertEquals(1, result.importedCards)
        assertEquals(2, result.skippedCards)
        assertEquals(2, cards.size)
        assertEquals("card-2", cards.last().id)
        assertEquals(1, cards.last().sortOrder)
    }

    private fun testCard(
        id: String,
        title: String,
        body: String = "很高兴认识你",
        type: String = DisplayCard.TYPE_CONTACT,
        qrContent: String = "PhraseVoice",
    ): DisplayCard =
        DisplayCard(
            id = id,
            title = title,
            body = body,
            type = type,
            qrContent = qrContent,
            sortOrder = 0,
            createdAt = 1L,
            updatedAt = 2L,
        )
}
