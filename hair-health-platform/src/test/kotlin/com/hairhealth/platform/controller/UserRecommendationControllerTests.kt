package com.hairhealth.platform.controller

import com.hairhealth.platform.config.SecurityConfig
import com.hairhealth.platform.security.JwtService
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.RecommendationService
import com.hairhealth.platform.service.dto.BatchRecommendationActionRequest
import com.hairhealth.platform.service.dto.RecommendationActionRequest
import com.hairhealth.platform.service.dto.RecommendationResponse
import io.mockk.coEvery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant
import java.util.UUID

@WebFluxTest(UserRecommendationController::class)
@Import(SecurityConfig::class)
class UserRecommendationControllerTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var recommendationService: RecommendationService
    
    @MockBean
    private lateinit var jwtService: JwtService // For SecurityConfig context

    private val userId = UUID.randomUUID()
    private val recommendationId1 = UUID.randomUUID()
    private val recommendationId2 = UUID.randomUUID()

    private val mockUserPrincipal = UserPrincipal(userId, "user@example.com", "testuser", listOf("USER"))

    private val mockRecResponse1 = RecommendationResponse(
        id = recommendationId1, professionalId = UUID.randomUUID(), userId = userId, consultationId = "c1",
        title = "Rec 1", description = "Desc 1", type = "LIFESTYLE_CHANGE", details = emptyMap(),
        status = "ACTIVE", createdAt = Instant.now(), updatedAt = Instant.now(),
        userAction = "PENDING_ACTION", userActionNotes = null, userActionAt = null
    )
    private val mockRecResponse2 = RecommendationResponse(
        id = recommendationId2, professionalId = UUID.randomUUID(), userId = userId, consultationId = "c2",
        title = "Rec 2", description = "Desc 2", type = "NEW_INTERVENTION", details = emptyMap(),
        status = "ACTIVE", createdAt = Instant.now(), updatedAt = Instant.now(),
        userAction = "PENDING_ACTION", userActionNotes = null, userActionAt = null
    )


    @Test
    fun `testGetMyRecommendations_ReturnsOk`() {
        coEvery { recommendationService.getRecommendationsForUser(userId) } returns listOf(mockRecResponse1, mockRecResponse2)

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/recommendations")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(RecommendationResponse::class.java).hasSize(2)
    }

    @Test
    fun `testBatchAction_ValidRequest_ReturnsOk`() {
        val actionRequest1 = RecommendationActionRequest(recommendationId1, "ACCEPTED")
        val batchRequest = BatchRecommendationActionRequest(listOf(actionRequest1))

        val updatedRecResponse1 = mockRecResponse1.copy(userAction = "ACCEPTED", userActionAt = Instant.now())
        
        // Mock the service to return a list of updated responses
        coEvery { recommendationService.processBatchUserActions(userId, batchRequest) } returns listOf(updatedRecResponse1)

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/recommendations/batch-action")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(batchRequest))
            .exchange()
            .expectStatus().isOk
            .expectBodyList(RecommendationResponse::class.java).hasSize(1)
            // Further assertions can be made on the content of the list items
            .value<List<RecommendationResponse>> { responses ->
                assert(responses[0].userAction == "ACCEPTED")
                assert(responses[0].id == recommendationId1)
            }
    }
    
    @Test
    fun `testBatchAction_PartialFailure_OrServiceThrows_ReturnsError`() {
        // This test depends on how processBatchUserActions handles errors.
        // If it throws on first error (current service impl):
        val actionRequest1 = RecommendationActionRequest(recommendationId1, "ACCEPTED")
        val actionRequest2 = RecommendationActionRequest(recommendationId2, "INVALID_ACTION_TYPE") // This one will fail
        val batchRequest = BatchRecommendationActionRequest(listOf(actionRequest1, actionRequest2))

        // Simulate service throwing an IllegalArgumentException for the second action
        coEvery { recommendationService.processBatchUserActions(userId, batchRequest) } throws IllegalArgumentException("Invalid recommendation action: INVALID_ACTION_TYPE")

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/recommendations/batch-action")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(batchRequest))
            .exchange()
            .expectStatus().isBadRequest // As per current controller error handling for IllegalArgumentException
            // Assert body contains error message if desired
            .expectBody()
            .jsonPath("$.message").isEqualTo("Invalid recommendation action: INVALID_ACTION_TYPE")
    }
}
