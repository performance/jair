package com.hairhealth.platform.service

import com.hairhealth.platform.controller.HairFallStatsResponse
import com.hairhealth.platform.domain.HairFallCategory
import com.hairhealth.platform.domain.HairFallLog
import com.hairhealth.platform.repository.HairFallLogRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
class HairFallLogService(
    private val hairFallLogRepository: HairFallLogRepository
) {

    suspend fun createHairFallLog(
        userId: UUID,
        date: LocalDate,
        count: Int?,
        category: HairFallCategory,
        description: String?,
        photoMetadataId: UUID?
    ): HairFallLog {
        val hairFallLog = HairFallLog(
            id = UUID.randomUUID(),
            userId = userId,
            date = date,
            count = count,
            category = category,
            description = description,
            photoMetadataId = photoMetadataId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        return hairFallLogRepository.create(hairFallLog)
    }

    suspend fun getHairFallLogById(id: UUID): HairFallLog? {
        return hairFallLogRepository.findById(id)
    }

    suspend fun getHairFallLogsByUserId(
        userId: UUID,
        limit: Int = 50,
        offset: Int = 0
    ): List<HairFallLog> {
        return hairFallLogRepository.findByUserId(userId, limit, offset)
    }

    suspend fun getHairFallLogsByDateRange(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HairFallLog> {
        return hairFallLogRepository.findByUserIdAndDateRange(userId, startDate, endDate)
    }

    suspend fun updateHairFallLog(
        id: UUID,
        date: LocalDate?,
        count: Int?,
        category: HairFallCategory?,
        description: String?,
        photoMetadataId: UUID?
    ): HairFallLog {
        val existingLog = hairFallLogRepository.findById(id)
            ?: throw IllegalArgumentException("Hair fall log not found")

        val updatedLog = existingLog.copy(
            date = date ?: existingLog.date,
            count = count ?: existingLog.count,
            category = category ?: existingLog.category,
            description = description ?: existingLog.description,
            photoMetadataId = photoMetadataId ?: existingLog.photoMetadataId,
            updatedAt = Instant.now()
        )

        return hairFallLogRepository.update(updatedLog)
    }

    suspend fun deleteHairFallLog(id: UUID): Boolean {
        return hairFallLogRepository.delete(id)
    }

    suspend fun getHairFallStats(userId: UUID): HairFallStatsResponse {
        val logs = hairFallLogRepository.findByUserId(userId, limit = 1000) // Get recent logs for stats
        val totalLogs = hairFallLogRepository.countByUserId(userId)

        val averageCount = logs.mapNotNull { it.count }.let { counts ->
            if (counts.isNotEmpty()) counts.average() else null
        }

        val mostCommonCategory = logs.groupBy { it.category }
            .maxByOrNull { it.value.size }?.key

        val recentTrend = calculateTrend(logs)

        return HairFallStatsResponse(
            totalLogs = totalLogs,
            averageCount = averageCount,
            mostCommonCategory = mostCommonCategory,
            recentTrend = recentTrend
        )
    }

    private fun calculateTrend(logs: List<HairFallLog>): String {
        if (logs.size < 2) return "insufficient_data"

        val sortedLogs = logs.sortedBy { it.date }
        val recentLogs = sortedLogs.takeLast(7) // Last week
        val previousLogs = sortedLogs.dropLast(7).takeLast(7) // Previous week

        val recentAverage = recentLogs.mapNotNull { it.count }.average()
        val previousAverage = previousLogs.mapNotNull { it.count }.average()

        return when {
            recentAverage > previousAverage * 1.1 -> "increasing"
            recentAverage < previousAverage * 0.9 -> "decreasing"
            else -> "stable"
        }
    }
}