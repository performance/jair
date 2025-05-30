package com.hairhealth.platform.service

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class CompleteMedicalSharingService(
    private val medicalSharingRepository: MedicalSharingRepository,
    private val doctorAccessRepository: DoctorAccessRepository,
    private val viewingEventRepository: ViewingEventRepository,
    private val ephemeralKeyRepository: EphemeralKeyRepository,
    private val notificationRepository: MedicalNotificationRepository,
    private val photoMetadataRepository: PhotoMetadataRepository,
    private val encryptionService: AdvancedEncryptionService,
    private val notificationService: RealTimeNotificationService
) {

    suspend fun createMedicalSharingSession(
        patientId: UUID,
        request: CreateMedicalSharingRequest
    ): MedicalSharingSessionResult {
        
        // Validate patient owns all requested photos
        val photos = validatePhotosOwnership(patientId, request.photoIds)
        
        // Create medical sharing session
        val session = MedicalSharingSession(
            id = UUID.randomUUID(),
            patientId = patientId,
            professionalId = request.professionalId,
            photoIds = request.photoIds,
            notes = request.notes,
            maxTotalViews = request.maxTotalViews,
            maxViewDurationMinutes = request.maxViewDurationMinutes,
            expiresAt = Instant.now().plus(request.durationHours, ChronoUnit.HOURS),
            allowScreenshots = false,
            allowDownload = false,
            status = MedicalSharingStatus.PENDING_DOCTOR_ACCESS,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val createdSession = medicalSharingRepository.createSession(session)
        
        // Generate ephemeral decryption keys for each photo
        val ephemeralKeys = photos.map { photo ->
            generateEphemeralKey(createdSession, photo)
        }
        
        // Create notification for professional
        val notification = MedicalNotification(
            id = UUID.randomUUID(),
            recipientId = request.professionalId,
            type = NotificationType.MEDICAL_SHARING_CREATED,
            title = "New Medical Photos Shared",
            message = "A patient has shared medical photos with you for review",
            relatedSessionId = createdSession.id,
            relatedProfessionalId = null,
            createdAt = Instant.now()
        )
        
        notificationRepository.createNotification(notification)
        notificationService.sendRealTimeNotification(request.professionalId, notification)
        
        return MedicalSharingSessionResult(
            session = createdSession,
            ephemeralKeys = ephemeralKeys,
            secureAccessUrl = generateSecureAccessUrl(createdSession.id)
        )
    }
    
    suspend fun requestDoctorAccess(
        sessionId: UUID,
        professionalId: UUID,
        deviceInfo: DeviceInfo
    ): DoctorAccessResult {
        
        val session = medicalSharingRepository.findSessionById(sessionId)
            ?: throw IllegalArgumentException("Medical sharing session not found")
        
        // Validate access permissions
        validateDoctorAccess(session, professionalId)
        
        // Check access limits
        val existingAccessCount = doctorAccessRepository.countAccessSessionsByMedicalSession(sessionId)
        if (existingAccessCount >= session.maxTotalViews) {
            updateSessionStatus(session, MedicalSharingStatus.EXHAUSTED_ATTEMPTS)
            throw IllegalArgumentException("Maximum access attempts exceeded")
        }
        
        // Create doctor access session
        val accessSession = DoctorAccessSession(
            id = UUID.randomUUID(),
            medicalSessionId = sessionId,
            professionalId = professionalId,
            deviceFingerprint = deviceInfo.fingerprint,
            ipAddress = deviceInfo.ipAddress,
            userAgent = deviceInfo.userAgent,
            startedAt = Instant.now(),
            expiresAt = Instant.now().plus(session.maxViewDurationMinutes.toLong(), ChronoUnit.MINUTES),
            isActive = true
        )
        
        val createdAccessSession = doctorAccessRepository.createAccessSession(accessSession)
        
        // Update session status to active if first access
        if (session.status == MedicalSharingStatus.PENDING_DOCTOR_ACCESS) {
            updateSessionStatus(session, MedicalSharingStatus.ACTIVE)
        }
        
        // Notify patient of doctor access
        val patientNotification = MedicalNotification(
            id = UUID.randomUUID(),
            recipientId = session.patientId,
            type = NotificationType.DOCTOR_ACCESSED_PHOTOS,
            title = "Doctor Accessed Your Photos",
            message = "Your healthcare provider has accessed the photos you shared",
            relatedSessionId = sessionId,
            relatedProfessionalId = professionalId,
            createdAt = Instant.now()
        )
        
        notificationRepository.createNotification(patientNotification)
        notificationService.sendRealTimeNotification(session.patientId, patientNotification)
        
        return DoctorAccessResult(
            accessSession = createdAccessSession,
            secureViewerUrl = generateSecureViewerUrl(createdAccessSession.id),
            photos = getPhotosForAccess(session),
            restrictions = createAccessRestrictions(session)
        )
    }
    
    suspend fun startPhotoViewing(
        accessSessionId: UUID,
        photoId: UUID,
        deviceInfo: DeviceInfo
    ): PhotoViewingResult {
        
        val accessSession = doctorAccessRepository.findAccessSessionById(accessSessionId)
            ?: throw IllegalArgumentException("Access session not found")
        
        validateAccessSession(accessSession)
        
        // Get ephemeral decryption key
        val ephemeralKey = ephemeralKeyRepository.findKeyBySessionAndPhoto(
            accessSession.medicalSessionId, 
            photoId
        ) ?: throw IllegalArgumentException("Decryption key not found")
        
        // Validate key usage
        if (ephemeralKey.currentUses >= ephemeralKey.maxUses) {
            throw IllegalArgumentException("Key usage limit exceeded")
        }
        
        if (ephemeralKey.expiresAt.isBefore(Instant.now())) {
            throw IllegalArgumentException("Decryption key has expired")
        }
        
        // Increment key usage
        ephemeralKeyRepository.incrementKeyUsage(ephemeralKey.id)
        
        // Create viewing event
        val viewingEvent = ViewingEvent(
            id = UUID.randomUUID(),
            accessSessionId = accessSessionId,
            photoId = photoId,
            startedAt = Instant.now(),
            deviceFingerprint = deviceInfo.fingerprint,
            ipAddress = deviceInfo.ipAddress,
            screenshotAttempts = 0,
            downloadAttempts = 0,
            suspiciousActivity = false
        )
        
        val createdEvent = viewingEventRepository.createViewingEvent(viewingEvent)
        
        // Get medical session for notification
        val medicalSession = medicalSharingRepository.findSessionById(accessSession.medicalSessionId)!!
        
        // Notify patient of viewing start
        val notification = MedicalNotification(
            id = UUID.randomUUID(),
            recipientId = medicalSession.patientId,
            type = NotificationType.DOCTOR_VIEWING_STARTED,
            title = "Doctor Viewing Photos",
            message = "Your healthcare provider is currently viewing your photos",
            relatedSessionId = accessSession.medicalSessionId,
            relatedProfessionalId = accessSession.professionalId,
            createdAt = Instant.now()
        )
        
        notificationRepository.createNotification(notification)
        notificationService.sendRealTimeNotification(medicalSession.patientId, notification)
        
        // Generate time-limited decryption token
        val decryptionToken = encryptionService.generateTimeboxedDecryptionToken(
            ephemeralKey,
            medicalSession.maxViewDurationMinutes
        )
        
        return PhotoViewingResult(
            viewingEventId = createdEvent.id,
            decryptionToken = decryptionToken,
            maxViewTimeSeconds = medicalSession.maxViewDurationMinutes * 60,
            restrictions = createViewingRestrictions(medicalSession)
        )
    }
    
    suspend fun endPhotoViewing(
        viewingEventId: UUID,
        endReason: ViewingEndReason,
        durationSeconds: Long
    ): ViewingEndResult {
        
        val ended = viewingEventRepository.endViewingEvent(
            viewingEventId, 
            Instant.now(), 
            durationSeconds
        )
        
        if (!ended) {
            throw IllegalArgumentException("Could not end viewing event")
        }
        
        // Get viewing event details for notification
        val events = viewingEventRepository.findEventsByAccessSession(viewingEventId)
        val event = events.find { it.id == viewingEventId }
        
        if (event != null) {
            // Get access session and medical session
            val accessSession = doctorAccessRepository.findAccessSessionById(event.accessSessionId)
            val medicalSession = accessSession?.let { 
                medicalSharingRepository.findSessionById(it.medicalSessionId) 
            }
            
            if (medicalSession != null) {
                // Notify patient of viewing end
                val notification = MedicalNotification(
                    id = UUID.randomUUID(),
                    recipientId = medicalSession.patientId,
                    type = NotificationType.DOCTOR_VIEWING_ENDED,
                    title = "Doctor Finished Viewing",
                    message = "Your healthcare provider has finished viewing your photos (${durationSeconds}s)",
                    relatedSessionId = medicalSession.id,
                    relatedProfessionalId = accessSession.professionalId,
                    createdAt = Instant.now()
                )
                
                notificationRepository.createNotification(notification)
                notificationService.sendRealTimeNotification(medicalSession.patientId, notification)
            }
        }
        
        return ViewingEndResult(
            success = true,
            finalDurationSeconds = durationSeconds,
            endReason = endReason
        )
    }
    
    suspend fun recordSuspiciousActivity(
        viewingEventId: UUID,
        activityType: SuspiciousActivityType,
        details: String
    ): SuspiciousActivityResult {
        
        val recorded = viewingEventRepository.recordSuspiciousActivity(
            viewingEventId, 
            activityType.name
        )
        
        if (recorded) {
            // Get event context for notifications
            val events = viewingEventRepository.findEventsByAccessSession(viewingEventId)
            val event = events.find { it.id == viewingEventId }
            
            if (event != null) {
                val accessSession = doctorAccessRepository.findAccessSessionById(event.accessSessionId)
                val medicalSession = accessSession?.let { 
                    medicalSharingRepository.findSessionById(it.medicalSessionId) 
                }
                
                if (medicalSession != null) {
                    // Notify patient immediately
                    val notification = MedicalNotification(
                        id = UUID.randomUUID(),
                        recipientId = medicalSession.patientId,
                        type = NotificationType.SUSPICIOUS_ACCESS_DETECTED,
                        title = "Suspicious Activity Detected",
                        message = "Unusual activity detected during photo viewing: ${activityType.name}",
                        relatedSessionId = medicalSession.id,
                        relatedProfessionalId = accessSession.professionalId,
                        createdAt = Instant.now()
                    )
                    
                    notificationRepository.createNotification(notification)
                    notificationService.sendRealTimeNotification(medicalSession.patientId, notification)
                    
                    // Consider auto-revoking session for severe violations
                    if (activityType in listOf(
                        SuspiciousActivityType.MULTIPLE_SCREENSHOT_ATTEMPTS,
                        SuspiciousActivityType.DOWNLOAD_ATTEMPT,
                        SuspiciousActivityType.SCREEN_RECORDING_DETECTED
                    )) {
                        revokeMedicalSession(
                            medicalSession.id, 
                            medicalSession.patientId,
                            "Automatically revoked due to suspicious activity: ${activityType.name}"
                        )
                    }
                }
            }
        }
        
        return SuspiciousActivityResult(
            recorded = recorded,
            activityType = activityType,
            autoRevokeTriggered = activityType in listOf(
                SuspiciousActivityType.MULTIPLE_SCREENSHOT_ATTEMPTS,
                SuspiciousActivityType.DOWNLOAD_ATTEMPT,
                SuspiciousActivityType.SCREEN_RECORDING_DETECTED
            )
        )
    }
    
    suspend fun revokeMedicalSession(
        sessionId: UUID,
        patientId: UUID,
        reason: String
    ): SessionRevocationResult {
        
        val session = medicalSharingRepository.findSessionById(sessionId)
            ?: throw IllegalArgumentException("Session not found")
        
        if (session.patientId != patientId) {
            throw IllegalArgumentException("Unauthorized to revoke this session")
        }
        
        val revoked = medicalSharingRepository.revokeSession(sessionId, reason)
        
        if (revoked) {
            // Revoke all ephemeral keys for this session
            session.photoIds.forEach { photoId ->
                ephemeralKeyRepository.findKeyBySessionAndPhoto(sessionId, photoId)?.let { key ->
                    ephemeralKeyRepository.revokeKey(key.id)
                }
            }
            
            // End all active access sessions
            val activeAccessSessions = doctorAccessRepository.findActiveAccessSessionsByMedicalSession(sessionId)
            activeAccessSessions.forEach { accessSession ->
                doctorAccessRepository.endAccessSession(accessSession.id, Instant.now())
            }
            
            // Notify professional of revocation
            val professionalNotification = MedicalNotification(
                id = UUID.randomUUID(),
                recipientId = session.professionalId,
                type = NotificationType.SHARING_SESSION_REVOKED,
                title = "Medical Sharing Session Revoked",
                message = "The patient has revoked access to their shared photos",
                relatedSessionId = sessionId,
                relatedProfessionalId = null,
                createdAt = Instant.now()
            )
            
            notificationRepository.createNotification(professionalNotification)
            notificationService.sendRealTimeNotification(session.professionalId, professionalNotification)
        }
        
        return SessionRevocationResult(
            success = revoked,
            sessionId = sessionId,
            revokedAt = if (revoked) Instant.now() else null,
            reason = reason
        )
    }
    
    suspend fun getPatientSharingSessions(patientId: UUID): List<MedicalSharingSessionSummary> {
        val sessions = medicalSharingRepository.findSessionsByPatientId(patientId)
        
        return sessions.map { session ->
            val accessHistory = doctorAccessRepository.findActiveAccessSessionsByMedicalSession(session.id)
            val lastAccess = accessHistory.maxByOrNull { it.startedAt }
            
            MedicalSharingSessionSummary(
                session = session,
                totalAccesses = accessHistory.size,
                lastAccessedAt = lastAccess?.startedAt,
                remainingViews = maxOf(0, session.maxTotalViews - accessHistory.size),
                timeUntilExpiry = if (session.expiresAt.isAfter(Instant.now())) {
                    ChronoUnit.HOURS.between(Instant.now(), session.expiresAt)
                } else 0
            )
        }
    }
    
    suspend fun getProfessionalAccessibleSessions(professionalId: UUID): List<ProfessionalAccessibleSession> {
        val sessions = medicalSharingRepository.findSessionsByProfessionalId(professionalId)
        
        return sessions.filter { 
            it.status in listOf(MedicalSharingStatus.PENDING_DOCTOR_ACCESS, MedicalSharingStatus.ACTIVE) &&
            it.expiresAt.isAfter(Instant.now())
        }.map { session ->
            val accessHistory = doctorAccessRepository.findActiveAccessSessionsByMedicalSession(session.id)
            
            ProfessionalAccessibleSession(
                session = session,
                canAccess = accessHistory.size < session.maxTotalViews,
remainingViews = maxOf(0, session.maxTotalViews - accessHistory.size),
               urgency = calculateUrgency(session),
               patientInfo = getAnonymizedPatientInfo(session.patientId)
           )
       }
   }
   
   private suspend fun validatePhotosOwnership(patientId: UUID, photoIds: List<UUID>): List<com.hairhealth.platform.domain.PhotoMetadata> {
       val photos = photoIds.map { photoId ->
           photoMetadataRepository.findById(photoId)
               ?: throw IllegalArgumentException("Photo $photoId not found")
       }
       
       photos.forEach { photo ->
           if (photo.userId != patientId) {
               throw IllegalArgumentException("Unauthorized access to photo ${photo.id}")
           }
       }
       
       return photos
   }
   
   private suspend fun generateEphemeralKey(
       session: MedicalSharingSession,
       photo: com.hairhealth.platform.domain.PhotoMetadata
   ): EphemeralDecryptionKey {
       
       val keyData = encryptionService.generateEphemeralDecryptionKey(
           sessionId = session.id,
           photoId = photo.id,
           professionalId = session.professionalId,
           validityMinutes = session.maxViewDurationMinutes
       )
       
       val ephemeralKey = EphemeralDecryptionKey(
           id = UUID.randomUUID(),
           sessionId = session.id,
           photoId = photo.id,
           professionalId = session.professionalId,
           encryptedKey = keyData.encryptedKey,
           keyDerivationParams = keyData.derivationParams,
           maxUses = 1, // Single use for maximum security
           currentUses = 0,
           expiresAt = session.expiresAt,
           createdAt = Instant.now(),
           isRevoked = false
       )
       
       return ephemeralKeyRepository.createKey(ephemeralKey)
   }
   
   private fun validateDoctorAccess(session: MedicalSharingSession, professionalId: UUID) {
       if (session.professionalId != professionalId) {
           throw IllegalArgumentException("Unauthorized access to medical session")
       }
       
       if (session.status == MedicalSharingStatus.REVOKED_BY_PATIENT) {
           throw IllegalArgumentException("Session has been revoked by patient")
       }
       
       if (session.status == MedicalSharingStatus.EXPIRED) {
           throw IllegalArgumentException("Session has expired")
       }
       
       if (session.expiresAt.isBefore(Instant.now())) {
           throw IllegalArgumentException("Session has expired")
       }
   }
   
   private fun validateAccessSession(accessSession: DoctorAccessSession) {
       if (!accessSession.isActive) {
           throw IllegalArgumentException("Access session is not active")
       }
       
       if (accessSession.expiresAt.isBefore(Instant.now())) {
           throw IllegalArgumentException("Access session has expired")
       }
   }
   
   private suspend fun updateSessionStatus(session: MedicalSharingSession, newStatus: MedicalSharingStatus) {
       val updatedSession = session.copy(
           status = newStatus,
           updatedAt = Instant.now()
       )
       medicalSharingRepository.updateSession(updatedSession)
   }
   
   private fun generateSecureAccessUrl(sessionId: UUID): String {
       return "/secure-medical-access/$sessionId"
   }
   
   private fun generateSecureViewerUrl(accessSessionId: UUID): String {
       return "/secure-viewer/$accessSessionId"
   }
   
   private suspend fun getPhotosForAccess(session: MedicalSharingSession): List<SecurePhotoInfo> {
       return session.photoIds.map { photoId ->
           val photo = photoMetadataRepository.findById(photoId)!!
           SecurePhotoInfo(
               photoId = photoId,
               angle = photo.angle,
               captureDate = photo.captureDate,
               filename = photo.filename // Encrypted filename
           )
       }
   }
   
   private fun createAccessRestrictions(session: MedicalSharingSession): AccessRestrictions {
       return AccessRestrictions(
           allowScreenshots = session.allowScreenshots,
           allowDownload = session.allowDownload,
           allowPrint = false,
           requireContinuousAuth = true,
           maxViewTimeMinutes = session.maxViewDurationMinutes
       )
   }
   
   private fun createViewingRestrictions(session: MedicalSharingSession): ViewingRestrictions {
       return ViewingRestrictions(
           preventScreenshots = !session.allowScreenshots,
           preventDownload = !session.allowDownload,
           preventPrint = true,
           preventCopy = true,
           maxViewTimeSeconds = session.maxViewDurationMinutes * 60,
           requireContinuousAuth = true,
           allowedDeviceFingerprint = null // Will be set by client
       )
   }
   
   private fun calculateUrgency(session: MedicalSharingSession): SessionUrgency {
       val hoursUntilExpiry = ChronoUnit.HOURS.between(Instant.now(), session.expiresAt)
       return when {
           hoursUntilExpiry <= 2 -> SessionUrgency.CRITICAL
           hoursUntilExpiry <= 6 -> SessionUrgency.HIGH
           hoursUntilExpiry <= 24 -> SessionUrgency.MEDIUM
           else -> SessionUrgency.LOW
       }
   }
   
   private suspend fun getAnonymizedPatientInfo(patientId: UUID): AnonymizedPatientInfo {
       // In real implementation, this would get basic patient info without PII
       return AnonymizedPatientInfo(
           patientInitials = "P.D.", // Anonymized
           ageRange = "25-30",
           shareDate = Instant.now()
       )
   }
}

// Advanced Encryption Service for medical sharing
@Service
class AdvancedEncryptionService {
   
   fun generateEphemeralDecryptionKey(
       sessionId: UUID,
       photoId: UUID,
       professionalId: UUID,
       validityMinutes: Int
   ): EphemeralKeyData {
       
       // Generate time-boxed key with specific session/photo/professional binding
       val keyMaterial = generateSecureKeyMaterial()
       val derivationParams = createDerivationParams(sessionId, photoId, professionalId)
       
       // Encrypt the key material with time-limited parameters
       val encryptedKey = encryptKeyMaterial(keyMaterial, derivationParams, validityMinutes)
       
       return EphemeralKeyData(
           encryptedKey = encryptedKey,
           derivationParams = derivationParams,
           validityMinutes = validityMinutes
       )
   }
   
   fun generateTimeboxedDecryptionToken(
       ephemeralKey: EphemeralDecryptionKey,
       maxViewTimeMinutes: Int
   ): TimeboxedDecryptionToken {
       
       val tokenId = UUID.randomUUID()
       val expiresAt = Instant.now().plus(maxViewTimeMinutes.toLong(), ChronoUnit.MINUTES)
       
       // Create a time-limited token that can decrypt the ephemeral key
       val tokenData = createTokenData(ephemeralKey, tokenId, expiresAt)
       
       return TimeboxedDecryptionToken(
           tokenId = tokenId,
           encryptedTokenData = tokenData,
           expiresAt = expiresAt,
           maxUses = 1,
           bindToSession = ephemeralKey.sessionId
       )
   }
   
   private fun generateSecureKeyMaterial(): ByteArray {
       // Generate cryptographically secure random key material
       val keyGen = javax.crypto.KeyGenerator.getInstance("AES")
       keyGen.init(256)
       return keyGen.generateKey().encoded
   }
   
   private fun createDerivationParams(sessionId: UUID, photoId: UUID, professionalId: UUID): String {
       // Create deterministic but unique parameters for key derivation
       return "$sessionId:$photoId:$professionalId:${System.currentTimeMillis()}"
   }
   
   private fun encryptKeyMaterial(keyMaterial: ByteArray, derivationParams: String, validityMinutes: Int): String {
       // Encrypt key material with time-limited validity
       // In real implementation, this would use proper cryptographic functions
       return java.util.Base64.getEncoder().encodeToString(keyMaterial + derivationParams.toByteArray())
   }
   
   private fun createTokenData(ephemeralKey: EphemeralDecryptionKey, tokenId: UUID, expiresAt: Instant): String {
       // Create token that can decrypt the ephemeral key
       val tokenPayload = mapOf(
           "keyId" to ephemeralKey.id.toString(),
           "sessionId" to ephemeralKey.sessionId.toString(),
           "photoId" to ephemeralKey.photoId.toString(),
           "tokenId" to tokenId.toString(),
           "expiresAt" to expiresAt.toString()
       )
       
       // In real implementation, this would be cryptographically signed and encrypted
       return java.util.Base64.getEncoder().encodeToString(
           tokenPayload.toString().toByteArray()
       )
   }
}

// Real-time notification service
@Service
class RealTimeNotificationService {
   
   suspend fun sendRealTimeNotification(recipientId: UUID, notification: MedicalNotification) {
       // In real implementation, this would use WebSockets, SSE, or push notifications
       println("Real-time notification sent to $recipientId: ${notification.title}")
       
       // Could integrate with:
       // - WebSocket connections
       // - Server-Sent Events (SSE)
       // - Push notification services (FCM, APNS)
       // - Email notifications for critical events
       // - SMS for urgent security alerts
   }
   
   suspend fun sendSecurityAlert(recipientId: UUID, alertType: SecurityAlertType, details: String) {
       // Send immediate security alerts for suspicious activity
       println("SECURITY ALERT for $recipientId: $alertType - $details")
       
       // In real implementation:
       // - Immediate push notification
       // - SMS alert
       // - Email notification
       // - In-app banner/modal
   }
}
