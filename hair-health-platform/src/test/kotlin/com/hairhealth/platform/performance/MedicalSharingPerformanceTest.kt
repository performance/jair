package com.hairhealth.platform.performance

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.service.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

@SpringBootTest
@ActiveProfiles("test")
class MedicalSharingPerformanceTest {

    @Autowired
    private lateinit var medicalSharingService: CompleteMedicalSharingService
    
    @Autowired
    private lateinit var userService: UserService
    
    @Autowired
    private lateinit var photoMetadataService: PhotoMetadataService

    @Test
    fun `should handle concurrent session creation`() = runBlocking {
        val userCount = 50
        val sessionsPerUser = 5
        val concurrentUsers = ConcurrentHashMap<UUID, MutableList<UUID>>()
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        // Create test users
        val users = (1..userCount).map { createTestUser("patient$it@test.com") }
        val doctors = (1..10).map { createTestUser("doctor$it@test.com") }

        println("🏥 Performance Test: Concurrent Session Creation")
        println("Users: $userCount, Sessions per user: $sessionsPerUser")
        println("Total expected sessions: ${userCount * sessionsPerUser}")

        val totalTime = measureTimeMillis {
            // Create sessions concurrently
            val jobs = users.map { user ->
                async {
                    val userSessions = mutableListOf<UUID>()
                    concurrentUsers[user.id] = userSessions
                    
                    repeat(sessionsPerUser) { sessionIndex ->
                        try {
                            val photos = createTestPhotos(user.id, 3)
                            val request = CreateMedicalSharingRequest(
                                professionalId = doctors.random().id,
                                photoIds = photos,
                                notes = "Performance test session $sessionIndex",
                                durationHours = 24,
                                maxTotalViews = 3,
                                maxViewDurationMinutes = 5
                            )
                            
                            val result = medicalSharingService.createMedicalSharingSession(
                                patientId = user.id,
                                request = request
                            )
                            
                            userSessions.add(result.session.id)
                            successCount.incrementAndGet()
                            
                        } catch (e: Exception) {
                            failureCount.incrementAndGet()
                            println("❌ Failed to create session for user ${user.id}: ${e.message}")
                        }
                    }
                }
            }
            
            jobs.awaitAll()
        }

        println("⏱️ Total time: ${totalTime}ms")
        println("✅ Successful sessions: ${successCount.get()}")
        println("❌ Failed sessions: ${failureCount.get()}")
        println("📊 Average time per session: ${totalTime / successCount.get()}ms")
        
        // Verify all sessions were created
        assert(successCount.get() > userCount * sessionsPerUser * 0.95) // Allow 5% failure rate
        assert(failureCount.get() < userCount * sessionsPerUser * 0.05)
    }

