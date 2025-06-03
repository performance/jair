package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.PhaseAction
import java.util.UUID

interface PhaseActionRepository {
    suspend fun save(action: PhaseAction): PhaseAction
    suspend fun saveAll(actions: List<PhaseAction>): List<PhaseAction> // For saving multiple actions at once
    suspend fun findById(id: UUID): PhaseAction?
    suspend fun findByPhaseId(phaseId: UUID): List<PhaseAction> // Get all actions for a phase
}
