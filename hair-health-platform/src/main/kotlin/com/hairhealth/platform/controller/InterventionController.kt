package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.InterventionType
import com.hairhealth.platform.service.AdherenceStats
import com.hairhealth.platform.service.InterventionService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/me/interventions")
class InterventionController(
    private val interventionService: InterventionService
) {

    @PostMapping
    suspend fun createIntervention(@RequestBody request: CreateInterventionRequest): InterventionResponse {
        val intervention = interventionService.createIntervention(
            userId = request.userId, // TODO: Extract from JWT
            type = request.type,
            productName = request.productName,
            dosageAmount = request.dosageAmount,
            frequency = request.frequency,
            applicationTime = request.applicationTime,
            startDate = request.startDate,
            endDate = request.endDate,
            provider = request.provider,
            notes = request.notes,
            sourceRecommendationId = request.sourceRecommendationId
        )

        return intervention.toResponse()
    }

    @GetMapping
    suspend fun getInterventions(
        @RequestParam(defaultValue = "dummy-user-id") userId: String, // TODO: Extract from JWT
        @RequestParam(defaultValue = "false") includeInactive: Boolean
    ): List<InterventionResponse> {
        val interventions = interventionService.getInterventionsByUserId(
            UUID.fromString(userId),
            includeInactive
        )
        return interventions.map { it.toResponse() }
    }

    @GetMapping("/active")
    suspend fun getActiveInterventions(
        @RequestParam(defaultValue = "dummy-user-id") userId: String // TODO: Extract from JWT
    ): List<InterventionResponse> {
        val interventions = interventionService.getActiveInterventionsByUserId(UUID.fromString(userId))
        return interventions.map { it.toResponse() }
    }

    @GetMapping("/{id}")
    suspend fun getIntervention(@PathVariable id: UUID): InterventionResponse? {
        val intervention = interventionService.getInterventionById(id)
        return intervention?.toResponse()
    }

    @PutMapping("/{id}")
    suspend fun updateIntervention(
        @PathVariable id: UUID,
        @RequestBody request: UpdateInterventionRequest
    ): InterventionResponse {
        val existingIntervention = interventionService.getInterventionById(id)
            ?: throw IllegalArgumentException("Intervention not found")

        val updatedIntervention = existingIntervention.copy(
            type = request.type ?: existingIntervention.type,
            productName = request.productName ?: existingIntervention.productName,
            dosageAmount = request.dosageAmount ?: existingIntervention.dosageAmount,
            frequency = request.frequency ?: existingIntervention.frequency,
            applicationTime = request.applicationTime ?: existingIntervention.applicationTime,
            startDate = request.startDate ?: existingIntervention.startDate,
            endDate = request.endDate ?: existingIntervention.endDate,
            provider = request.provider ?: existingIntervention.provider,
            notes = request.notes ?: existingIntervention.notes
        )

        val result = interventionService.updateIntervention(updatedIntervention)
        return result.toResponse()
    }

    @PostMapping("/{id}/deactivate")
    suspend fun deactivateIntervention(@PathVariable id: UUID): Map<String, String> {
        val deactivated = interventionService.deactivateIntervention(id)
        return if (deactivated) {
            mapOf("status" to "deactivated")
        } else {
            mapOf("status" to "not_found")
        }
    }

    @PostMapping("/{id}/log-application")
    suspend fun logApplication(
        @PathVariable id: UUID,
        @RequestBody request: LogApplicationRequest
    ): InterventionApplicationResponse {
        val application = interventionService.logApplication(
            interventionId = id,
            userId = request.userId, // TODO: Extract from JWT
            timestamp = request.timestamp ?: Instant.now(),
            notes = request.notes
        )

        return application.toResponse()
    }

    @GetMapping("/{id}/applications")
    suspend fun getApplications(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<InterventionApplicationResponse> {
        val applications = interventionService.getApplicationsByInterventionId(id, limit, offset)
        return applications.map { it.toResponse() }
    }

    @GetMapping("/{id}/adherence")
    suspend fun getAdherenceStats(@PathVariable id: UUID): AdherenceStatsResponse {
        val stats = interventionService.getAdherenceStats(id)
        return AdherenceStatsResponse(
            interventionId = stats.interventionId,
            expectedApplications = stats.expectedApplications,
            actualApplications = stats.actualApplications,
            adherenceRate = stats.adherenceRate,
            adherencePercentage = (stats.adherenceRate * 100).toInt(),
            daysSinceStart = stats.daysSinceStart,
            lastApplication = stats.lastApplication?.toString(),
            adherenceLevel = when {
                stats.adherenceRate >= 0.9 -> "EXCELLENT"
                stats.adherenceRate >= 0.75 -> "GOOD"
                stats.adherenceRate >= 0.5 -> "FAIR"
                else -> "POOR"
            }
        )
    }

    @GetMapping("/applications/date-range")
    suspend fun getApplicationsByDateRange(
        @RequestParam(defaultValue = "dummy-user-id") userId: String, // TODO: Extract from JWT
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): List<InterventionApplicationResponse> {
        val applications = interventionService.getApplicationsByUserIdAndDateRange(
            UUID.fromString(userId),
            startDate,
            endDate
        )
        return applications.map { it.toResponse() }
    }
}

// Request/Response DTOs
data class CreateInterventionRequest(
    val userId: UUID, // TODO: Remove when extracting from JWT
    val type: InterventionType,
    val productName: String,
    val dosageAmount: String?,
    val frequency: String,
    val applicationTime: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val provider: String?,
    val notes: String?,
    val sourceRecommendationId: UUID?
)

data class UpdateInterventionRequest(
    val type: InterventionType?,
    val productName: String?,
    val dosageAmount: String?,
    val frequency: String?,
    val applicationTime: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val provider: String?,
    val notes: String?
)

data class LogApplicationRequest(
    val userId: UUID, // TODO: Remove when extracting from JWT
    val timestamp: Instant?,
    val notes: String?
)

data class InterventionResponse(
    val id: UUID,
    val userId: UUID,
    val type: InterventionType,
    val productName: String,
    val dosageAmount: String?,
    val frequency: String,
    val applicationTime: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val provider: String?,
    val notes: String?,
    val sourceRecommendationId: UUID?,
    val createdAt: String,
    val updatedAt: String
)

data class InterventionApplicationResponse(
    val id: UUID,
    val interventionId: UUID,
    val userId: UUID,
    val timestamp: String,
    val notes: String?,
    val createdAt: String
)

data class AdherenceStatsResponse(
    val interventionId: UUID,
    val expectedApplications: Int,
    val actualApplications: Int,
    val adherenceRate: Double,
    val adherencePercentage: Int,
    val daysSinceStart: Int,
    val lastApplication: String?,
    val adherenceLevel: String
)

// Extension functions
private fun com.hairhealth.platform.domain.Intervention.toResponse() = InterventionResponse(
    id = this.id,
    userId = this.userId,
    type = this.type,
    productName = this.productName,
    dosageAmount = this.dosageAmount,
    frequency = this.frequency,
    applicationTime = this.applicationTime,
    startDate = this.startDate,
    endDate = this.endDate,
    isActive = this.isActive,
    provider = this.provider,
    notes = this.notes,
    sourceRecommendationId = this.sourceRecommendationId,
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString()
)

private fun com.hairhealth.platform.domain.InterventionApplication.toResponse() = InterventionApplicationResponse(
    id = this.id,
    interventionId = this.interventionId,
    userId = this.userId,
    timestamp = this.timestamp.toString(),
    notes = this.notes,
    createdAt = this.createdAt.toString()
)