package com.hairhealth.platform.controller

import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.RecommendationService
import com.hairhealth.platform.service.RecommendationAccessException
import com.hairhealth.platform.service.RecommendationActionException
import com.hairhealth.platform.service.RecommendationNotFoundException
import com.hairhealth.platform.service.dto.BatchRecommendationActionRequest
import com.hairhealth.platform.service.dto.RecommendationResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1/me/recommendations")
// Assuming general authentication for "/api/v1/me/**" is handled by SecurityConfig
// Specific role checks like @PreAuthorize("hasAuthority('ROLE_USER')") can be added if needed.
class UserRecommendationController(
    private val recommendationService: RecommendationService
) {

    @GetMapping
    suspend fun getMyRecommendations(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<RecommendationResponse>> {
        val userId = principal.userId
        val recommendations = recommendationService.getRecommendationsForUser(userId)
        return ResponseEntity.ok(recommendations)
    }

    @PostMapping("/batch-action")
    suspend fun batchProcessRecommendationActions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: BatchRecommendationActionRequest
    ): ResponseEntity<Any> {
        val userId = principal.userId
        try {
            // Note: The service's batchProcessUserActions currently processes sequentially
            // and will throw an exception on the first error.
            // For a true batch response with partial successes, the service and controller
            // would need to be adjusted to return a list of individual action results.
            val results = recommendationService.processBatchUserActions(userId, request)
            return ResponseEntity.ok(results)
        } catch (e: RecommendationNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: RecommendationAccessException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("message" to e.message))
        } catch (e: RecommendationActionException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        } catch (e: Exception) {
            // Log the exception server-side for debugging
            // e.printStackTrace() // Or use a proper logger
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "An unexpected error occurred while processing batch actions."))
        }
    }
}
