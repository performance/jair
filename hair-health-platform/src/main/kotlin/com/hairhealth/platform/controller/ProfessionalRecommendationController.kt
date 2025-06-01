package com.hairhealth.platform.controller

import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.RecommendationService
import com.hairhealth.platform.service.UserNotFoundException
import com.hairhealth.platform.service.RecommendationUpdateException
import com.hairhealth.platform.service.dto.CreateRecommendationRequest
import com.hairhealth.platform.service.dto.RecommendationResponse
import com.hairhealth.platform.service.dto.UpdateRecommendationRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid // For @Valid if using Spring Boot 3+
// import javax.validation.Valid // For @Valid if using Spring Boot 2.x
import java.util.UUID

@RestController
@RequestMapping("/api/v1/professionals/me/recommendations")
// @PreAuthorize("hasAuthority('ROLE_PROFESSIONAL')") // Uncomment when roles are fully set up
class ProfessionalRecommendationController(
    private val recommendationService: RecommendationService
) {

    @PostMapping
    suspend fun createRecommendation(
        @AuthenticationPrincipal principal: UserPrincipal, // Assuming UserPrincipal has userId for the professional
        @Valid @RequestBody request: CreateRecommendationRequest
    ): ResponseEntity<Any> {
        // TODO: Add @PreAuthorize("hasAuthority('ROLE_PROFESSIONAL')") or check principal.role if available
        // For now, we assume the principal.userId is the professional's ID.
        // A check like: if (!principal.roles.contains("PROFESSIONAL")) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        // might be needed if @PreAuthorize isn't active yet.

        val professionalId = principal.userId
        return try {
            val recommendation = recommendationService.createRecommendation(professionalId, request)
            ResponseEntity.status(HttpStatus.CREATED).body(recommendation)
        } catch (e: UserNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to "An unexpected error occurred."))
        }
    }

    @GetMapping
    suspend fun getRecommendations(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) patientUserId: UUID?
    ): ResponseEntity<List<RecommendationResponse>> {
        // TODO: Add role check as above
        val professionalId = principal.userId
        val recommendations = recommendationService.getRecommendationsByProfessional(professionalId, patientUserId)
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/{recommendationId}")
    suspend fun getRecommendationById(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable recommendationId: UUID
    ): ResponseEntity<Any> {
        // TODO: Add role check
        val professionalId = principal.userId
        return recommendationService.getRecommendationById(professionalId, recommendationId)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Recommendation not found or you do not have access."))
    }

    @PutMapping("/{recommendationId}")
    suspend fun updateRecommendation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable recommendationId: UUID,
        @Valid @RequestBody request: UpdateRecommendationRequest
    ): ResponseEntity<Any> {
        // TODO: Add role check
        val professionalId = principal.userId
        return try {
            recommendationService.updateRecommendation(professionalId, recommendationId, request)
                ?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Recommendation not found or you do not have permission to update it."))
        } catch (e: RecommendationUpdateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("message" to "An unexpected error occurred during update."))
        }
    }

    @DeleteMapping("/{recommendationId}")
    suspend fun deleteRecommendation(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable recommendationId: UUID
    ): ResponseEntity<Any> {
        // TODO: Add role check
        val professionalId = principal.userId
        val success = recommendationService.deleteRecommendation(professionalId, recommendationId)
        return if (success) {
            ResponseEntity.noContent().build() // Or ResponseEntity.ok().body(mapOf("message" to "Recommendation deleted successfully"))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to "Recommendation not found or you do not have permission to delete it."))
        }
    }
}
