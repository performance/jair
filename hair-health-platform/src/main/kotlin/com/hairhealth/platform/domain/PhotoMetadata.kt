package com.hairhealth.platform.domain

import java.time.Instant
import java.util.UUID

data class PhotoMetadata(
    val id: UUID,
    val userId: UUID,
    val filename: String, // The encrypted filename
    val angle: PhotoAngle,
    val captureDate: Instant,
    val fileSize: Long?,
    val encryptionKeyInfo: String, // Client-managed key identifier/metadata
    val blobPath: String, // Path in cloud storage
    val uploadedAt: Instant,
    val isDeleted: Boolean = false
)

enum class PhotoAngle {
    VERTEX, HAIRLINE, TEMPLES, LEFT_SIDE, RIGHT_SIDE, BACK
}