package com.hairhealth.platform.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.*
import com.hairhealth.platform.service.*
import com.hairhealth.platform.controller.RequestAccessRequest
import com.hairhealth.platform.controller.RevokeMedicalSharingRequest

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureWebMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Transactional
class MedicalSharingIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var photoMetadataRepository: PhotoMetadataRepository
    
    @Autowired
    private lateinit var medicalSharingRepository: MedicalSharingRepository
    
    @Autowired
    private lateinit var medicalSharingService: CompleteMedicalSharingService
    
    private lateinit var testPatientId: UUID
    private lateinit var testProfessionalId: UUID
    private lateinit var testPhotoIds: List<UUID>
    
    @BeforeEach
    fun setup() = runBlocking {
        // Create test patient
        val patient = com.hairhealth.platform.domain.User(
            id = UUID.randomUUID(),
            email = "patient@test.com",
            username = "testpatient",
            passwordHash = "hash",
            isEmailVerified = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isActive = true
        )
        testPatientId = patient.id
        userRepository.create(patient)
        
        // Create test professional
        val professional = com.hairhealth.platform.domain.User(
            id = UUID.randomUUID(),
            email = "doctor@test.com",
            username = "testdoctor",
            passwordHash = "hash",
            isEmailVerified = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isActive = true
        )
        testProfessionalId = professional.id
        userRepository.create(professional)
        
        // Create test photos
        testPhotoIds = listOf(
            createTestPhoto(testPatientId, PhotoAngle.VERTEX),
            createTestPhoto(testPatientId, PhotoAngle.HAIRLINE),
            createTestPhoto(testPatientId, PhotoAngle.TEMPLES)
        )
    }
    
    @Test
    @WithMockUser(username = "testpatient", roles = ["USER"])
    fun `should create medical sharing session successfully`() = runBlocking {
        val request = CreateMedicalSharingRequest(
            professionalId = testProfessionalId,
            photoIds = testPhotoIds,
            notes = "Please review my progress photos",
            durationHours = 24,
            maxTotalViews = 3,
            maxViewDurationMinutes = 5
        )
        
        mockMvc.perform(
            post("/api/v1/me/medical-sharing/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.sessionId").exists())
        .andExpect(jsonPath("$.professionalId").value(testProfessionalId.toString()))
        .andExpect(jsonPath("$.photoCount").value(3))
        .andExpect(jsonPath("$.maxTotalViews").value(3))
        .andExpect(jsonPath("$.maxViewDurationMinutes").value(5))
        .andExpect(jsonPath("$.status").value("PENDING_DOCTOR_ACCESS"))
        .andExpect(jsonPath("$.secureAccessUrl").exists())
    }
    
    @Test
    @WithMockUser(username = "testpatient", roles = ["USER"])
    fun `should reject sharing session with invalid photo IDs`() = runBlocking {
        val invalidPhotoId = UUID.randomUUID()
        val request = CreateMedicalSharingRequest(
            professionalId = testProfessionalId,
            photoIds = listOf(invalidPhotoId),
            notes = "Test with invalid photo",
            durationHours = 24,
            maxTotalViews = 3,
            maxViewDurationMinutes = 5
        )
        
        mockMvc.perform(
            post("/api/v1/me/medical-sharing/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(username = "testdoctor", roles = ["PROFESSIONAL"])
    fun `should allow professional to request access to shared photos`() = runBlocking {
        // First create a sharing session
        val session = createTestMedicalSession()
        
        val accessRequest = RequestAccessRequest(
            deviceFingerprint = "test-device-123",
            userAgent = "Mozilla/5.0 Test Browser",
            ipAddress = "192.168.1.100",
            screenResolution = "1920x1080",
            timeZone = "America/New_York"
        )
        
        mockMvc.perform(
            post("/api/v1/professionals/me/medical-access/sessions/${session.id}/request-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accessRequest))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.accessSessionId").exists())
        .andExpect(jsonPath("$.secureViewerUrl").exists())
        .andExpect(jsonPath("$.maxDurationMinutes").value(5))
        .andExpect(jsonPath("$.photos").isArray)
        .andExpect(jsonPath("$.photos.length()").value(3))
        .andExpect(jsonPath("$.restrictions.allowScreenshots").value(false))
        .andExpect(jsonPath("$.restrictions.allowDownload").value(false))
    }
    
    @Test
    @WithMockUser(username = "testdoctor", roles = ["PROFESSIONAL"])
    fun `should prevent access when session is expired`() = runBlocking {
        // Create an expired session
        val expiredSession = MedicalSharingSession(
            id = UUID.randomUUID(),
            patientId = testPatientId,
            professionalId = testProfessionalId,
            photoIds = testPhotoIds,
            notes = "Test expired session",
            maxTotalViews = 3,
            maxViewDurationMinutes = 5,
            expiresAt = Instant.now().minusSeconds(3600), // Expired 1 hour ago
            status = MedicalSharingStatus.PENDING_DOCTOR_ACCESS,
            createdAt = Instant.now().minusSeconds(7200),
            updatedAt = Instant.now().minusSeconds(7200)
        )
        medicalSharingRepository.createSession(expiredSession)
        
        val accessRequest = RequestAccessRequest(
            deviceFingerprint = "test-device-123",
            userAgent = "Mozilla/5.0 Test Browser",
            ipAddress = "192.168.1.100",
            screenResolution = null, // Explicitly providing null
            timeZone = null // Explicitly providing null
        )
        
        mockMvc.perform(
            post("/api/v1/professionals/me/medical-access/sessions/${expiredSession.id}/request-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accessRequest))
        )
        .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(username = "testdoctor", roles = ["PROFESSIONAL"])
    fun `should prevent access when max views exceeded`() = runBlocking {
        // Create session with max views = 1
        val session = MedicalSharingSession(
            id = UUID.randomUUID(),
            patientId = testPatientId,
            professionalId = testProfessionalId,
            photoIds = testPhotoIds,
            notes = "Limited views test",
            maxTotalViews = 1,
            maxViewDurationMinutes = 5,
            expiresAt = Instant.now().plusSeconds(3600),
            status = MedicalSharingStatus.ACTIVE,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val createdSession = medicalSharingRepository.createSession(session)
        
        // Create existing access session to reach the limit
        val existingAccess = DoctorAccessSession(
            id = UUID.randomUUID(),
            medicalSessionId = createdSession.id,
            professionalId = testProfessionalId,
            deviceFingerprint = "previous-device",
            ipAddress = "192.168.1.99",
            userAgent = "Previous Browser",
            startedAt = Instant.now().minusSeconds(1800),
            expiresAt = Instant.now().minusSeconds(1500),
            isActive = false
        )
        
        val accessRequest = RequestAccessRequest(
            deviceFingerprint = "test-device-123",
            userAgent = "Mozilla/5.0 Test Browser",
            ipAddress = "192.168.1.100",
            screenResolution = null, // Explicitly providing null
            timeZone = null // Explicitly providing null
        )
        
        mockMvc.perform(
            post("/api/v1/professionals/me/medical-access/sessions/${createdSession.id}/request-access")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(accessRequest))
        )
        .andExpect(status().isBadRequest)
    }
    
    @Test
    @WithMockUser(username = "testpatient", roles = ["USER"])
    fun `should allow patient to revoke sharing session`() = runBlocking {
        val session = createTestMedicalSession()
        
        val revokeRequest = RevokeMedicalSharingRequest(
            reason = "Changed my mind about sharing"
        )
        
        mockMvc.perform(
            post("/api/v1/me/medical-sharing/sessions/${session.id}/revoke")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(revokeRequest))
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.sessionId").value(session.id.toString()))
        .andExpect(jsonPath("$.reason").value("Changed my mind about sharing"))
        .andExpect(jsonPath("$.revokedAt").exists())
    }
    
    @Test
    @WithMockUser(username = "testpatient", roles = ["USER"])
    fun `should list patient medical sharing sessions with summaries`() = runBlocking {
        // Create multiple sessions
        val session1 = createTestMedicalSession()
        val session2 = createTestMedicalSession()
        
        mockMvc.perform(
            get("/api/v1/me/medical-sharing/sessions")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].sessionId").exists())
        .andExpect(jsonPath("$[0].professionalId").value(testProfessionalId.toString()))
        .andExpect(jsonPath("$[0].photoCount").value(3))
        .andExpect(jsonPath("$[0].status").exists())
        .andExpect(jsonPath("$[0].remainingViews").exists())
        .andExpect(jsonPath("$[0].hoursUntilExpiry").exists())
    }
    
    @Test
    @WithMockUser(username = "testdoctor", roles = ["PROFESSIONAL"])
    fun `should list accessible sessions for professional`() = runBlocking {
        val session = createTestMedicalSession()
        
        mockMvc.perform(
            get("/api/v1/professionals/me/medical-access/sessions")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$").isArray)
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].sessionId").value(session.id.toString()))
        .andExpect(jsonPath("$[0].canAccess").value(true))
        .andExpect(jsonPath("$[0].remainingViews").value(3))
        .andExpect(jsonPath("$[0].urgency").exists())
        .andExpect(jsonPath("$[0].patientInfo").exists())
        .andExpect(jsonPath("$[0].patientInfo.patientInitials").exists())
    }
    
    @Test
    fun `should complete full photo viewing workflow`() = runBlocking {
        // Create session
        val session = createTestMedicalSession()
        
        // Professional requests access
        val deviceInfo = DeviceInfo(
            fingerprint = "test-device-456",
            ipAddress = "192.168.1.101",
            userAgent = "Mozilla/5.0 Test Browser"
        )
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.id,
            professionalId = testProfessionalId,
            deviceInfo = deviceInfo
        )
        
        // Start viewing a photo
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = testPhotoIds[0],
            deviceInfo = deviceInfo
        )
        
        // Verify viewing session created
        assert(viewingResult.viewingEventId != null)
        assert(viewingResult.decryptionToken != null)
        assert(viewingResult.maxViewTimeSeconds == 300) // 5 minutes
        
        // End viewing session
        val endResult = medicalSharingService.endPhotoViewing(
            viewingEventId = viewingResult.viewingEventId,
            endReason = ViewingEndReason.USER_CLOSED,
            durationSeconds = 120 // 2 minutes
        )
        
        assert(endResult.success)
        assert(endResult.finalDurationSeconds == 120L)
    }
    
    @Test
    fun `should detect and handle suspicious activity`() = runBlocking {
        val session = createTestMedicalSession()
        
        val deviceInfo = DeviceInfo(
            fingerprint = "suspicious-device",
            ipAddress = "192.168.1.200",
            userAgent = "Suspicious Browser"
        )
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.id,
            professionalId = testProfessionalId,
            deviceInfo = deviceInfo
        )
        
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = testPhotoIds[0],
            deviceInfo = deviceInfo
        )
        
        // Report suspicious activity
        val suspiciousResult = medicalSharingService.recordSuspiciousActivity(
            viewingEventId = viewingResult.viewingEventId,
            activityType = SuspiciousActivityType.SCREENSHOT_ATTEMPT,
            details = "User attempted to take screenshot"
        )
        
        assert(suspiciousResult.recorded)
        assert(suspiciousResult.activityType == SuspiciousActivityType.SCREENSHOT_ATTEMPT)
        assert(!suspiciousResult.autoRevokeTriggered) // Single screenshot attempt shouldn't auto-revoke
        
        // Report multiple screenshot attempts (should trigger auto-revoke)
        val multipleScreenshotResult = medicalSharingService.recordSuspiciousActivity(
            viewingEventId = viewingResult.viewingEventId,
            activityType = SuspiciousActivityType.MULTIPLE_SCREENSHOT_ATTEMPTS,
            details = "Multiple screenshot attempts detected"
        )
        
        assert(multipleScreenshotResult.recorded)
        assert(multipleScreenshotResult.autoRevokeTriggered)
    }
    
    @Test
    fun `should enforce viewing time limits`() = runBlocking {
        val session = createTestMedicalSession()
        
        val deviceInfo = DeviceInfo(
            fingerprint = "time-test-device",
            ipAddress = "192.168.1.150",
            userAgent = "Time Test Browser"
        )
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.id,
            professionalId = testProfessionalId,
            deviceInfo = deviceInfo
        )
        
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = testPhotoIds[0],
            deviceInfo = deviceInfo
        )
        
        // Verify time limits are set correctly
        assert(viewingResult.maxViewTimeSeconds == 300) // 5 minutes
        assert(viewingResult.restrictions.maxViewTimeSeconds == 300)
        assert(viewingResult.restrictions.preventScreenshots)
        assert(viewingResult.restrictions.preventDownload)
        assert(viewingResult.restrictions.requireContinuousAuth)
    }
    
    private suspend fun createTestPhoto(userId: UUID, angle: PhotoAngle): UUID {
        val photo = com.hairhealth.platform.domain.PhotoMetadata(
            id = UUID.randomUUID(),
            userId = userId,
            filename = "test_${angle.name.lowercase()}.jpg.enc",
            angle = angle,
            captureDate = Instant.now(),
            fileSize = 1024L,
            encryptionKeyInfo = "test_key_info",
            blobPath = "test/path/${angle.name.lowercase()}.jpg",
            uploadedAt = Instant.now(),
            isDeleted = false
        )
        photoMetadataRepository.create(photo)
        return photo.id
    }
    
    private suspend fun createTestMedicalSession(): MedicalSharingSession {
        val session = MedicalSharingSession(
            id = UUID.randomUUID(),
            patientId = testPatientId,
            professionalId = testProfessionalId,
            photoIds = testPhotoIds,
            notes = "Test medical sharing session",
            maxTotalViews = 3,
            maxViewDurationMinutes = 5,
            expiresAt = Instant.now().plusSeconds(24 * 3600), // 24 hours
            status = MedicalSharingStatus.PENDING_DOCTOR_ACCESS,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return medicalSharingRepository.createSession(session)
    }
}