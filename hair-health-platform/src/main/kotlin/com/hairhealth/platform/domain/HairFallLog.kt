package com.hairhealth.platform.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class HairFallLog(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val count: Int?,
    val category: HairFallCategory,
    val description: String?,
    val photoMetadataId: UUID?, // Links to encrypted photo
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class HairFallCategory {
    SHOWER, PILLOW, COMBING, BRUSHING, OTHER
}