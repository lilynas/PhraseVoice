package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DisplayCard(
    val id: String,
    val title: String,
    val body: String,
    val type: String = TYPE_TEXT,
    val qrContent: String = "",
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_CONTACT = "contact"
        const val TYPE_QR = "qr"

        val TYPES = setOf(TYPE_TEXT, TYPE_CONTACT, TYPE_QR)
    }
}
