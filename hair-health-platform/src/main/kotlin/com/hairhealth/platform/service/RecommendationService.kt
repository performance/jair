package com.hairhealth.platform.service

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.RecommendationRepository
import com.hairhealth.platform.repository.UserRepository 
import com.hairhealth.platform.service.dto.*
import com.hairhealth.platform.service.dto.toDomain
import com.hairhealth.platform.service.dto.toResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class RecommendationService(
    private val recommendationRepository: RecommendationRepository,
    private val userRepository: UserRepository, // For validating user_id
    private val auditLogService: AuditLogService // Injected AuditLogService
) {

    suspend fun createRecommendation(professionalId: UUID, request: CreateRecommendationRequest): RecommendationResponse = withContext(Dispatchers.IO) {
        // Validate that the target user (patient) exists
        userRepository.findById(request.userId)
            ?: throw UserNotFoundException("User (patient) with ID ${request.userId} not found.")

        val recommendationDomain = request.toDomain(professionalId)
        val savedRecommendation = recommendationRepository.save(recommendationDomain)

        auditLogService.logEvent(
            actorId = professionalId.toString(),
            actorType = ActorType.PROFESSIONAL, // Assuming professional creates recommendations
            action = "CREATE_RECOMMENDATION_SUCCESS",
            targetEntityType = "RECOMMENDATION",
            targetEntityId = savedRecommendation.id.toString(),
            status = AuditEventStatus.SUCCESS,
            details = mapOf(
                "professionalId" to professionalId.toString(),
                "patientUserId" to request.userId.toString(),
                "recommendationId" to savedRecommendation.id.toString(),
                "type" to request.type
            )
        )
        savedRecommendation.toResponse()
    }

    suspend fun getRecommendationById(professionalId: UUID, recommendationId: UUID): RecommendationResponse? = withContext(Dispatchers.IO) {
        recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId)
            ?.takeIf { it.status != RecommendationStatus.DELETED } // Don't return deleted ones directly by ID unless specified
            ?.toResponse()
    }

    suspend fun getRecommendationsByProfessional(professionalId: UUID, patientUserId: UUID?): List<RecommendationResponse> = withContext(Dispatchers.IO) {
        val recommendations = if (patientUserId != null) {
            recommendationRepository.findByProfessionalIdAndUserId(professionalId, patientUserId)
        } else {
            recommendationRepository.findByProfessionalId(professionalId)
        }
        // Filter out DELETED ones if not already handled by repository (repository methods currently do this)
        recommendations.map { it.toResponse() }
    }

    suspend fun updateRecommendation(
        professionalId: UUID,
        recommendationId: UUID,
        request: UpdateRecommendationRequest
    ): RecommendationResponse? = withContext(Dispatchers.IO) {
        val existingRecommendation = recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId)
            ?: return@withContext null // Not found or doesn't belong to professional

        // Cannot update a DELETED recommendation unless specifically changing status to non-DELETED
        if (existingRecommendation.status == RecommendationStatus.DELETED && (request.status == null || RecommendationStatus.valueOf(request.status.uppercase()) == RecommendationStatus.DELETED)) {
            throw RecommendationUpdateException("Cannot update a DELETED recommendation unless reactivating it.")
        }
        
        val updatedRecommendation = existingRecommendation.copy(
            title = request.title ?: existingRecommendation.title,
            description = request.description ?: existingRecommendation.description,
            type = request.type?.let {
                try { RecommendationType.valueOf(it.uppercase()) }
                catch (e: IllegalArgumentException) { throw IllegalArgumentException("Invalid recommendation type: $it")}
            } ?: existingRecommendation.type,
            details = request.details?.let { recommendationObjectMapper.writeValueAsString(it) } ?: existingRecommendation.details,
            status = request.status?.let {
                try { RecommendationStatus.valueOf(it.uppercase()) }
                catch (e: IllegalArgumentException) { throw IllegalArgumentException("Invalid recommendation status: $it")}
            } ?: existingRecommendation.status,
            updatedAt = Instant.now()
        )
        val savedUpdatedRec = recommendationRepository.save(updatedRecommendation)

        auditLogService.logEvent(
            actorId = professionalId.toString(),
            actorType = ActorType.PROFESSIONAL,
            action = "UPDATE_RECOMMENDATION_SUCCESS",
            targetEntityType = "RECOMMENDATION",
            targetEntityId = savedUpdatedRec.id.toString(),
            status = AuditEventStatus.SUCCESS,
            details = mapOf(
                "professionalId" to professionalId.toString(),
                "recommendationId" to savedUpdatedRec.id.toString(),
                "updatedFields" to request.toString() // Consider more specific field logging if needed
            )
        )
        savedUpdatedRec.toResponse()
    }

    suspend fun deleteRecommendation(professionalId: UUID, recommendationId: UUID): Boolean = withContext(Dispatchers.IO) {
        val recommendation = recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId)
            ?: return@withContext false // Not found or doesn't belong to professional

        if (recommendation.status == RecommendationStatus.DELETED) {
            // Optionally log attempt to delete already deleted item, or just return true
            return@withContext true // Already deleted
        }

        val deletedRecommendation = recommendation.copy(
            status = RecommendationStatus.DELETED,
            updatedAt = Instant.now()
        )
        recommendationRepository.save(deletedRecommendation)

        auditLogService.logEvent(
            actorId = professionalId.toString(),
            actorType = ActorType.PROFESSIONAL,
            action = "DELETE_RECOMMENDATION_SUCCESS", // Soft delete
            targetEntityType = "RECOMMENDATION",
            targetEntityId = recommendation.id.toString(), 
            status = AuditEventStatus.SUCCESS,
            details = mapOf(
                "professionalId" to professionalId.toString(),
                "recommendationId" to recommendation.id.toString(),
                "newStatus" to RecommendationStatus.DELETED.name
            )
        )
        true
    }

    // --- Methods for User (Patient) actions ---

    suspend fun getRecommendationsForUser(userId: UUID): List<RecommendationResponse> = withContext(Dispatchers.IO) {
        // Fetch ACTIVE recommendations, could also filter by userAction PENDING or null if desired.
        // For now, returning all ACTIVE ones for the user, client can filter by userAction if needed.
        recommendationRepository.findByUserIdAndStatus(userId, RecommendationStatus.ACTIVE, null)
            .map { it.toResponse() }
    }

    suspend fun processUserAction(userId: UUID, actionRequest: RecommendationActionRequest): RecommendationResponse = withContext(Dispatchers.IO) {
        try {
            val recommendation = recommendationRepository.findById(actionRequest.recommendationId)
                ?: throw RecommendationNotFoundException("Recommendation with ID ${actionRequest.recommendationId} not found.")

            if (recommendation.userId != userId) {
                throw RecommendationAccessException("User does not have access to recommendation ID ${actionRequest.recommendationId}.")
            }

            if (recommendation.status != RecommendationStatus.ACTIVE) {
                throw RecommendationActionException("Cannot act on a recommendation that is not ACTIVE. Current status: ${recommendation.status}")
            }

            val userAction = try {
                UserRecommendationAction.valueOf(actionRequest.action.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid recommendation action: ${actionRequest.action}")
            }

            var notes: String? = null
        when (userAction) {
            UserRecommendationAction.ACCEPTED_WITH_MODIFICATIONS -> {
                if (actionRequest.modifications == null || actionRequest.modifications.isEmpty()) {
                    throw IllegalArgumentException("Modifications are required for action ACCEPTED_WITH_MODIFICATIONS.")
                }
                notes = "Modifications: ${recommendationObjectMapper.writeValueAsString(actionRequest.modifications)}"
                // Placeholder: Here, you might trigger InterventionService.createOrUpdateFromRecommendation(recommendation, actionRequest.modifications)
            }
            UserRecommendationAction.DECLINED -> {
                if (actionRequest.declineReason.isNullOrBlank()) {
                    throw IllegalArgumentException("Decline reason is required for action DECLINED.")
                }
                notes = "Declined Reason: ${actionRequest.declineReason}"
            }
            UserRecommendationAction.ACCEPTED -> {
                notes = "User accepted."
                 // Placeholder: Here, you might trigger InterventionService.createOrUpdateFromRecommendation(recommendation)
            }
            UserRecommendationAction.PENDING_ACTION -> {
                // This might be used to reset an action, notes could be optional or specific.
                notes = "Action reset to pending."
            }
        }

            val updatedRecommendation = recommendation.copy(
                userAction = userAction,
                userActionNotes = notes,
                userActionAt = Instant.now(),
                updatedAt = Instant.now() // Also update the general updatedAt timestamp
            )

            val savedRecommendation = recommendationRepository.save(updatedRecommendation)

            auditLogService.logEvent(
                actorId = userId.toString(),
                actorType = ActorType.USER,
                action = "USER_ACTION_ON_RECOMMENDATION_SUCCESS",
                targetEntityType = "RECOMMENDATION",
                targetEntityId = savedRecommendation.id.toString(),
                status = AuditEventStatus.SUCCESS,
                details = mapOf(
                    "userId" to userId.toString(),
                    "recommendationId" to savedRecommendation.id.toString(),
                    "actionTaken" to userAction.name,
                    "notes" to notes
                )
            )
            return@withContext savedRecommendation.toResponse()

        } catch (e: Exception) {
            // Log failure
            auditLogService.logEvent(
                actorId = userId.toString(),
                actorType = ActorType.USER,
                action = "USER_ACTION_ON_RECOMMENDATION_FAILURE",
                targetEntityType = "RECOMMENDATION",
                targetEntityId = actionRequest.recommendationId.toString(),
                status = AuditEventStatus.FAILURE,
                details = mapOf(
                    "userId" to userId.toString(),
                    "recommendationId" to actionRequest.recommendationId.toString(),
                    "actionAttempted" to actionRequest.action,
                    "error" to e.message
                )
            )
            throw e // Re-throw the original exception
        }
    }

    suspend fun processBatchUserActions(userId: UUID, batchRequest: BatchRecommendationActionRequest): List<RecommendationResponse> = withContext(Dispatchers.IO) {
        // For simplicity, processing sequentially. For higher throughput, could explore parallel processing (e.g. coroutines).
        // Error handling: if one fails, should others proceed? For now, let individual actions throw exceptions.
        // A more robust batch might return a list of success/failure statuses for each action.
        batchRequest.actions.map { actionRequest ->
            processUserAction(userId, actionRequest)
        }
    }
}

// Custom Exceptions for more specific error handling
class UserNotFoundException(message: String) : RuntimeException(message)
class RecommendationUpdateException(message: String): RuntimeException(message)
class RecommendationNotFoundException(message: String) : RuntimeException(message)
class RecommendationAccessException(message: String) : RuntimeException(message)
class RecommendationActionException(message: String) : RuntimeException(message)
