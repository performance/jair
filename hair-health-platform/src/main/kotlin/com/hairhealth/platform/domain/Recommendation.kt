package com.hairhealth.platform.domain

import java.time.Instant
import java.util.UUID

enum class RecommendationType {
    TREATMENT_ADJUSTMENT,
    NEW_INTERVENTION,
    LIFESTYLE_CHANGE
}

enum class RecommendationStatus {
    ACTIVE,
    SUPERSEDED,
    DELETED
}

data class Recommendation(
    val id: UUID = UUID.randomUUID(),
    val professionalId: UUID, // ID of the professional (who is also a user)
    val userId: UUID,         // ID of the user/patient
    val consultationId: String?,
    val title: String,
    val description: String,
    val type: RecommendationType,
    val details: String,      // JSON stored as String
    val status: RecommendationStatus = RecommendationStatus.ACTIVE,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    // New fields for user actions
    val userAction: UserRecommendationAction? = null,
    val userActionNotes: String? = null,
    val userActionAt: Instant? = null
)

enum class UserRecommendationAction {
    PENDING_ACTION, // Default state before user acts or if user action is reset
    ACCEPTED,
    ACCEPTED_WITH_MODIFICATIONS,
    DECLINED
}
