package com.hairhealth.platform.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Intervention(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val type: InterventionType,
    val productName: String,
    val dosageAmount: String?,
    val frequency: String, // e.g., "Twice Daily", "Every Other Day"
    val applicationTime: String?, // e.g., "08:00, 20:00"
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean = true,
    val provider: String?, // Doctor name for OTHER_TREATMENT
    val notes: String?,
    val sourceRecommendationId: UUID?, // Links to professional recommendation
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class InterventionType {
    TOPICAL, ORAL, OTHER_TREATMENT
}

data class InterventionApplication(
    val id: UUID = UUID.randomUUID(),
    val interventionId: UUID,
    val userId: UUID,
    val timestamp: Instant,
    val notes: String?,
    val createdAt: Instant = Instant.now()
)