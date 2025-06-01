package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.Recommendation
import java.util.UUID

interface RecommendationRepository {
    /**
     * Saves a recommendation. If it's a new recommendation (ID doesn't exist), it creates it.
     * If it exists, it updates it.
     * @param recommendation The recommendation object to save.
     * @return The saved recommendation object.
     */
    suspend fun save(recommendation: Recommendation): Recommendation

    /**
     * Finds a recommendation by its ID.
     * @param id The UUID of the recommendation.
     * @return The Recommendation object if found, null otherwise.
     */
    suspend fun findById(id: UUID): Recommendation?

    /**
     * Finds all recommendations created by a specific professional.
     * @param professionalId The UUID of the professional.
     * @return A list of Recommendation objects.
     */
    suspend fun findByProfessionalId(professionalId: UUID): List<Recommendation>

    /**
     * Finds all recommendations created by a specific professional for a specific user.
     * @param professionalId The UUID of the professional.
     * @param userId The UUID of the user (patient).
     * @return A list of Recommendation objects.
     */
    suspend fun findByProfessionalIdAndUserId(professionalId: UUID, userId: UUID): List<Recommendation>


    /**
     * Finds a specific recommendation by its ID, ensuring it belongs to the given professional.
     * This is useful for verifying ownership before an update or delete operation.
     * @param professionalId The UUID of the professional.
     * @param id The UUID of the recommendation.
     * @return The Recommendation object if found and owned by the professional, null otherwise.
     */
    suspend fun findByProfessionalIdAndId(professionalId: UUID, id: UUID): Recommendation?

    // Note: A specific delete method is not strictly needed if soft delete is handled
    // by updating the status through the save method. If hard delete were required,
    // a suspend fun deleteById(id: UUID) would be added.

    /**
     * Finds recommendations for a user based on professional's status (e.g., 'ACTIVE')
     * and optionally the user's action status.
     * @param userId The UUID of the user.
     * @param professionalStatus The status set by the professional (e.g., RecommendationStatus.ACTIVE).
     * @param userActionStatus The action taken by the user (optional). If null, not filtered by user action.
     * @return A list of Recommendation objects.
     */
    suspend fun findByUserIdAndStatus(
        userId: UUID,
        professionalStatus: com.hairhealth.platform.domain.RecommendationStatus,
        userActionStatus: com.hairhealth.platform.domain.UserRecommendationAction?
    ): List<Recommendation>
}
