package com.hairhealth.platform.service

import com.hairhealth.platform.domain.Intervention
import com.hairhealth.platform.domain.InterventionApplication
import com.hairhealth.platform.domain.InterventionType
import com.hairhealth.platform.repository.InterventionRepository
import com.hairhealth.platform.repository.InterventionApplicationRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
class InterventionService(
    private val interventionRepository: InterventionRepository,
    private val interventionApplicationRepository: InterventionApplicationRepository
) {

    suspend fun createIntervention(
        userId: UUID,
        type: InterventionType,
        productName: String,
        dosageAmount: String?,
        frequency: String,
        applicationTime: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        provider: String?,
        notes: String?,
        sourceRecommendationId: UUID?
    ): Intervention {
        val intervention = Intervention(
            id = UUID.randomUUID(),
            userId = userId,
            type = type,
            productName = productName,
            dosageAmount = dosageAmount,
            frequency = frequency,
            applicationTime = applicationTime,
            startDate = startDate,
            endDate = endDate,
            isActive = true,
            provider = provider,
            notes = notes,
            sourceRecommendationId = sourceRecommendationId,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        return interventionRepository.create(intervention)
    }

    suspend fun getInterventionById(id: UUID): Intervention? {
        return interventionRepository.findById(id)
    }

    suspend fun getInterventionsByUserId(userId: UUID, includeInactive: Boolean = false): List<Intervention> {
        return interventionRepository.findByUserId(userId, includeInactive)
    }

    suspend fun getActiveInterventionsByUserId(userId: UUID): List<Intervention> {
        return interventionRepository.findActiveByUserId(userId)
    }

    suspend fun updateIntervention(intervention: Intervention): Intervention {
        return interventionRepository.update(intervention.copy(updatedAt = Instant.now()))
    }

    suspend fun deactivateIntervention(id: UUID): Boolean {
        return interventionRepository.deactivate(id)
    }

    suspend fun logApplication(
        interventionId: UUID,
        userId: UUID,
        timestamp: Instant = Instant.now(),
        notes: String?
    ): InterventionApplication {
        val application = InterventionApplication(
            id = UUID.randomUUID(),
            interventionId = interventionId,
            userId = userId,
            timestamp = timestamp,
            notes = notes,
            createdAt = Instant.now()
        )

        return interventionApplicationRepository.create(application)
    }

    suspend fun getApplicationsByInterventionId(
        interventionId: UUID,
        limit: Int = 50,
        offset: Int = 0
    ): List<InterventionApplication> {
        return interventionApplicationRepository.findByInterventionId(interventionId, limit, offset)
    }

    suspend fun getApplicationsByUserIdAndDateRange(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<InterventionApplication> {
        return interventionApplicationRepository.findByUserIdAndDateRange(userId, startDate, endDate)
    }

    suspend fun getAdherenceStats(interventionId: UUID): AdherenceStats {
        val intervention = interventionRepository.findById(interventionId)
            ?: throw IllegalArgumentException("Intervention not found")

        val applications = interventionApplicationRepository.findByInterventionId(interventionId, limit = 1000)
        
        // Calculate adherence based on frequency and applications
        val expectedApplicationsPerDay = parseFrequencyToDaily(intervention.frequency)
        val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(intervention.startDate, LocalDate.now()) + 1
        val expectedTotalApplications = (daysSinceStart * expectedApplicationsPerDay).toInt()
        
        val actualApplications = applications.size
        val adherenceRate = if (expectedTotalApplications > 0) {
            (actualApplications.toDouble() / expectedTotalApplications).coerceAtMost(1.0)
        } else 0.0

        return AdherenceStats(
            interventionId = interventionId,
            expectedApplications = expectedTotalApplications,
            actualApplications = actualApplications,
            adherenceRate = adherenceRate,
            daysSinceStart = daysSinceStart.toInt(),
            lastApplication = applications.maxByOrNull { it.timestamp }?.timestamp
        )
    }

    private fun parseFrequencyToDaily(frequency: String): Double {
        return when (frequency.lowercase()) {
            "once daily", "daily", "1x daily" -> 1.0
            "twice daily", "2x daily" -> 2.0
            "three times daily", "3x daily" -> 3.0
            "every other day", "every 2 days" -> 0.5
            "twice weekly", "2x weekly" -> 2.0 / 7.0
            "weekly", "once weekly", "1x weekly" -> 1.0 / 7.0
            else -> 1.0 // Default assumption
        }
    }
}

data class AdherenceStats(
    val interventionId: UUID,
    val expectedApplications: Int,
    val actualApplications: Int,
    val adherenceRate: Double,
    val daysSinceStart: Int,
    val lastApplication: Instant?
)