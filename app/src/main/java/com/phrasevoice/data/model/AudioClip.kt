package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AudioClip(
    val id: String,
    val title: String,
    val fileName: String,
    val mimeType: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastPlayedAt: Long? = null,
)
