package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PhraseGroup(
    val id: String,
    val name: String,
    val sortOrder: Int,
)
