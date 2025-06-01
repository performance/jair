package com.hairhealth.platform.service.dto

import com.hairhealth.platform.domain.Intervention
import com.hairhealth.platform.domain.InterventionApplication
import com.hairhealth.platform.domain.InterventionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// --- Intervention DTOs ---

data class CreateInterventionRequest(
    val type: String, // "TOPICAL", "ORAL", "OTHER_TREATMENT"
    val productName: String,
    val dosageAmount: String?,
    val frequency: String,
    val applicationTime: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val provider: String?,
    val notes: String?,
    val sourceRecommendationId: UUID?
)

data class InterventionResponse(
    val id: UUID,
    val userId: UUID,
    val type: String,
    val productName: String,
    val dosageAmount: String?,
    val frequency: String,
    val applicationTime: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val provider: String?,
    val notes: String?,
    val sourceRecommendationId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant
)

// --- Intervention Application DTOs ---

data class LogApplicationRequest(
    val timestamp: Instant?, // Defaults to Instant.now() if null, handled in service
    val notes: String?
)

data class InterventionApplicationResponse(
    val id: UUID,
    val interventionId: UUID,
    val userId: UUID,
    val timestamp: Instant,
    val notes: String?,
    val createdAt: Instant
)

// --- Mapper Functions ---

fun CreateInterventionRequest.toDomain(userId: UUID): Intervention {
    val interventionTypeEnum = try {
        InterventionType.valueOf(this.type.uppercase())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid intervention type: ${this.type}. Valid types are: ${InterventionType.values().joinToString { it.name }}")
    }
    // id, isActive, createdAt, updatedAt have defaults in domain class
    return Intervention(
        userId = userId,
        type = interventionTypeEnum,
        productName = this.productName,
        dosageAmount = this.dosageAmount,
        frequency = this.frequency,
        applicationTime = this.applicationTime,
        startDate = this.startDate,
        endDate = this.endDate,
        provider = this.provider,
        notes = this.notes,
        sourceRecommendationId = this.sourceRecommendationId
        // isActive defaults to true
        // id, createdAt, updatedAt default in domain data class
    )
}

fun Intervention.toResponse(): InterventionResponse {
    return InterventionResponse(
        id = this.id,
        userId = this.userId,
        type = this.type.name,
        productName = this.productName,
        dosageAmount = this.dosageAmount,
        frequency = this.frequency,
        applicationTime = this.applicationTime,
        startDate = this.startDate,
        endDate = this.endDate,
        isActive = this.isActive,
        provider = this.provider,
        notes = this.notes,
        sourceRecommendationId = this.sourceRecommendationId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun LogApplicationRequest.toDomain(interventionId: UUID, userId: UUID, currentTimestamp: Instant): InterventionApplication {
    // id, createdAt have defaults in domain class
    return InterventionApplication(
        interventionId = interventionId,
        userId = userId, // Denormalized for easier querying / direct association
        timestamp = this.timestamp ?: currentTimestamp, // Use provided or current time
        notes = this.notes
        // id, createdAt default in domain data class
    )
}

fun InterventionApplication.toResponse(): InterventionApplicationResponse {
    return InterventionApplicationResponse(
        id = this.id,
        interventionId = this.interventionId,
        userId = this.userId,
        timestamp = this.timestamp,
        notes = this.notes,
        createdAt = this.createdAt
    )
}
