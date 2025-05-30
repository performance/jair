package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.HairFallLog
import java.time.LocalDate
import java.util.UUID

interface HairFallLogRepository {
    suspend fun create(hairFallLog: HairFallLog): HairFallLog
    suspend fun findById(id: UUID): HairFallLog?
    suspend fun findByUserId(userId: UUID, limit: Int = 50, offset: Int = 0): List<HairFallLog>
    suspend fun findByUserIdAndDateRange(
        userId: UUID, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<HairFallLog>
    suspend fun update(hairFallLog: HairFallLog): HairFallLog
    suspend fun delete(id: UUID): Boolean
    suspend fun countByUserId(userId: UUID): Long
}