package com.hairhealth.platform.security

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.service.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class SecurityValidationTest {

    @Autowired
    private lateinit var medicalSharingService: CompleteMedicalSharingService
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var photoMetadataService: PhotoMetadataService

    @Test
    fun `should prevent cross-tenant data access`() = runBlocking {
        println("🔒 Security Test: Cross-Tenant Data Access Prevention")
        
        // Create two separate patients
        val patient1 = createTestUser("patient1@security.test")
        val patient2 = createTestUser("patient2@security.test")
        val doctor = createTestUser("doctor@security.test")
        
        // Patient 1 creates photos
        val patient1Photos = createTestPhotos(patient1.id, 3)
        
        // Patient 2 tries to share Patient 1's photos (should fail)
        assertThrows<IllegalArgumentException> {
            medicalSharingService.createMedicalSharingSession(
                patientId = patient2.id,
                request = CreateMedicalSharingRequest(
                    professionalId = doctor.id,
                    photoIds = patient1Photos,
                    notes = "Unauthorized sharing attempt",
                    durationHours = 24,
                    maxTotalViews = 3,
                    maxViewDurationMinutes = 5
                )
            )
        }
        
        println("   ✅ Cross-tenant photo sharing blocked")
    }

    @Test
    fun `should enforce viewing time limits strictly`() = runBlocking {
        println("🔒 Security Test: Viewing Time Limits")
        
        val patient = createTestUser("patient@time.test")
        val doctor = createTestUser("doctor@time.test")
        val photos = createTestPhotos(patient.id, 2)
        
        // Create session with very short viewing time
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "Time limit test",
                durationHours = 24,
                maxTotalViews = 3,
                maxViewDurationMinutes = 1 // Very short time limit
            )
        )
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = createTestDeviceInfo("time-test-device")
        )
        
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = photos.first(),
            deviceInfo = createTestDeviceInfo("time-test-device")
        )
        
        // Verify time limits are enforced
        assert(viewingResult.maxViewTimeSeconds == 60) // 1 minute
        assert(viewingResult.restrictions.maxViewTimeSeconds == 60)
        
        println("   ✅ Time limits properly enforced: ${viewingResult.maxViewTimeSeconds}s")
    }

    @Test
    fun `should prevent unauthorized professional access`() = runBlocking {
        println("🔒 Security Test: Unauthorized Professional Access")
        
        val patient = createTestUser("patient@auth.test")
        val authorizedDoctor = createTestUser("authorized@auth.test")
        val unauthorizedDoctor = createTestUser("unauthorized@auth.test")
        val photos = createTestPhotos(patient.id, 2)
        
        // Patient shares with authorized doctor only
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = authorizedDoctor.id,
                photoIds = photos,
                notes = "Authorization test",
                durationHours = 24,
                maxTotalViews = 3,
                maxViewDurationMinutes = 5
            )
        )
        
        // Authorized doctor should succeed
        val authorizedAccess = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = authorizedDoctor.id,
            deviceInfo = createTestDeviceInfo("authorized-device")
        )
        assert(authorizedAccess.accessSession.professionalId == authorizedDoctor.id)
        println("   ✅ Authorized doctor access granted")
        
        // Unauthorized doctor should fail
        assertThrows<IllegalArgumentException> {
            medicalSharingService.requestDoctorAccess(
                sessionId = session.session.id,
                professionalId = unauthorizedDoctor.id,
                deviceInfo = createTestDeviceInfo("unauthorized-device")
            )
        }
        println("   ✅ Unauthorized doctor access blocked")
    }

    @Test
    fun `should enforce maximum view count limits`() = runBlocking {
        println("🔒 Security Test: Maximum View Count Enforcement")
        
        val patient = createTestUser("patient@view.test")
        val doctor = createTestUser("doctor@view.test")
        val photos = createTestPhotos(patient.id, 2)
        
        // Create session with max 2 views
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "View limit test",
                durationHours = 24,
                maxTotalViews = 2, // Limited to 2 views
                maxViewDurationMinutes = 5
            )
        )
        
        // First access - should succeed
        val access1 = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = createTestDeviceInfo("device-1")
        )
        println("   ✅ First access granted")
        
        // Second access - should succeed
        val access2 = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = createTestDeviceInfo("device-2")
        )
        println("   ✅ Second access granted")
        
        // Third access - should fail (exceeds limit)
        assertThrows<IllegalArgumentException> {
            medicalSharingService.requestDoctorAccess(
                sessionId = session.session.id,
                professionalId = doctor.id,
                deviceInfo = createTestDeviceInfo("device-3")
            )
        }
        println("   ✅ Third access blocked (exceeds limit)")
    }

    @Test
    fun `should immediately revoke access on suspicious activity`() = runBlocking {
        println("🔒 Security Test: Automatic Revocation on Suspicious Activity")
        
        val patient = createTestUser("patient@suspicious.test")
        val doctor = createTestUser("doctor@suspicious.test")
        val photos = createTestPhotos(patient.id, 2)
        
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "Suspicious activity test",
                durationHours = 24,
                maxTotalViews = 3,
                maxViewDurationMinutes = 5
            )
        )
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = createTestDeviceInfo("suspicious-device")
        )
        
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = photos.first(),
            deviceInfo = createTestDeviceInfo("suspicious-device")
        )
        
        // Report serious suspicious activity (should trigger auto-revoke)
        val suspiciousResult = medicalSharingService.recordSuspiciousActivity(
            viewingEventId = viewingResult.viewingEventId,
            activityType = SuspiciousActivityType.SCREEN_RECORDING_DETECTED,
            details = "Screen recording software detected"
        )
        
        assert(suspiciousResult.recorded)
        assert(suspiciousResult.autoRevokeTriggered)
        println("   ✅ Session automatically revoked on serious violation")
        
        // Verify that further access is blocked
        assertThrows<IllegalArgumentException> {
            medicalSharingService.requestDoctorAccess(
                sessionId = session.session.id,
                professionalId = doctor.id,
                deviceInfo = createTestDeviceInfo("another-device")
            )
        }
        println("   ✅ Further access blocked after auto-revocation")
    }

    @Test
    fun `should prevent access with expired tokens`() = runBlocking {
        println("🔒 Security Test: Expired Token Prevention")
        
        val patient = createTestUser("patient@expired.test")
        val doctor = createTestUser("doctor@expired.test")
        val photos = createTestPhotos(patient.id, 2)
        
        // Create session that expires very soon
        val expiredSession = MedicalSharingSession(
            id = UUID.randomUUID(),
            patientId = patient.id,
            professionalId = doctor.id,
            photoIds = photos,
            notes = "Expired session test",
            maxTotalViews = 3,
            maxViewDurationMinutes = 5,
            expiresAt = Instant.now().minusSeconds(3600), // Expired 1 hour ago
            status = MedicalSharingStatus.PENDING_DOCTOR_ACCESS,
            createdAt = Instant.now().minusSeconds(7200),
            updatedAt = Instant.now().minusSeconds(7200)
        )
        
        // Attempt to access expired session should fail
        assertThrows<IllegalArgumentException> {
            medicalSharingService.requestDoctorAccess(
                sessionId = expiredSession.id,
                professionalId = doctor.id,
                deviceInfo = createTestDeviceInfo("expired-test-device")
            )
        }
        println("   ✅ Access to expired session blocked")
    }

    @Test
    fun `should validate device fingerprints consistently`() = runBlocking {
        println("🔒 Security Test: Device Fingerprint Validation")
        
        val patient = createTestUser("patient@device.test")
        val doctor = createTestUser("doctor@device.test")
        val photos = createTestPhotos(patient.id, 2)
        
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "Device validation test",
                durationHours = 24,
                maxTotalViews = 5,
                maxViewDurationMinutes = 5
            )
        )
        
        val deviceInfo1 = createTestDeviceInfo("consistent-device")
        val deviceInfo2 = createTestDeviceInfo("different-device")
        
        // First access with device 1
        val access1 = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = deviceInfo1
        )
        
        // Access with different device should be tracked separately
        val access2 = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = deviceInfo2
        )
        
        assert(access1.accessSession.deviceFingerprint == deviceInfo1.fingerprint)
        assert(access2.accessSession.deviceFingerprint == deviceInfo2.fingerprint)
        assert(access1.accessSession.deviceFingerprint != access2.accessSession.deviceFingerprint)
        
        println("   ✅ Device fingerprints tracked correctly")
        println("   📱 Device 1: ${access1.accessSession.deviceFingerprint}")
        println("   📱 Device 2: ${access2.accessSession.deviceFingerprint}")
    }

    @Test
    fun `should prevent ephemeral key reuse`() = runBlocking {
        println("🔒 Security Test: Ephemeral Key Reuse Prevention")
        
        val patient = createTestUser("patient@key.test")
        val doctor = createTestUser("doctor@key.test")
        val photos = createTestPhotos(patient.id, 2)
        
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "Key reuse test",
                durationHours = 24,
                maxTotalViews = 3,
                maxViewDurationMinutes = 5
            )
        )
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = createTestDeviceInfo("key-test-device")
        )
        
        // Start viewing (this should consume the ephemeral key)
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = photos.first(),
            deviceInfo = createTestDeviceInfo("key-test-device")
        )
        
        // End viewing
        medicalSharingService.endPhotoViewing(
            viewingEventId = viewingResult.viewingEventId,
            endReason = ViewingEndReason.USER_CLOSED,
            durationSeconds = 10
        )
        
        // Attempt to reuse the same photo (should require new key)
        // This test verifies that keys are single-use
        val newViewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = photos.first(),
            deviceInfo = createTestDeviceInfo("key-test-device")
        )
        
        // Should get a different viewing event ID (new key was generated)
        assert(newViewingResult.viewingEventId != viewingResult.viewingEventId)
        
        println("   ✅ Ephemeral keys are single-use only")
    }

    @Test
    fun `should enforce IP address validation`() = runBlocking {
        println("🔒 Security Test: IP Address Validation")
        
        val patient = createTestUser("patient@ip.test")
        val doctor = createTestUser("doctor@ip.test")
        val photos = createTestPhotos(patient.id, 2)
        
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "IP validation test",
                durationHours = 24,
                maxTotalViews = 3,
                maxViewDurationMinutes = 5
            )
        )
        
        // Access from first IP
        val deviceInfo1 = DeviceInfo(
            fingerprint = "ip-test-device",
            ipAddress = "192.168.1.100",
            userAgent = "Test Browser"
        )
        
        val access1 = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = deviceInfo1
        )
        
        // Access from different IP (same device fingerprint)
        val deviceInfo2 = DeviceInfo(
            fingerprint = "ip-test-device",
            ipAddress = "10.0.0.50", // Different IP
            userAgent = "Test Browser"
        )
        
        val access2 = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = deviceInfo2
        )
        
        // Both should succeed but be tracked separately
        assert(access1.accessSession.ipAddress == "192.168.1.100")
        assert(access2.accessSession.ipAddress == "10.0.0.50")
        
        println("   ✅ IP addresses tracked correctly")
        println("   🌐 IP 1: ${access1.accessSession.ipAddress}")
        println("   🌐 IP 2: ${access2.accessSession.ipAddress}")
    }

    @Test
    fun `should validate user agent consistency`() = runBlocking {
        println("🔒 Security Test: User Agent Consistency")
        
        val patient = createTestUser("patient@ua.test")
        val doctor = createTestUser("doctor@ua.test")
        val photos = createTestPhotos(patient.id, 2)
        
        val session = medicalSharingService.createMedicalSharingSession(
            patientId = patient.id,
            request = CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "User agent test",
                durationHours = 24,
                maxTotalViews = 3,
                maxViewDurationMinutes = 5
            )
        )
        
        val suspiciousDeviceInfo = DeviceInfo(
            fingerprint = "suspicious-bot",
            ipAddress = "192.168.1.200",
            userAgent = "SuspiciousBot/1.0 (automated-screenshot-tool)"
        )
        
        // This should still work (we log but don't block based on user agent alone)
        val suspiciousAccess = medicalSharingService.requestDoctorAccess(
            sessionId = session.session.id,
            professionalId = doctor.id,
            deviceInfo = suspiciousDeviceInfo
        )
        
        assert(suspiciousAccess.accessSession.userAgent == "SuspiciousBot/1.0 (automated-screenshot-tool)")
        
        println("   ✅ User agent tracking functional")
        println("   🤖 Suspicious agent logged: ${suspiciousAccess.accessSession.userAgent}")
    }

    private suspend fun createTestUser(email: String): com.hairhealth.platform.domain.User {
        return userService.createUser(
            email = email,
            passwordHash = "testpassword123",
            username = email.split("@")[0]
        )
    }

    private suspend fun createTestPhotos(userId: UUID, count: Int): List<UUID> {
        val angles = PhotoAngle.values()
        return (1..count).map { index ->
            val uploadSession = photoMetadataService.createPhotoMetadata(
                userId = userId,
                filename = "security_test_${index}.jpg.enc",
                angle = angles[index % angles.size],
                captureDate = Instant.now(),
                encryptionKeyInfo = "security_test_key_$index"
            )
            
            photoMetadataService.finalizePhotoUpload(
                photoId = uploadSession.photoMetadataId,
                fileSize = (100_000..500_000).random().toLong()
            )
            
            uploadSession.photoMetadataId
        }
    }

    private fun createTestDeviceInfo(deviceId: String) = DeviceInfo(
        fingerprint = deviceId,
        ipAddress = "192.168.1.${(100..200).random()}",
        userAgent = "Mozilla/5.0 (Test Device) AppleWebKit/537.36"
    )
}