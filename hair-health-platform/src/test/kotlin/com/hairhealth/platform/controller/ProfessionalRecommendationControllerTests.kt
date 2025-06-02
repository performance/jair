package com.hairhealth.platform.controller

import com.hairhealth.platform.config.SecurityConfig
import com.hairhealth.platform.security.JwtService
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.RecommendationService
import com.hairhealth.platform.service.UserNotFoundException
import com.hairhealth.platform.service.dto.CreateRecommendationRequest
import com.hairhealth.platform.service.dto.RecommendationResponse
import com.hairhealth.platform.service.dto.recommendationObjectMapper
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

@WebFluxTest(ProfessionalRecommendationController::class)
@Import(SecurityConfig::class)
class ProfessionalRecommendationControllerTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var recommendationService: RecommendationService

    @MockBean
    private lateinit var jwtService: JwtService // For SecurityConfig context

    private val professionalId = UUID.randomUUID()
    private val patientUserId = UUID.randomUUID()
    private val recommendationId = UUID.randomUUID()

    private val mockProfessionalPrincipal = UserPrincipal(professionalId, "prof@example.com", "profuser", listOf("PROFESSIONAL"))

    private val createRecRequest = CreateRecommendationRequest(
        userId = patientUserId,
        consultationId = "consult-123",
        title = "Test Rec",
        description = "Test Description",
        type = "TREATMENT_ADJUSTMENT",
        details = mapOf("dosage" to "1ml")
    )

    private val mockRecResponse = RecommendationResponse(
        id = recommendationId,
        professionalId = professionalId,
        userId = patientUserId,
        consultationId = "consult-123",
        title = "Test Rec",
        description = "Test Description",
        type = "TREATMENT_ADJUSTMENT",
        details = mapOf("dosage" to "1ml"),
        status = "ACTIVE",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        userAction = null, userActionNotes = null, userActionAt = null
    )

    @Test
    fun `testCreateRecommendation_ValidRequest_ReturnsCreated`() {
        coEvery { recommendationService.createRecommendation(professionalId, createRecRequest) } returns mockRecResponse

        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .post().uri("/api/v1/professionals/me/recommendations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(createRecRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(RecommendationResponse::class.java).isEqualTo(mockRecResponse)
    }
    
    @Test
    fun `testCreateRecommendation_PatientNotFound_ReturnsNotFound`() {
        coEvery { recommendationService.createRecommendation(professionalId, createRecRequest) } throws UserNotFoundException("Patient not found")

        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .post().uri("/api/v1/professionals/me/recommendations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(createRecRequest))
            .exchange()
            .expectStatus().isNotFound
    }
    
    @Test
    fun `testCreateRecommendation_InvalidTypeInRequest_ReturnsBadRequest`() {
        val invalidRequest = createRecRequest.copy(type="INVALID_TYPE_FOO")
        // This exception would likely originate from DTO mapping or service validation before service method body
        coEvery { recommendationService.createRecommendation(professionalId, invalidRequest) } throws IllegalArgumentException("Invalid recommendation type")


        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .post().uri("/api/v1/professionals/me/recommendations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(invalidRequest))
            .exchange()
            .expectStatus().isBadRequest
    }


    @Test
    fun `testGetRecommendations_ByProfessional_ReturnsOk`() {
        coEvery { recommendationService.getRecommendationsByProfessional(professionalId, null) } returns listOf(mockRecResponse)

        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .get().uri("/api/v1/professionals/me/recommendations")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(RecommendationResponse::class.java).hasSize(1).contains(mockRecResponse)
    }
    
    @Test
    fun `testGetRecommendations_ByProfessionalAndPatient_ReturnsOk`() {
        coEvery { recommendationService.getRecommendationsByProfessional(professionalId, patientUserId) } returns listOf(mockRecResponse)

        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .get().uri { uriBuilder ->
                uriBuilder.path("/api/v1/professionals/me/recommendations")
                    .queryParam("patientUserId", patientUserId.toString())
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBodyList(RecommendationResponse::class.java).hasSize(1).contains(mockRecResponse)
    }


    @Test
    fun `testGetRecommendationById_ProfessionalOwns_ReturnsOk`() {
        coEvery { recommendationService.getRecommendationById(professionalId, recommendationId) } returns mockRecResponse
        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .get().uri("/api/v1/professionals/me/recommendations/${recommendationId}")
            .exchange()
            .expectStatus().isOk
            .expectBody(RecommendationResponse::class.java).isEqualTo(mockRecResponse)
    }

    @Test
    fun `testGetRecommendationById_NotOwnedOrNonExistent_ReturnsNotFound`() {
        coEvery { recommendationService.getRecommendationById(professionalId, recommendationId) } returns null
        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .get().uri("/api/v1/professionals/me/recommendations/${recommendationId}")
            .exchange()
            .expectStatus().isNotFound
    }
    
    @Test
    fun `testUpdateRecommendation_ValidRequest_ReturnsOk`() {
        val updateRequestMap = mapOf("title" to "Updated Title", "status" to "SUPERSEDED")
        // val updateRequest = UpdateRecommendationRequest(title = "Updated Title", status = "SUPERSEDED") // If using typed DTO
        val updatedResponse = mockRecResponse.copy(title = "Updated Title", status = "SUPERSEDED")

        coEvery { recommendationService.updateRecommendation(eq(professionalId), eq(recommendationId), any()) } returns updatedResponse
        
        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .put().uri("/api/v1/professionals/me/recommendations/${recommendationId}")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(updateRequestMap)) // Using map for flexibility if Update DTO is complex
            .exchange()
            .expectStatus().isOk
            .expectBody(RecommendationResponse::class.java).isEqualTo(updatedResponse)
    }


    @Test
    fun `testDeleteRecommendation_ValidId_ReturnsNoContent`() {
        coEvery { recommendationService.deleteRecommendation(professionalId, recommendationId) } returns true

        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .delete().uri("/api/v1/professionals/me/recommendations/${recommendationId}")
            .exchange()
            .expectStatus().isNoContent // Or .isOk() if service returns the deleted object
    }
    
    @Test
    fun `testDeleteRecommendation_NotFound_ReturnsNotFound`() {
        coEvery { recommendationService.deleteRecommendation(professionalId, recommendationId) } returns false // Service indicates not found or not owned

        webTestClient
            .mutateWith(mockUser().principal(mockProfessionalPrincipal))
            .delete().uri("/api/v1/professionals/me/recommendations/${recommendationId}")
            .exchange()
            .expectStatus().isNotFound
    }
}
