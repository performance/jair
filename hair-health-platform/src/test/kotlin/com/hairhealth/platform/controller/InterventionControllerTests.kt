package com.hairhealth.platform.controller

import com.hairhealth.platform.config.SecurityConfig
import com.hairhealth.platform.security.JwtService
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.InterventionService
import com.hairhealth.platform.service.InterventionNotFoundException
import com.hairhealth.platform.service.dto.CreateInterventionRequest
import com.hairhealth.platform.service.dto.InterventionApplicationResponse
import com.hairhealth.platform.service.dto.InterventionResponse
import com.hairhealth.platform.service.dto.LogApplicationRequest
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
import java.time.LocalDate
import java.util.UUID

@WebFluxTest(InterventionController::class)
@Import(SecurityConfig::class)
class InterventionControllerTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var interventionService: InterventionService

    @MockBean
    private lateinit var jwtService: JwtService // For SecurityConfig context

    private val userId = UUID.randomUUID()
    private val interventionId = UUID.randomUUID()
    private val applicationId = UUID.randomUUID()
    private val mockUserPrincipal = UserPrincipal(userId, "user@example.com", "testuser", listOf("USER"))

    private val createInterventionRequest = CreateInterventionRequest(
        type = "TOPICAL", productName = "Minoxidil", dosageAmount = "1ml", frequency = "Daily",
        applicationTime = "Evening", startDate = LocalDate.now(), endDate = null, provider = null,
        notes = "Apply to scalp", sourceRecommendationId = null
    )

    private val mockInterventionResponse = InterventionResponse(
        id = interventionId, userId = userId, type = "TOPICAL", productName = "Minoxidil",
        dosageAmount = "1ml", frequency = "Daily", applicationTime = "Evening",
        startDate = LocalDate.now(), endDate = null, isActive = true, provider = null,
        notes = "Apply to scalp", sourceRecommendationId = null,
        createdAt = Instant.now(), updatedAt = Instant.now()
    )

    private val logApplicationRequest = LogApplicationRequest(timestamp = Instant.now(), notes = "Applied")

    private val mockApplicationResponse = InterventionApplicationResponse(
        id = applicationId, interventionId = interventionId, userId = userId,
        timestamp = logApplicationRequest.timestamp!!, notes = logApplicationRequest.notes, createdAt = Instant.now()
    )

    @Test
    fun `testCreateIntervention_ValidRequest_ReturnsCreated`() {
        coEvery { interventionService.createIntervention(userId, createInterventionRequest) } returns mockInterventionResponse

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/interventions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(createInterventionRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(InterventionResponse::class.java).isEqualTo(mockInterventionResponse)
    }

    @Test
    fun `testCreateIntervention_InvalidTypeInRequest_ReturnsBadRequest`() {
        val badRequest = createInterventionRequest.copy(type="INVALID_TYPE_FOO")
        coEvery { interventionService.createIntervention(userId, badRequest) } throws IllegalArgumentException("Invalid intervention type")

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/interventions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(badRequest))
            .exchange()
            .expectStatus().isBadRequest // Assuming controller advice or service maps this to 400
    }


    @Test
    fun `testGetInterventions_ReturnsOk`() {
        coEvery { interventionService.getInterventionsForUser(userId, false) } returns listOf(mockInterventionResponse)

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/interventions?includeInactive=false")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(InterventionResponse::class.java).hasSize(1)
    }

    @Test
    fun `testGetInterventionById_ValidId_ReturnsOk`() {
        coEvery { interventionService.getInterventionById(userId, interventionId) } returns mockInterventionResponse

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/interventions/${interventionId}")
            .exchange()
            .expectStatus().isOk
            .expectBody(InterventionResponse::class.java).isEqualTo(mockInterventionResponse)
    }

    @Test
    fun `testGetInterventionById_NotFound_ReturnsNotFound`() {
        coEvery { interventionService.getInterventionById(userId, interventionId) } returns null

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/interventions/${interventionId}")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `testLogApplication_ValidRequest_ReturnsCreated`() {
        coEvery { interventionService.logInterventionApplication(userId, interventionId, logApplicationRequest) } returns mockApplicationResponse

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/interventions/${interventionId}/log-application")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(logApplicationRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(InterventionApplicationResponse::class.java).isEqualTo(mockApplicationResponse)
    }

    @Test
    fun `testLogApplication_InterventionNotFound_ReturnsNotFound`() {
        coEvery { interventionService.logInterventionApplication(userId, interventionId, logApplicationRequest) } throws InterventionNotFoundException("Intervention not found")

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/interventions/${interventionId}/log-application")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(logApplicationRequest))
            .exchange()
            .expectStatus().isNotFound
    }


    @Test
    fun `testGetApplications_ReturnsOk`() {
        coEvery { interventionService.getApplicationsForIntervention(userId, interventionId, 50, 0) } returns listOf(mockApplicationResponse)

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/interventions/${interventionId}/applications?limit=50&offset=0")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(InterventionApplicationResponse::class.java).hasSize(1)
    }

    @Test
    fun `testGetApplications_InterventionNotFound_ReturnsNotFound`() {
        coEvery { interventionService.getApplicationsForIntervention(userId, interventionId, 50, 0) } throws InterventionNotFoundException("Intervention not found")

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/interventions/${interventionId}/applications?limit=50&offset=0")
            .exchange()
            .expectStatus().isNotFound
    }
}
