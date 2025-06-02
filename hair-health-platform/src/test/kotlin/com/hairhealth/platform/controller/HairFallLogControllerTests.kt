package com.hairhealth.platform.controller

import com.hairhealth.platform.config.SecurityConfig
import com.hairhealth.platform.security.JwtService
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.HairFallLogService
import com.hairhealth.platform.service.dto.CreateHairFallLogRequest
import com.hairhealth.platform.service.dto.HairFallLogResponse
import io.mockk.coEvery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebFluxTest(HairFallLogController::class)
@Import(SecurityConfig::class)
class HairFallLogControllerTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var hairFallLogService: HairFallLogService

    @MockBean
    private lateinit var jwtService: JwtService // For SecurityConfig context

    private val userId = UUID.randomUUID()
    private val logId = UUID.randomUUID()
    private val mockUserPrincipal = UserPrincipal(userId, "user@example.com", "testuser", listOf("USER"))

    private val createLogRequestDTO = CreateHairFallLogRequest(
        date = LocalDate.now(),
        count = 50,
        category = "SHOWER", // String as per DTO
        description = "Normal shedding",
        photoMetadataId = null
    )

    private val mockLogResponseDTO = HairFallLogResponse(
        id = logId,
        userId = userId,
        date = createLogRequestDTO.date,
        count = createLogRequestDTO.count,
        category = createLogRequestDTO.category, // String as per DTO
        description = createLogRequestDTO.description,
        photoMetadataId = createLogRequestDTO.photoMetadataId,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @Test
    fun `testCreateHairFallLog_ValidRequest_ReturnsCreated`() {
        // This test assumes HairFallLogController's createHairFallLog method
        // correctly handles the DTO and calls the service.
        // The service (even the old one) takes individual params.
        // The controller test should verify the controller correctly unpacks the DTO for the service.
        
        // Mocking the service's createHairFallLog method (the one that takes individual params)
        // The controller's responsibility is to call this correctly.
        // The actual response from service is domain object, controller maps it to DTO.
        coEvery { 
            hairFallLogService.createHairFallLog(
                userId = eq(userId), 
                date = eq(createLogRequestDTO.date),
                count = eq(createLogRequestDTO.count),
                category = eq(com.hairhealth.platform.domain.HairFallCategory.SHOWER), // Service expects Enum
                description = eq(createLogRequestDTO.description),
                photoMetadataId = eq(createLogRequestDTO.photoMetadataId)
            ) 
        } returns com.hairhealth.platform.domain.HairFallLog( // service returns domain object
            id = logId, userId = userId, date = createLogRequestDTO.date, count = createLogRequestDTO.count,
            category = com.hairhealth.platform.domain.HairFallCategory.SHOWER, 
            description = createLogRequestDTO.description, photoMetadataId = null,
            createdAt = mockLogResponseDTO.createdAt, updatedAt = mockLogResponseDTO.updatedAt
        )


        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .post().uri("/api/v1/me/hair-fall-logs")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(createLogRequestDTO))
            .exchange()
            .expectStatus().isCreated
            .expectBody(HairFallLogResponse::class.java)
            .value { response ->
                // Compare essential fields that should match the DTO response after mapping
                assertEquals(logId, response.id)
                assertEquals(userId, response.userId)
                assertEquals(createLogRequestDTO.category, response.category)
            }
    }

    @Test
    fun `testGetHairFallLogs_ReturnsOk`() {
        coEvery { hairFallLogService.getHairFallLogsByUserId(userId, 50, 0) } returns listOf(
            // Service returns domain list
            com.hairhealth.platform.domain.HairFallLog(
                id = logId, userId = userId, date = LocalDate.now(), count = 50,
                category = com.hairhealth.platform.domain.HairFallCategory.SHOWER, description = "test", photoMetadataId = null,
                createdAt = mockLogResponseDTO.createdAt, updatedAt = mockLogResponseDTO.updatedAt
            )
        )

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/hair-fall-logs?limit=50&offset=0")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(HairFallLogResponse::class.java).hasSize(1)
            .value { responses ->
                 assertEquals(logId, responses[0].id)
            }
    }

    @Test
    fun `testGetHairFallLogById_ValidIdAndOwner_ReturnsOk`() {
        // Service's getHairFallLogById takes only ID, controller must verify ownership
        coEvery { hairFallLogService.getHairFallLogById(logId) } returns com.hairhealth.platform.domain.HairFallLog(
            id = logId, userId = userId, // CRITICAL: ensure this userId matches principal for test pass
            date = LocalDate.now(), count = 50, category = com.hairhealth.platform.domain.HairFallCategory.SHOWER, 
            description = "test", photoMetadataId = null, 
            createdAt = mockLogResponseDTO.createdAt, updatedAt = mockLogResponseDTO.updatedAt
        )

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/hair-fall-logs/${logId}")
            .exchange()
            .expectStatus().isOk
            .expectBody(HairFallLogResponse::class.java)
            .value { response ->
                assertEquals(logId, response.id)
                assertEquals(userId, response.userId)
            }
    }

    @Test
    fun `testGetHairFallLogById_NotOwner_ReturnsForbidden`() {
        val otherUserId = UUID.randomUUID()
        // Service returns log, but it belongs to another user
        coEvery { hairFallLogService.getHairFallLogById(logId) } returns com.hairhealth.platform.domain.HairFallLog(
            id = logId, userId = otherUserId, // Belongs to otherUser
            date = LocalDate.now(), count = 50, category = com.hairhealth.platform.domain.HairFallCategory.SHOWER, 
            description = "test", photoMetadataId = null, 
            createdAt = mockLogResponseDTO.createdAt, updatedAt = mockLogResponseDTO.updatedAt
        )
        
        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal)) // Authenticated as 'userId'
            .get().uri("/api/v1/me/hair-fall-logs/${logId}")
            .exchange()
            .expectStatus().isForbidden // Due to controller's ownership check
    }
    
    @Test
    fun `testGetHairFallLogById_NotFound_ReturnsNotFound`() {
        coEvery { hairFallLogService.getHairFallLogById(logId) } returns null // Log not found by service

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/hair-fall-logs/${logId}")
            .exchange()
            .expectStatus().isNotFound
    }
    
    @Test
    fun `testGetHairFallLogsByDateRange_ReturnsOk`() {
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        coEvery { hairFallLogService.getHairFallLogsByDateRange(userId, startDate, endDate) } returns listOf(
            com.hairhealth.platform.domain.HairFallLog(
                id = logId, userId = userId, date = startDate, count = 50,
                category = com.hairhealth.platform.domain.HairFallCategory.SHOWER, description = "test", photoMetadataId = null,
                createdAt = mockLogResponseDTO.createdAt, updatedAt = mockLogResponseDTO.updatedAt
            )
        )

        webTestClient
            .mutateWith(mockUser().principal(mockUserPrincipal))
            .get().uri("/api/v1/me/hair-fall-logs/date-range?startDate=${startDate}&endDate=${endDate}")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(HairFallLogResponse::class.java).hasSize(1)
    }
}