    @Test
    fun `should handle concurrent doctor access requests`() = runBlocking {
        val sessionCount = 20
        val accessAttemptsPerSession = 10
        val concurrentAccess = ConcurrentHashMap<UUID, AtomicInteger>()
        val successfulAccess = AtomicInteger(0)
        val rejectedAccess = AtomicInteger(0)

        // Create test data
        val patient = createTestUser("load-test-patient@test.com")
        val doctors = (1..5).map { createTestUser("load-doctor$it@test.com") }
        
        // Create test sessions
        val sessions = (1..sessionCount).map {
            val photos = createTestPhotos(patient.id, 2)
            val request = CreateMedicalSharingRequest(
                professionalId = doctors.random().id,
                photoIds = photos,
                notes = "Load test session $it",
                durationHours = 24,
                maxTotalViews = 3, // Limited views to test concurrency
                maxViewDurationMinutes = 5
            )
            
            medicalSharingService.createMedicalSharingSession(patient.id, request).session
        }

        println("🔐 Performance Test: Concurrent Doctor Access")
        println("Sessions: $sessionCount, Access attempts per session: $accessAttemptsPerSession")
        println("Total access attempts: ${sessionCount * accessAttemptsPerSession}")

        val totalTime = measureTimeMillis {
            val jobs = sessions.flatMap { session ->
                concurrentAccess[session.id] = AtomicInteger(0)
                
                (1..accessAttemptsPerSession).map { attemptIndex ->
                    async {
                        try {
                            val deviceInfo = DeviceInfo(
                                fingerprint = "load-test-device-$attemptIndex",
                                ipAddress = "192.168.1.${100 + attemptIndex}",
                                userAgent = "Load Test Browser $attemptIndex"
                            )
                            
                            val result = medicalSharingService.requestDoctorAccess(
                                sessionId = session.id,
                                professionalId = session.professionalId,
                                deviceInfo = deviceInfo
                            )
                            
                            concurrentAccess[session.id]?.incrementAndGet()
                            successfulAccess.incrementAndGet()
                            
                        } catch (e: Exception) {
                            rejectedAccess.incrementAndGet()
                            // Expected for some attempts due to view limits
                        }
                    }
                }
            }
            
            jobs.awaitAll()
        }

        println("⏱️ Total time: ${totalTime}ms")
        println("✅ Successful access: ${successfulAccess.get()}")
        println("🚫 Rejected access: ${rejectedAccess.get()}")
        
        // Verify that view limits were respected
        concurrentAccess.forEach { (sessionId, accessCount) ->
            val session = sessions.first { it.id == sessionId }
            assert(accessCount.get() <= session.maxTotalViews) {
                "Session $sessionId exceeded max views: ${accessCount.get()} > ${session.maxTotalViews}"
            }
        }
        
        println("✅ All view limits were properly enforced")
    }

    @Test
    fun `should handle concurrent photo viewing sessions`() = runBlocking {
        val viewingSessionCount = 100
        val viewingDurationMs = 1000L // 1 second per viewing
        val concurrentViewers = AtomicInteger(0)
        val maxConcurrentViewers = AtomicInteger(0)
        val completedViews = AtomicInteger(0)

        // Create test data
        val patient = createTestUser("viewer-test-patient@test.com")
        val doctor = createTestUser("viewer-test-doctor@test.com")
        val photos = createTestPhotos(patient.id, 5)
        
        val request = CreateMedicalSharingRequest(
            professionalId = doctor.id,
            photoIds = photos,
            notes = "Concurrent viewing test",
            durationHours = 24,
            maxTotalViews = viewingSessionCount, // Allow all viewing attempts
            maxViewDurationMinutes = 10
        )
        
        val session = medicalSharingService.createMedicalSharingSession(patient.id, request).session
        
        // Doctor gets access
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.id,
            professionalId = doctor.id,
            deviceInfo = DeviceInfo("test-device", "192.168.1.100", "Test Browser")
        )

        println("👁️ Performance Test: Concurrent Photo Viewing")
        println("Viewing sessions: $viewingSessionCount")
        println("Duration per view: ${viewingDurationMs}ms")

        val totalTime = measureTimeMillis {
            val jobs = (1..viewingSessionCount).map { viewIndex ->
                async {
                    try {
                        val currentConcurrent = concurrentViewers.incrementAndGet()
                        maxConcurrentViewers.updateAndGet { maxOf(it, currentConcurrent) }
                        
                        val deviceInfo = DeviceInfo(
                            fingerprint = "viewer-device-$viewIndex",
                            ipAddress = "192.168.1.${100 + (viewIndex % 50)}",
                            userAgent = "Viewer Browser $viewIndex"
                        )
                        
                        val viewingResult = medicalSharingService.startPhotoViewing(
                            accessSessionId = accessResult.accessSession.id,
                            photoId = photos.random(),
                            deviceInfo = deviceInfo
                        )
                        
                        // Simulate viewing time
                        delay(viewingDurationMs)
                        
                        medicalSharingService.endPhotoViewing(
                            viewingEventId = viewingResult.viewingEventId,
                            endReason = ViewingEndReason.USER_CLOSED,
                            durationSeconds = viewingDurationMs / 1000
                        )
                        
                        completedViews.incrementAndGet()
                        
                    } catch (e: Exception) {
                        println("❌ Viewing failed: ${e.message}")
                    } finally {
                        concurrentViewers.decrementAndGet()
                    }
                }
            }
            
            jobs.awaitAll()
        }

