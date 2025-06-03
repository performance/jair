package com.hairhealth.platform.domain

import java.time.Instant
import java.util.UUID

enum class PlanStatus { DRAFT, ACTIVE, COMPLETED, CANCELLED, ADJUSTMENT_REQUESTED }

data class ImplementationPlan(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val sourceRecommendationIds: String?, // Consider List<UUID> and TypeConverter or JSON string
    var status: PlanStatus = PlanStatus.DRAFT,
    var userContextSnapshot: String?, // JSON
    var riskAssessmentResults: String?, // JSON
    var successProbability: Double?,
    var currentPhaseNumber: Int = 1,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)
