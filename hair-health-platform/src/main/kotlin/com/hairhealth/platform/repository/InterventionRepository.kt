package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.Intervention
import com.hairhealth.platform.domain.InterventionApplication
import com.hairhealth.platform.domain.InterventionType
import java.time.Instant // Changed from LocalDate
import java.time.LocalDate
import java.util.UUID

interface InterventionRepository {
    suspend fun create(intervention: Intervention): Intervention
    suspend fun findById(id: UUID): Intervention?
    suspend fun findByIdAndUserId(id: UUID, userId: UUID): Intervention? // Added
    suspend fun findByUserId(userId: UUID, includeInactive: Boolean = false): List<Intervention>
    suspend fun findActiveByUserId(userId: UUID): List<Intervention>
    suspend fun update(intervention: Intervention): Intervention
    suspend fun deactivate(id: UUID): Boolean
    suspend fun delete(id: UUID): Boolean
}

interface InterventionApplicationRepository {
    suspend fun create(application: InterventionApplication): InterventionApplication
    suspend fun findById(id: UUID): InterventionApplication?
    suspend fun findByInterventionId(interventionId: UUID, limit: Int = 50, offset: Int = 0): List<InterventionApplication>
    suspend fun findByUserIdAndDateRange(
        userId: UUID,
        startDate: Instant, // Changed from LocalDate
        endDate: Instant   // Changed from LocalDate
    ): List<InterventionApplication>
    suspend fun countByInterventionId(interventionId: UUID): Long
    suspend fun delete(id: UUID): Boolean
}