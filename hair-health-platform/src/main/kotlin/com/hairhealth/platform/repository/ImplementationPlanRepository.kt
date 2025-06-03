package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.ImplementationPlan
import com.hairhealth.platform.domain.PlanStatus // Import the enum
import java.util.UUID

interface ImplementationPlanRepository {
    suspend fun save(plan: ImplementationPlan): ImplementationPlan
    suspend fun findById(id: UUID): ImplementationPlan?
    // Using PlanStatus enum directly for type safety if desired, or String if preferred for flexibility
    suspend fun findByUserIdAndStatus(userId: UUID, status: PlanStatus): List<ImplementationPlan>
    suspend fun findActiveByUserId(userId: UUID): ImplementationPlan? // Assuming one active plan per user
}
