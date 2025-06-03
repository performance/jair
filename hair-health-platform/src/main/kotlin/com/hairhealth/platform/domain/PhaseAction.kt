package com.hairhealth.platform.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class ActionStatus { PENDING, IN_PROGRESS, COMPLETED, MISSED, NOT_APPLICABLE }

data class PhaseAction(
    val id: UUID = UUID.randomUUID(),
    val phaseId: UUID,
    var actionDescription: String,
    var actionType: String, // Consider an enum later if types become well-defined
    var actionDetails: String?, // JSON
    var isKeyAction: Boolean = false,
    var status: ActionStatus = ActionStatus.PENDING,
    var dueDate: LocalDate?,
    var completedAt: Instant?,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)
