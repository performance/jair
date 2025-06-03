package com.hairhealth.platform.service

import com.hairhealth.platform.domain.Intervention
import com.hairhealth.platform.domain.InterventionApplication
import com.hairhealth.platform.domain.InterventionType
import com.hairhealth.platform.repository.InterventionRepository
import com.hairhealth.platform.repository.InterventionApplicationRepository
import com.hairhealth.platform.service.dto.CreateInterventionRequest
import com.hairhealth.platform.service.dto.LogApplicationRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class InterventionServiceTests {

    private lateinit var interventionRepository: InterventionRepository
    private lateinit var interventionApplicationRepository: InterventionApplicationRepository
    private lateinit var interventionService: InterventionService

    private val userId = UUID.randomUUID()
    private val interventionId = UUID.randomUUID()
    private val applicationId = UUID.randomUUID()

    private val createInterventionRequest = CreateInterventionRequest(
        type = "TOPICAL",
        productName = "Minoxidil 5%",
        dosageAmount = "1ml",
        frequency = "Twice daily",
        applicationTime = "Morning and Evening",
        startDate = LocalDate.now(),
        endDate = null,
        provider = null,
        notes = "Apply to scalp",
        sourceRecommendationId = null
    )

    private val mockIntervention = Intervention(
        id = interventionId,
        userId = userId,
        type = InterventionType.TOPICAL,
        productName = "Minoxidil 5%",
        dosageAmount = "1ml",
        frequency = "Twice daily",
        applicationTime = "Morning and Evening",
        startDate = LocalDate.now(),
        endDate = null,
        isActive = true,
        provider = null,
        notes = "Apply to scalp",
        sourceRecommendationId = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private val logApplicationRequest = LogApplicationRequest(
        timestamp = Instant.now(),
        notes = "Applied as usual"
    )

    private val mockApplication = InterventionApplication(
        id = applicationId,
        interventionId = interventionId,
        userId = userId,
        timestamp = logApplicationRequest.timestamp!!,
        notes = logApplicationRequest.notes,
        createdAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        interventionRepository = mockk()
        interventionApplicationRepository = mockk()
        // As with other service tests, this assumes the service was refactored
        // to use DTOs as per the subtask plan.
        interventionService = InterventionService(interventionRepository, interventionApplicationRepository)
    }

    @Test
    fun `testCreateIntervention_Success`() = runBlocking {
        coEvery { interventionRepository.create(any()) } answers { firstArg<Intervention>().copy(id = interventionId) }

        val result = interventionService.createIntervention(userId, createInterventionRequest)

        assertNotNull(result)
        assertEquals(interventionId, result.id)
        assertEquals(createInterventionRequest.productName, result.productName)
    }

    @Test
    fun `testCreateIntervention_InvalidType_ThrowsIllegalArgumentException`() = runBlocking {
        val badRequest = createInterventionRequest.copy(type = "INVALID_TYPE_FOO")
        assertThrows<IllegalArgumentException> {
            interventionService.createIntervention(userId, badRequest)
        }
    }


    @Test
    fun `testGetInterventionById_UserOwns_Success`() = runBlocking {
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns mockIntervention
        val result = interventionService.getInterventionById(userId, interventionId)
        assertNotNull(result)
        assertEquals(interventionId, result!!.id)
    }

    @Test
    fun `testGetInterventionById_NotOwnedOrNonExistent_ReturnsNull`() = runBlocking {
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns null
        val result = interventionService.getInterventionById(userId, interventionId)
        assertNull(result)
    }

    @Test
    fun `testGetInterventionsForUser_Success`() = runBlocking {
        coEvery { interventionRepository.findByUserId(userId, false) } returns listOf(mockIntervention)
        val results = interventionService.getInterventionsForUser(userId, false)
        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
    }

    @Test
    fun `testGetInterventionsForUser_IncludeInactive_Success`() = runBlocking {
        val inactiveIntervention = mockIntervention.copy(isActive = false)
        coEvery { interventionRepository.findByUserId(userId, true) } returns listOf(mockIntervention, inactiveIntervention)
        val results = interventionService.getInterventionsForUser(userId, true)
        assertEquals(2, results.size)
    }


    @Test
    fun `testLogInterventionApplication_Success`() = runBlocking {
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns mockIntervention
        coEvery { interventionApplicationRepository.create(any()) } answers { firstArg<InterventionApplication>().copy(id = applicationId) }

        val result = interventionService.logInterventionApplication(userId, interventionId, logApplicationRequest)

        assertNotNull(result)
        assertEquals(applicationId, result.id)
        assertEquals(logApplicationRequest.notes, result.notes)
    }

    @Test
    fun `testLogInterventionApplication_InterventionNotFound_ThrowsInterventionNotFoundException`() = runBlocking {
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns null

        assertThrows<InterventionNotFoundException> {
            interventionService.logInterventionApplication(userId, interventionId, logApplicationRequest)
        }
    }

    @Test
    fun `testLogInterventionApplication_InterventionNotActive_ThrowsInterventionInteractionException`() = runBlocking {
        val inactiveIntervention = mockIntervention.copy(isActive = false)
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns inactiveIntervention

        assertThrows<InterventionInteractionException> {
            interventionService.logInterventionApplication(userId, interventionId, logApplicationRequest)
        }
    }


    @Test
    fun `testGetApplicationsForIntervention_Success`() = runBlocking {
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns mockIntervention // Verify ownership
        coEvery { interventionApplicationRepository.findByInterventionId(interventionId, 50, 0) } returns listOf(mockApplication)

        val results = interventionService.getApplicationsForIntervention(userId, interventionId, 50, 0)

        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
        assertEquals(applicationId, results[0].id)
    }

    @Test
    fun `testGetApplicationsForIntervention_InterventionNotFoundOrNotOwned_ThrowsInterventionNotFoundException`() = runBlocking {
        coEvery { interventionRepository.findByIdAndUserId(interventionId, userId) } returns null // Intervention not found for user

        assertThrows<InterventionNotFoundException> {
            interventionService.getApplicationsForIntervention(userId, interventionId, 50, 0)
        }
    }
}
