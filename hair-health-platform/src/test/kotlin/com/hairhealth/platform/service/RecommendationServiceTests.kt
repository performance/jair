package com.hairhealth.platform.service

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.RecommendationRepository
import com.hairhealth.platform.repository.UserRepository
import com.hairhealth.platform.service.dto.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID

class RecommendationServiceTests {

    private lateinit var recommendationRepository: RecommendationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var auditLogService: AuditLogService // Mock for audit logging
    private lateinit var recommendationService: RecommendationService

    private val professionalId = UUID.randomUUID()
    private val patientUserId = UUID.randomUUID()
    private val recommendationId = UUID.randomUUID()

    private val mockUser = User(patientUserId, "patient@example.com", "patient", "hash", true, Instant.now(), Instant.now(), true)
    private val createRecRequest = CreateRecommendationRequest(
        userId = patientUserId,
        consultationId = "consult-123",
        title = "Test Rec",
        description = "Test Description",
        type = "TREATMENT_ADJUSTMENT",
        details = mapOf("dosage" to "1ml")
    )
    private val mockRecommendation = Recommendation(
        id = recommendationId,
        professionalId = professionalId,
        userId = patientUserId,
        consultationId = "consult-123",
        title = "Test Rec",
        description = "Test Description",
        type = RecommendationType.TREATMENT_ADJUSTMENT,
        details = """{"dosage":"1ml"}""",
        status = RecommendationStatus.ACTIVE,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    @BeforeEach
    fun setUp() {
        recommendationRepository = mockk()
        userRepository = mockk()
        auditLogService = mockk(relaxed = true) // Relaxed mock for audit service
        recommendationService = RecommendationService(recommendationRepository, userRepository, auditLogService)

        // Common stubs
        coEvery { userRepository.findById(patientUserId) } returns mockUser
    }

    @Test
    fun `testCreateRecommendation_Success`() = runBlocking {
        coEvery { recommendationRepository.save(any()) } returns mockRecommendation

        val result = recommendationService.createRecommendation(professionalId, createRecRequest)

        assertNotNull(result)
        assertEquals(recommendationId, result.id)
        assertEquals("TREATMENT_ADJUSTMENT", result.type)
    }

    @Test
    fun `testCreateRecommendation_PatientNotFound_ThrowsUserNotFoundException`() = runBlocking {
        coEvery { userRepository.findById(patientUserId) } returns null

        assertThrows<UserNotFoundException> {
            recommendationService.createRecommendation(professionalId, createRecRequest)
        }
    }
    
    @Test
    fun `testCreateRecommendation_InvalidTypeInRequest_ThrowsIllegalArgumentException`() = runBlocking {
         val badRequest = createRecRequest.copy(type = "INVALID_TYPE_FOO")
         assertThrows<IllegalArgumentException> {
            recommendationService.createRecommendation(professionalId, badRequest)
         }
    }


    @Test
    fun `testGetRecommendationById_ProfessionalOwns_Success`() = runBlocking {
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns mockRecommendation

        val result = recommendationService.getRecommendationById(professionalId, recommendationId)

        assertNotNull(result)
        assertEquals(mockRecommendation.id, result!!.id)
    }
    
    @Test
    fun `testGetRecommendationById_NotOwnedOrNonExistent_ReturnsNull`() = runBlocking {
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns null
        val result = recommendationService.getRecommendationById(professionalId, recommendationId)
        assertNull(result)
    }
    
    @Test
    fun `testGetRecommendationById_IsDeleted_ReturnsNull`() = runBlocking {
        val deletedRecommendation = mockRecommendation.copy(status = RecommendationStatus.DELETED)
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns deletedRecommendation
        // The service method getRecommendationById has a .takeIf { it.status != RecommendationStatus.DELETED }
        val result = recommendationService.getRecommendationById(professionalId, recommendationId)
        assertNull(result)
    }


    @Test
    fun `testGetRecommendationsByProfessional_SpecificUser_Success`() = runBlocking {
        coEvery { recommendationRepository.findByProfessionalIdAndUserId(professionalId, patientUserId) } returns listOf(mockRecommendation)
        val results = recommendationService.getRecommendationsByProfessional(professionalId, patientUserId)
        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
    }

    @Test
    fun `testGetRecommendationsByProfessional_AllUsers_Success`() = runBlocking {
        coEvery { recommendationRepository.findByProfessionalId(professionalId) } returns listOf(mockRecommendation)
        val results = recommendationService.getRecommendationsByProfessional(professionalId, null)
        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
    }

    @Test
    fun `testUpdateRecommendation_Success`() = runBlocking {
        val updateRequest = UpdateRecommendationRequest(title = "Updated Title", status = "SUPERSEDED")
        val updatedDomainRec = mockRecommendation.copy(title = "Updated Title", status = RecommendationStatus.SUPERSEDED, updatedAt = Instant.now())
        
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns mockRecommendation
        coEvery { recommendationRepository.save(any()) } returns updatedDomainRec

        val result = recommendationService.updateRecommendation(professionalId, recommendationId, updateRequest)
        
        assertNotNull(result)
        assertEquals("Updated Title", result!!.title)
        assertEquals("SUPERSEDED", result.status)
    }

    @Test
    fun `testUpdateRecommendation_NotFoundOrNotOwned_ReturnsNull`() = runBlocking {
        val updateRequest = UpdateRecommendationRequest(title = "Updated Title")
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns null
        
        val result = recommendationService.updateRecommendation(professionalId, recommendationId, updateRequest)
        assertNull(result)
    }

    @Test
    fun `testUpdateRecommendation_ToInvalidStatus_ThrowsIllegalArgumentException`() = runBlocking {
        val updateRequest = UpdateRecommendationRequest(status = "INVALID_STATUS_FOO")
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns mockRecommendation
         // The exception comes from RecommendationStatus.valueOf
        assertThrows<IllegalArgumentException> {
             recommendationService.updateRecommendation(professionalId, recommendationId, updateRequest)
        }
    }
    
    @Test
    fun `testUpdateRecommendation_OnDeletedRecommendation_ThrowsRecommendationUpdateException`() = runBlocking {
        val deletedRec = mockRecommendation.copy(status = RecommendationStatus.DELETED)
        val updateRequest = UpdateRecommendationRequest(title = "New Title for Deleted")
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns deletedRec
        
        assertThrows<RecommendationUpdateException> {
            recommendationService.updateRecommendation(professionalId, recommendationId, updateRequest)
        }
    }
    
    @Test
    fun `testUpdateRecommendation_ReactivatingDeletedRecommendation_Success`() = runBlocking {
        val deletedRec = mockRecommendation.copy(status = RecommendationStatus.DELETED)
        val reactivateRequest = UpdateRecommendationRequest(status = "ACTIVE")
        val reactivatedDomainRec = deletedRec.copy(status = RecommendationStatus.ACTIVE, updatedAt = Instant.now())

        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns deletedRec
        coEvery { recommendationRepository.save(any()) } returns reactivatedDomainRec
        
        val result = recommendationService.updateRecommendation(professionalId, recommendationId, reactivateRequest)
        assertNotNull(result)
        assertEquals("ACTIVE", result!!.status)
    }


    @Test
    fun `testDeleteRecommendation_Success`() = runBlocking {
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns mockRecommendation
        coEvery { recommendationRepository.save(any()) } returns mockRecommendation.copy(status = RecommendationStatus.DELETED)

        val success = recommendationService.deleteRecommendation(professionalId, recommendationId)
        assertTrue(success)
    }
    
    @Test
    fun `testDeleteRecommendation_AlreadyDeleted_ReturnsTrue`() = runBlocking {
        val alreadyDeletedRec = mockRecommendation.copy(status = RecommendationStatus.DELETED)
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns alreadyDeletedRec
        // No call to save needed if already deleted
        val success = recommendationService.deleteRecommendation(professionalId, recommendationId)
        assertTrue(success)
    }


    @Test
    fun `testDeleteRecommendation_NotFoundOrNotOwned_ReturnsFalse`() = runBlocking {
        coEvery { recommendationRepository.findByProfessionalIdAndId(professionalId, recommendationId) } returns null
        val success = recommendationService.deleteRecommendation(professionalId, recommendationId)
        assertFalse(success)
    }


    // --- User Logic Tests ---
    @Test
    fun `testGetRecommendationsForUser_Success`() = runBlocking {
        coEvery { recommendationRepository.findByUserIdAndStatus(patientUserId, RecommendationStatus.ACTIVE, null) } returns listOf(mockRecommendation)
        val results = recommendationService.getRecommendationsForUser(patientUserId)
        assertFalse(results.isEmpty())
        assertEquals(1, results.size)
    }

    @Test
    fun `testProcessUserAction_Accept_Success`() = runBlocking {
        val actionRequest = RecommendationActionRequest(recommendationId, "ACCEPTED")
        coEvery { recommendationRepository.findById(recommendationId) } returns mockRecommendation
        coEvery { recommendationRepository.save(any()) } answers { firstArg() } // Returns the arg passed to save

        val result = recommendationService.processUserAction(patientUserId, actionRequest)

        assertNotNull(result)
        assertEquals("ACCEPTED", result.userAction)
    }

    @Test
    fun `testProcessUserAction_Decline_Success`() = runBlocking {
        val actionRequest = RecommendationActionRequest(recommendationId, "DECLINED", declineReason = "Not interested")
        coEvery { recommendationRepository.findById(recommendationId) } returns mockRecommendation
        coEvery { recommendationRepository.save(any()) } answers { firstArg() }

        val result = recommendationService.processUserAction(patientUserId, actionRequest)

        assertNotNull(result)
        assertEquals("DECLINED", result.userAction)
        assertTrue(result.userActionNotes?.contains("Not interested") ?: false)
    }
    
    @Test
    fun `testProcessUserAction_AcceptWithModifications_Success`() = runBlocking {
        val modifications = mapOf("dosage" to "0.5ml")
        val actionRequest = RecommendationActionRequest(recommendationId, "ACCEPTED_WITH_MODIFICATIONS", modifications = modifications)
        coEvery { recommendationRepository.findById(recommendationId) } returns mockRecommendation
        coEvery { recommendationRepository.save(any()) } answers { firstArg() }

        val result = recommendationService.processUserAction(patientUserId, actionRequest)

        assertNotNull(result)
        assertEquals("ACCEPTED_WITH_MODIFICATIONS", result.userAction)
        assertTrue(result.userActionNotes?.contains("0.5ml") ?: false)
    }


    @Test
    fun `testProcessUserAction_RecommendationNotFound_ThrowsRecommendationNotFoundException`() = runBlocking {
        val actionRequest = RecommendationActionRequest(recommendationId, "ACCEPTED")
        coEvery { recommendationRepository.findById(recommendationId) } returns null

        assertThrows<RecommendationNotFoundException> {
            recommendationService.processUserAction(patientUserId, actionRequest)
        }
    }

    @Test
    fun `testProcessUserAction_UserDoesNotOwn_ThrowsRecommendationAccessException`() = runBlocking {
        val otherUserId = UUID.randomUUID()
        val actionRequest = RecommendationActionRequest(recommendationId, "ACCEPTED")
        coEvery { recommendationRepository.findById(recommendationId) } returns mockRecommendation // Found, but for patientUserId

        assertThrows<RecommendationAccessException> {
            recommendationService.processUserAction(otherUserId, actionRequest) // Called with otherUserId
        }
    }
    
    @Test
    fun `testProcessUserAction_RecommendationNotActive_ThrowsRecommendationActionException`() = runBlocking {
        val inactiveRec = mockRecommendation.copy(status = RecommendationStatus.SUPERSEDED)
        val actionRequest = RecommendationActionRequest(recommendationId, "ACCEPTED")
        coEvery { recommendationRepository.findById(recommendationId) } returns inactiveRec

        assertThrows<RecommendationActionException> {
            recommendationService.processUserAction(patientUserId, actionRequest)
        }
    }
    
    @Test
    fun `testProcessUserAction_InvalidActionString_ThrowsIllegalArgumentException`() = runBlocking {
        val actionRequest = RecommendationActionRequest(recommendationId, "INVALID_ACTION_STRING")
        coEvery { recommendationRepository.findById(recommendationId) } returns mockRecommendation

        assertThrows<IllegalArgumentException> {
            recommendationService.processUserAction(patientUserId, actionRequest)
        }
    }
}
