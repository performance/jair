package com.hairhealth.platform.service.dto

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.hairhealth.platform.domain.Recommendation
import com.hairhealth.platform.domain.RecommendationStatus
import com.hairhealth.platform.domain.RecommendationType
import java.time.Instant
import java.util.UUID

// Jackson ObjectMapper for JSON string to Map and vice-versa
// Register JavaTimeModule for Instant serialization/deserialization if not already global
val recommendationObjectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

data class CreateRecommendationRequest(
    val userId: UUID,
    val consultationId: String?,
    val title: String,
    val description: String,
    val type: String, // e.g., "TREATMENT_ADJUSTMENT"
    val details: Map<String, Any> // JSON flexibility
)

data class UpdateRecommendationRequest(
    val title: String?,
    val description: String?,
    val type: String?, // e.g., "TREATMENT_ADJUSTMENT"
    val details: Map<String, Any>?,
    val status: String? // e.g., "ACTIVE", "SUPERSEDED"
)

data class RecommendationResponse(
    val id: UUID,
    val professionalId: UUID,
    val userId: UUID,
    val consultationId: String?,
    val title: String,
    val description: String,
    val type: String,
    val details: Map<String, Any>,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val userAction: String?,
    val userActionNotes: String?,
    val userActionAt: Instant?
)

data class RecommendationActionRequest(
    val recommendationId: UUID,
    val action: String, // "ACCEPTED", "ACCEPTED_WITH_MODIFICATIONS", "DECLINED"
    val modifications: Map<String, Any>? = null, // For "ACCEPTED_WITH_MODIFICATIONS"
    val declineReason: String? = null // For "DECLINED"
)

data class BatchRecommendationActionRequest(
    val actions: List<RecommendationActionRequest>
)


// Mapper functions

fun CreateRecommendationRequest.toDomain(professionalId: UUID, createdAt: Instant = Instant.now(), updatedAt: Instant = Instant.now()): Recommendation {
    val recommendationType = try {
        RecommendationType.valueOf(this.type.uppercase())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid recommendation type: ${this.type}")
    }
    return Recommendation(
        professionalId = professionalId,
        userId = this.userId,
        consultationId = this.consultationId,
        title = this.title,
        description = this.description,
        type = recommendationType,
        details = recommendationObjectMapper.writeValueAsString(this.details), // Map to JSON String
        // status is defaulted in domain
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Recommendation.toResponse(): RecommendationResponse {
    val detailsMap: Map<String, Any> = try {
        if (this.details.isNotBlank()) {
            recommendationObjectMapper.readValue(this.details, object : TypeReference<Map<String, Any>>() {})
        } else {
            emptyMap()
        }
    } catch (e: Exception) {
        // Log error or handle gracefully if details are not valid JSON
        // For now, returning an error field or empty map
        mapOf("error" to "Invalid JSON format in details field")
    }
    return RecommendationResponse(
        id = this.id,
        professionalId = this.professionalId,
        userId = this.userId,
        consultationId = this.consultationId,
        title = this.title,
        description = this.description,
        type = this.type.name,
        details = detailsMap, // JSON String to Map
        status = this.status.name,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        userAction = this.userAction?.name,
        userActionNotes = this.userActionNotes,
        userActionAt = this.userActionAt
    )
}

// Note: For UpdateRecommendationRequest, the conversion to domain will happen in the service layer,
// as it needs to fetch the existing domain object first.
