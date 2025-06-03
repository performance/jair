package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.ImplementationPhase
import java.util.UUID

interface ImplementationPhaseRepository {
    suspend fun save(phase: ImplementationPhase): ImplementationPhase
    suspend fun saveAll(phases: List<ImplementationPhase>): List<ImplementationPhase> // For saving multiple phases at once
    suspend fun findById(id: UUID): ImplementationPhase?
    suspend fun findByPlanId(planId: UUID): List<ImplementationPhase> // Get all phases for a plan
    suspend fun findByPlanIdAndPhaseNumber(planId: UUID, phaseNumber: Int): ImplementationPhase?
}
