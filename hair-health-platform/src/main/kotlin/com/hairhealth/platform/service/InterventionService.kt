package com.hairhealth.platform.service

import com.hairhealth.platform.domain.Intervention
import com.hairhealth.platform.domain.InterventionApplication
import com.hairhealth.platform.domain.InterventionType
import com.hairhealth.platform.repository.InterventionRepository
import com.hairhealth.platform.repository.InterventionApplicationRepository
import com.hairhealth.platform.service.dto.CreateInterventionRequest
import com.hairhealth.platform.service.dto.InterventionApplicationResponse
import com.hairhealth.platform.service.dto.InterventionResponse
import com.hairhealth.platform.service.dto.LogApplicationRequest
import com.hairhealth.platform.service.dto.toDomain
import com.hairhealth.platform.service.dto.toResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Service
class InterventionService(
    private val interventionRepository: InterventionRepository,
    private val interventionApplicationRepository: InterventionApplicationRepository
) {

    suspend fun createIntervention(userId: UUID, request: CreateInterventionRequest): InterventionResponse = withContext(Dispatchers.IO) {
        val intervention = request.toDomain(userId).copy(
            // Ensure new instances get fresh timestamps and ID, if not handled by toDomain or default constructor
            id = UUID.randomUUID(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        interventionRepository.create(intervention).toResponse()
    }

    suspend fun getInterventionById(userId: UUID, interventionId: UUID): InterventionResponse? = withContext(Dispatchers.IO) {
        interventionRepository.findByIdAndUserId(interventionId, userId)?.toResponse()
    }

    suspend fun getInterventionsForUser(userId: UUID, includeInactive: Boolean): List<InterventionResponse> = withContext(Dispatchers.IO) {
        interventionRepository.findByUserId(userId, includeInactive).map { it.toResponse() }
    }

    // Keeping existing getActiveInterventionsByUserId for potential internal use or if other parts of app use it.
    suspend fun getActiveInterventionsByUserId(userId: UUID): List<Intervention> {
        return interventionRepository.findActiveByUserId(userId)
    }

    // Keeping existing updateIntervention for potential internal use.
    suspend fun updateIntervention(intervention: Intervention): Intervention {
        return interventionRepository.update(intervention.copy(updatedAt = Instant.now()))
    }

    // Keeping existing deactivateIntervention.
    suspend fun deactivateIntervention(id: UUID): Boolean {
        return interventionRepository.deactivate(id)
    }

    suspend fun logInterventionApplication(userId: UUID, interventionId: UUID, request: LogApplicationRequest): InterventionApplicationResponse = withContext(Dispatchers.IO) {
        val intervention = interventionRepository.findByIdAndUserId(interventionId, userId)
            ?: throw InterventionNotFoundException("Active intervention not found for user or ID: $interventionId")

        if (!intervention.isActive) {
             throw InterventionInteractionException("Cannot log application for an inactive intervention.")
        }

        val application = request.toDomain(interventionId, userId, Instant.now()).copy(
             id = UUID.randomUUID(),
             createdAt = Instant.now()
        )
        interventionApplicationRepository.create(application).toResponse()
    }

    suspend fun getApplicationsForIntervention(userId: UUID, interventionId: UUID, limit: Int, offset: Int): List<InterventionApplicationResponse> = withContext(Dispatchers.IO) {
        // Verify user owns the intervention first
        interventionRepository.findByIdAndUserId(interventionId, userId)
            ?: throw InterventionNotFoundException("Intervention not found for user or ID: $interventionId")

        interventionApplicationRepository.findByInterventionId(interventionId, limit, offset).map { it.toResponse() }
    }

    // Keeping existing getApplicationsByUserIdAndDateRange for potential internal use.
    // Note: Its signature in repository (LocalDate) differs from the updated one (Instant).
    // This might need alignment if this method is to be used widely and consistently.
    // For this subtask, this method is not directly exposed via the new controller endpoints.
    suspend fun getApplicationsByUserIdAndDateRange(
        userId: UUID,
        startDate: Instant, // Matching updated repo interface
        endDate: Instant   // Matching updated repo interface
    ): List<InterventionApplication> {
        return interventionApplicationRepository.findByUserIdAndDateRange(userId, startDate, endDate)
    }
}

// Custom Exceptions
class InterventionNotFoundException(message: String) : RuntimeException(message)
class InterventionInteractionException(message: String) : RuntimeException(message)