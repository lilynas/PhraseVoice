package com.phrasevoice.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OfflineVoiceModel(
    val id: String,
    val name: String,
    val fileName: String,
    val engine: String = ENGINE_SHERPA_ONNX,
    val language: String = "",
    val voiceName: String = "",
    val status: String = STATUS_AVAILABLE,
    val sizeBytes: Long = 0L,
    val importedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val note: String? = null,
) {
    companion object {
        const val ENGINE_SHERPA_ONNX = "sherpa-onnx"

        const val STATUS_NOT_INSTALLED = "not_installed"
        const val STATUS_AVAILABLE = "available"
        const val STATUS_CORRUPT = "corrupt"
        const val STATUS_INCOMPATIBLE = "incompatible"
        const val STATUS_LOAD_FAILED = "load_failed"

        val STATUSES = setOf(
            STATUS_NOT_INSTALLED,
            STATUS_AVAILABLE,
            STATUS_CORRUPT,
            STATUS_INCOMPATIBLE,
            STATUS_LOAD_FAILED,
        )
    }
}
