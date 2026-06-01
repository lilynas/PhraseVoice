package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Phrase(
    val id: String,
    val text: String,
    val title: String,
    val groupId: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long? = null,
    val isFavorite: Boolean = false,
)