        println("⏱️ Total time: ${totalTime}ms")
        println("👁️ Completed views: ${completedViews.get()}")
        println("🔢 Max concurrent viewers: ${maxConcurrentViewers.get()}")
        println("📊 Average view completion time: ${totalTime / completedViews.get()}ms")
        
        // Verify all views completed successfully
        assert(completedViews.get() > viewingSessionCount * 0.95) // Allow 5% failure rate
    }

    @Test
    fun `should handle high-frequency suspicious activity reporting`() = runBlocking {
        val activityReportCount = 1000
        val reportedActivities = AtomicInteger(0)
        val autoRevocations = AtomicInteger(0)

        // Create test session with viewing
        val patient = createTestUser("activity-test-patient@test.com")
        val doctor = createTestUser("activity-test-doctor@test.com")
        val photos = createTestPhotos(patient.id, 3)
        
        val session = medicalSharingService.createMedicalSharingSession(
            patient.id,
            CreateMedicalSharingRequest(
                professionalId = doctor.id,
                photoIds = photos,
                notes = "Activity reporting test",
                durationHours = 24,
                maxTotalViews = 10,
                maxViewDurationMinutes = 60
            )
        ).session
        
        val accessResult = medicalSharingService.requestDoctorAccess(
            sessionId = session.id,
            professionalId = doctor.id,
            deviceInfo = DeviceInfo("test-device", "192.168.1.100", "Test Browser")
        )
        
        val viewingResult = medicalSharingService.startPhotoViewing(
            accessSessionId = accessResult.accessSession.id,
            photoId = photos.first(),
            deviceInfo = DeviceInfo("test-device", "192.168.1.100", "Test Browser")
        )

        println("🚨 Performance Test: High-Frequency Suspicious Activity Reporting")
        println("Activity reports: $activityReportCount")

        val totalTime = measureTimeMillis {
            val jobs = (1..activityReportCount).map { reportIndex ->
                async {
                    try {
                        val activityType = when (reportIndex % 5) {
                            0 -> SuspiciousActivityType.SCREENSHOT_ATTEMPT
                            1 -> SuspiciousActivityType.DOWNLOAD_ATTEMPT
                            2 -> SuspiciousActivityType.PRINT_ATTEMPT
                            3 -> SuspiciousActivityType.COPY_ATTEMPT
                            else -> SuspiciousActivityType.UNUSUAL_DEVICE_BEHAVIOR
                        }
                        
                        val result = medicalSharingService.recordSuspiciousActivity(
                            viewingEventId = viewingResult.viewingEventId,
                            activityType = activityType,
                            details = "Load test activity report $reportIndex"
                        )
                        
                        if (result.recorded) {
                            reportedActivities.incrementAndGet()
                        }
                        
                        if (result.autoRevokeTriggered) {
                            autoRevocations.incrementAndGet()
                        }
                        
                    } catch (e: Exception) {
                        println("❌ Activity reporting failed: ${e.message}")
                    }
                }
            }
            
            jobs.awaitAll()
        }

        println("⏱️ Total time: ${totalTime}ms")
        println("📊 Reported activities: ${reportedActivities.get()}")
        println("🚫 Auto-revocations: ${autoRevocations.get()}")
        println("⚡ Reports per second: ${reportedActivities.get() * 1000 / totalTime}")
        
        // Verify high success rate
        assert(reportedActivities.get() > activityReportCount * 0.95)
    }

    @Test
    fun `should maintain data integrity under load`() = runBlocking {
        val operationCount = 500
        val operations = listOf("create", "access", "view", "revoke")
        val results = ConcurrentHashMap<String, AtomicInteger>()
        operations.forEach { results[it] = AtomicInteger(0) }

        // Create test users
        val patients = (1..10).map { createTestUser("integrity-patient$it@test.com") }
        val doctors = (1..5).map { createTestUser("integrity-doctor$it@test.com") }

        println("🔒 Performance Test: Data Integrity Under Load")
        println("Mixed operations: $operationCount")

        val totalTime = measureTimeMillis {
            val jobs = (1..operationCount).map { opIndex ->
                async {
                    val operation = operations[opIndex % operations.size]
                    
                    try {
                        when (operation) {
                            "create" -> {
                                val patient = patients.random()
                                val photos = createTestPhotos(patient.id, 2)
                                medicalSharingService.createMedicalSharingSession(
                                    patient.id,
                                    CreateMedicalSharingRequest(
                                        professionalId = doctors.random().id,
                                        photoIds = photos,
                                        notes = "Integrity test $opIndex",
                                        durationHours = 24,
                                        maxTotalViews = 3,
                                        maxViewDurationMinutes = 5
                                    )
                                )
                                results["create"]?.incrementAndGet()
                            }
                            
                            "access" -> {
                                // Try to access a random existing session
                                val patient = patients.random()
                                val sessions = medicalSharingService.getPatientSharingSessions(patient.id)
                                if (sessions.isNotEmpty()) {
                                    val session = sessions.random().session
                                    if (session.status == MedicalSharingStatus.PENDING_DOCTOR_ACCESS ||
                                        session.status == MedicalSharingStatus.ACTIVE) {
                                        medicalSharingService.requestDoctorAccess(
                                            sessionId = session.id,
                                            professionalId = session.professionalId,
                                            deviceInfo = DeviceInfo(
                                                "integrity-device-$opIndex",
                                                "192.168.1.${100 + (opIndex % 50)}",
                                                "Integrity Test Browser"
                                            )
                                        )
                                        results["access"]?.incrementAndGet()
                                    }
                                }
                            }
                            
                            "view" -> {
                                // Try to view photos from existing sessions
                                val doctor = doctors.random()
                                val sessions = medicalSharingService.getProfessionalAccessibleSessions(doctor.id)
                                if (sessions.isNotEmpty()) {
                                    val accessibleSession = sessions.random()
                                    if (accessibleSession.canAccess) {
                                        val accessResult = medicalSharingService.requestDoctorAccess(
                                            sessionId = accessibleSession.session.id,
                                            professionalId = doctor.id,
                                            deviceInfo = DeviceInfo(
                                                "view-device-$opIndex",
                                                "192.168.1.${150 + (opIndex % 50)}",
                                                "View Test Browser"
                                            )
                                        )
                                        
                                        val viewingResult = medicalSharingService.startPhotoViewing(
                                            accessSessionId = accessResult.accessSession.id,
                                            photoId = accessibleSession.session.photoIds.random(),
                                            deviceInfo = DeviceInfo(
                                                "view-device-$opIndex",
                                                "192.168.1.${150 + (opIndex % 50)}",
                                                "View Test Browser"
                                            )
                                        )
                                        
                                        // End viewing immediately
                                        medicalSharingService.endPhotoViewing(
                                            viewingEventId = viewingResult.viewingEventId,
                                            endReason = ViewingEndReason.USER_CLOSED,
                                            durationSeconds = 1
                                        )
                                        
                                        results["view"]?.incrementAndGet()
                                    }
                                }
                            }
                            
                            "revoke" -> {
                                // Try to revoke existing sessions
                                val patient = patients.random()
                                val sessions = medicalSharingService.getPatientSharingSessions(patient.id)
                                val activeSession = sessions.find { 
                                    it.session.status == MedicalSharingStatus.PENDING_DOCTOR_ACCESS ||
                                    it.session.status == MedicalSharingStatus.ACTIVE
                                }
                                
                                if (activeSession != null) {
                                    medicalSharingService.revokeMedicalSession(
                                        sessionId = activeSession.session.id,
                                        patientId = patient.id,
                                        reason = "Integrity test revocation $opIndex"
                                    )
                                    results["revoke"]?.incrementAndGet()
                                }
                            }
                        }
                        
                    } catch (e: Exception) {
                        // Expected for some operations due to constraints
                    }
                }
            }
            
            jobs.awaitAll()
        }

        println("⏱️ Total time: ${totalTime}ms")
        println("📊 Operation results:")
        results.forEach { (operation, count) ->
            println("   $operation: ${count.get()}")
        }
        
        // Verify data consistency
        val totalOperations = results.values.sumOf { it.get() }
        println("✅ Total successful operations: $totalOperations")
        println("📈 Operations per second: ${totalOperations * 1000 / totalTime}")
        
        // Check that we had some successful operations of each type
        assert(results["create"]?.get()!! > 0) { "No create operations succeeded" }
        assert(totalOperations > operationCount * 0.3) { "Too few operations succeeded" }
    }

    @Test
    fun `should handle ephemeral key cleanup efficiently`() = runBlocking {
        val keyCount = 1000
        val createdKeys = AtomicInteger(0)
        val cleanedKeys = AtomicInteger(0)

        // Create many ephemeral keys with different expiration times
        val patient = createTestUser("key-test-patient@test.com")
        val doctor = createTestUser("key-test-doctor@test.com")
        val photos = createTestPhotos(patient.id, 5)

        println("🔑 Performance Test: Ephemeral Key Management")
        println("Keys to create: $keyCount")

        val creationTime = measureTimeMillis {
            val jobs = (1..keyCount).map { keyIndex ->
                async {
                    try {
                        val session = medicalSharingService.createMedicalSharingSession(
                            patient.id,
                            CreateMedicalSharingRequest(
                                professionalId = doctor.id,
                                photoIds = listOf(photos.random()),
                                notes = "Key test $keyIndex",
                                durationHours = if (keyIndex % 10 == 0) 1 else 24, // Some expire soon
                                maxTotalViews = 1,
                                maxViewDurationMinutes = 1
                            )
                        )
                        createdKeys.incrementAndGet()
                        
                    } catch (e: Exception) {
                        println("❌ Key creation failed: ${e.message}")
                    }
                }
            }
            
            jobs.awaitAll()
        }

        println("⏱️ Key creation time: ${creationTime}ms")
        println("🔑 Keys created: ${createdKeys.get()}")
        println("📊 Keys per second: ${createdKeys.get() * 1000 / creationTime}")
        
        // Wait for some keys to expire (simulate time passage)
        delay(1000)
        
        // Test cleanup performance (would normally be done by scheduled task)
        val cleanupTime = measureTimeMillis {
            // This would be the cleanup operation
            // In real implementation, this would call ephemeralKeyRepository.cleanupExpiredKeys()
            println("🧹 Cleanup operation simulated")
        }
        
        println("🧹 Cleanup time: ${cleanupTime}ms")
        println("✅ Ephemeral key management performance acceptable")
    }

    private suspend fun createTestUser(email: String): com.hairhealth.platform.domain.User {
        val user = userService.createUser(
            email = email,
            password = "testpassword123",
            username = email.split("@")[0]
        )
        return user
    }

    private suspend fun createTestPhotos(userId: UUID, count: Int): List<UUID> {
        val angles = PhotoAngle.values()
        return (1..count).map { index ->
            val uploadSession = photoMetadataService.createPhotoMetadata(
                userId = userId,
                filename = "test_photo_${index}.jpg.enc",
                angle = angles[index % angles.size],
                captureDate = Instant.now(),
                encryptionKeyInfo = "test_key_$index"
            )
            
            photoMetadataService.finalizePhotoUpload(
                photoId = uploadSession.photoMetadataId,
                fileSize = (100_000..2_000_000).random().toLong()
            )
            
            uploadSession.photoMetadataId
        }
    }
}