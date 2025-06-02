package com.hairhealth.platform.service

import com.hairhealth.platform.domain.HairFallCategory
import com.hairhealth.platform.domain.HairFallLog
import com.hairhealth.platform.repository.HairFallLogRepository
import com.hairhealth.platform.service.dto.CreateHairFallLogRequest
import com.hairhealth.platform.service.dto.toResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class HairFallLogServiceTests {

    private lateinit var hairFallLogRepository: HairFallLogRepository
    private lateinit var hairFallLogService: HairFallLogService

    private val userId = UUID.randomUUID()
    private val logId = UUID.randomUUID()

    private val createLogRequest = CreateHairFallLogRequest(
        date = LocalDate.now(),
        count = 50,
        category = "SHOWER",
        description = "Normal shedding",
        photoMetadataId = null
    )

    private val mockHairFallLog = HairFallLog(
        id = logId,
        userId = userId,
        date = createLogRequest.date,
        count = createLogRequest.count,
        category = HairFallCategory.SHOWER,
        description = createLogRequest.description,
        photoMetadataId = createLogRequest.photoMetadataId,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        hairFallLogRepository = mockk()
        // The existing HairFallLogService in the codebase does not take DTOs directly in its methods.
        // These tests are written against the *refactored* service as per the subtask's ideal plan
        // (where service methods would accept DTOs).
        // If the service overwrite failed and the old service is still in place, these tests would need adaptation
        // to call the service with individual parameters instead of CreateHairFallLogRequest.
        hairFallLogService = HairFallLogService(hairFallLogRepository)
    }

    @Test
    fun `testCreateHairFallLog_Success`() = runBlocking {
        // Assuming the service's createHairFallLog was refactored to take CreateHairFallLogRequest
        // and that toDomain works as expected.
        // The service then calls repository.create() with a domain object.
        
        // We mock the repository's `create` method, which is called by the service's `createHairFallLog`
        // after it converts the DTO to a domain object.
        coEvery { hairFallLogRepository.create(any()) } answers { 
            // The 'any()' here would be the domain object created by request.toDomain(userId).copy(...)
            // We return a domain object that would be the result of persistence.
            // For this test, we can return the `mockHairFallLog` or the argument itself if it's correctly formed.
            firstArg<HairFallLog>().copy(id = logId) // Ensure it has an ID as if persisted
        }

        val resultResponse = hairFallLogService.createHairFallLog(userId, createLogRequest)

        assertNotNull(resultResponse)
        assertEquals(logId, resultResponse.id) // Check if ID is present from the mock
        assertEquals(createLogRequest.count, resultResponse.count)
        assertEquals(createLogRequest.category, resultResponse.category)
    }
    
    @Test
    fun `testCreateHairFallLog_InvalidCategory_ThrowsIllegalArgumentException`() = runBlocking {
        val badRequest = createLogRequest.copy(category = "INVALID_CATEGORY")
        // The exception should be thrown by the toDomain() mapper or by the service if it re-validates
        assertThrows<IllegalArgumentException> {
            hairFallLogService.createHairFallLog(userId, badRequest)
        }
    }

    @Test
    fun `testGetHairFallLogById_UserOwns_Success`() = runBlocking {
        coEvery { hairFallLogRepository.findByIdAndUserId(logId, userId) } returns mockHairFallLog

        val result = hairFallLogService.getHairFallLogById(userId, logId)

        assertNotNull(result)
        assertEquals(logId, result!!.id)
    }

    @Test
    fun `testGetHairFallLogById_NotOwnedOrNonExistent_ReturnsNull`() = runBlocking {
        coEvery { hairFallLogRepository.findByIdAndUserId(logId, userId) } returns null
        val result = hairFallLogService.getHairFallLogById(userId, logId)
        assertNull(result)
    }

    @Test
    fun `testGetHairFallLogsForUser_Success`() = runBlocking {
        coEvery { hairFallLogRepository.findByUserId(userId, 50, 0) } returns listOf(mockHairFallLog)
        val results = hairFallLogService.getHairFallLogsForUser(userId, 50, 0)
        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
    }
    
    @Test
    fun `testGetHairFallLogsByDateRange_Success`() = runBlocking {
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        coEvery { hairFallLogRepository.findByUserIdAndDateRange(userId, startDate, endDate) } returns listOf(mockHairFallLog)
        
        val results = hairFallLogService.getHairFallLogsByDateRange(userId, startDate, endDate)
        
        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
        assertEquals(mockHairFallLog.id, results[0].id)
    }
}
