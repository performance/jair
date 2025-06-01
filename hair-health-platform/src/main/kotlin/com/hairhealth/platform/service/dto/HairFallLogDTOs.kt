package com.hairhealth.platform.service.dto

import com.hairhealth.platform.domain.HairFallCategory
import com.hairhealth.platform.domain.HairFallLog
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateHairFallLogRequest(
    val date: LocalDate,
    val count: Int?,
    val category: String, // e.g., "SHOWER"
    val description: String?,
    val photoMetadataId: UUID?
)

data class HairFallLogResponse(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val count: Int?,
    val category: String,
    val description: String?,
    val photoMetadataId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant
)

// Mapper functions

fun CreateHairFallLogRequest.toDomain(userId: UUID): HairFallLog {
    val logCategory = try {
        HairFallCategory.valueOf(this.category.uppercase())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid hair fall category: ${this.category}. Valid categories are: ${HairFallCategory.values().joinToString { it.name }}")
    }
    // id, createdAt, updatedAt will be set with defaults in the domain or by the DB/repository
    return HairFallLog(
        userId = userId,
        date = this.date,
        count = this.count,
        category = logCategory,
        description = this.description,
        photoMetadataId = this.photoMetadataId
        // Assuming id, createdAt, updatedAt have defaults in domain or are set by repository/DB
    )
}

fun HairFallLog.toResponse(): HairFallLogResponse {
    return HairFallLogResponse(
        id = this.id,
        userId = this.userId,
        date = this.date,
        count = this.count,
        category = this.category.name,
        description = this.description,
        photoMetadataId = this.photoMetadataId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
