package com.hairhealth.platform.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class PhaseStatus { PENDING, ACTIVE, COMPLETED, SKIPPED }

data class ImplementationPhase(
    val id: UUID = UUID.randomUUID(),
    val planId: UUID,
    val phaseNumber: Int,
    var title: String,
    var description: String?,
    var startDate: LocalDate?,
    var endDate: LocalDate?,
    var status: PhaseStatus = PhaseStatus.PENDING,
    var goals: String?, // JSON array of strings
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now()
)
