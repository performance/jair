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
    // For a new entity, ID and timestamps are typically generated just before saving.
    // This function, if used, should reflect that these would be new values.
    return HairFallLog(
        id = UUID.randomUUID(), // Placeholder, will be overridden by service/repo if this instance is persisted
        userId = userId,
        date = this.date,
        count = this.count,
        category = logCategory,
        description = this.description,
        photoMetadataId = this.photoMetadataId,
        createdAt = Instant.now(), // Placeholder, will be overridden by service/repo
        updatedAt = Instant.now()  // Placeholder, will be overridden by service/repo
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

data class HairFallStatsResponse(
    val totalLogs: Long,
    val averageCount: Double?, // Nullable if no logs with counts
    val mostCommonCategory: String?, // Nullable if no logs, stores category.name
    val recentTrend: String // e.g., "increasing", "decreasing", "stable", "insufficient_data"
)
