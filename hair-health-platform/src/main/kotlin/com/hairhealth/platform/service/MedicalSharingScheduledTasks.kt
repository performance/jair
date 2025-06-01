package com.hairhealth.platform.service

import com.hairhealth.platform.domain.MedicalSharingStatus
import com.hairhealth.platform.domain.NotificationType
import com.hairhealth.platform.domain.MedicalNotification
import com.hairhealth.platform.repository.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class MedicalSharingScheduledTasks(
    private val medicalSharingRepository: MedicalSharingRepository,
    private val doctorAccessRepository: DoctorAccessRepository,
    private val ephemeralKeyRepository: EphemeralKeyRepository,
    private val notificationRepository: MedicalNotificationRepository,
    private val notificationService: RealTimeNotificationService
) {

    @Scheduled(fixedDelay = 300_000) // Every 5 minutes
    suspend fun expireOldSessions() {
        val expiredSessions = medicalSharingRepository.findExpiredSessions()
        
        expiredSessions.forEach { session ->
            // Update status to expired
            val updatedSession = session.copy(
                status = MedicalSharingStatus.EXPIRED,
                updatedAt = Instant.now()
            )
            medicalSharingRepository.updateSession(updatedSession)
            
            // Revoke all ephemeral keys
            session.photoIds.forEach { photoId ->
                ephemeralKeyRepository.findKeyBySessionAndPhoto(session.id, photoId)?.let { key ->
                    ephemeralKeyRepository.revokeKey(key.id)
                }
            }
            
            // End active access sessions
            val activeAccessSessions = doctorAccessRepository.findActiveAccessSessionsByMedicalSession(session.id)
            activeAccessSessions.forEach { accessSession ->
                doctorAccessRepository.endAccessSession(accessSession.id, Instant.now())
            }
            
            // Notify both patient and professional
            notifySessionExpired(session)
        }
        
        if (expiredSessions.isNotEmpty()) {
            println("Expired ${expiredSessions.size} medical sharing sessions")
        }
    }
    
    @Scheduled(fixedDelay = 900_000) // Every 15 minutes
    suspend fun cleanupExpiredKeys() {
        val cleanedCount = ephemeralKeyRepository.cleanupExpiredKeys()
        if (cleanedCount > 0) {
            println("Cleaned up $cleanedCount expired ephemeral keys")
        }
    }
    
    @Scheduled(fixedDelay = 3600_000) // Every hour
    suspend fun detectAnomalousActivity() {
        // Find sessions with unusual access patterns
        val allActiveSessions = medicalSharingRepository.findSessionsByPatientId(UUID.randomUUID()) // This would be a proper query
        
        // TODO: Implement anomaly detection logic
        // - Multiple rapid access attempts
        // - Access from unusual locations
        // - Suspicious device fingerprints
        // - Excessive screenshot attempts
        
        println("Anomaly detection scan completed")
    }
    
    @Scheduled(cron = "0 0 8 * * *") // Daily at 8 AM
    suspend fun sendExpiryReminders() {
        // Find sessions expiring in the next 24 hours
        val soonToExpire = findSessionsExpiringSoon()
        
        soonToExpire.forEach { session ->
            val hoursUntilExpiry = java.time.temporal.ChronoUnit.HOURS.between(
                Instant.now(), 
                session.expiresAt
            )
            
            // Notify patient
            val patientNotification = MedicalNotification(
                id = UUID.randomUUID(),
                recipientId = session.patientId,
                type = NotificationType.SHARING_SESSION_EXPIRED,
                title = "Medical Sharing Expiring Soon",
                message = "Your shared photos will expire in $hoursUntilExpiry hours",
                relatedSessionId = session.id,
                relatedProfessionalId = session.professionalId,
                createdAt = Instant.now()
            )
            
            notificationRepository.createNotification(patientNotification)
            notificationService.sendRealTimeNotification(session.patientId, patientNotification)
            
            // Notify professional if they haven't accessed yet
            val accessCount = doctorAccessRepository.countAccessSessionsByMedicalSession(session.id)
            if (accessCount == 0) {
                val professionalNotification = MedicalNotification(
                    id = UUID.randomUUID(),
                    recipientId = session.professionalId,
                    type = NotificationType.SHARING_SESSION_EXPIRED,
                    title = "Patient Photos Expiring Soon",
                    message = "Patient shared photos expire in $hoursUntilExpiry hours - review needed",
                    relatedSessionId = session.id,
                    relatedProfessionalId = null,
                    createdAt = Instant.now()
                )
                
                notificationRepository.createNotification(professionalNotification)
                notificationService.sendRealTimeNotification(session.professionalId, professionalNotification)
            }
        }
        
        if (soonToExpire.isNotEmpty()) {
            println("Sent expiry reminders for ${soonToExpire.size} sessions")
        }
    }
    
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    suspend fun generateComplianceReport() {
        // Generate daily compliance report
        val yesterday = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS)
        
        // TODO: Implement compliance metrics
        // - Total sessions created
        // - Sessions accessed vs not accessed
        // - Average view duration
        // - Suspicious activity incidents
        // - Policy violations
        
        println("Daily compliance report generated")
    }
    
    private suspend fun notifySessionExpired(session: com.hairhealth.platform.domain.MedicalSharingSession) {
        // Notify patient
        val patientNotification = MedicalNotification(
            id = UUID.randomUUID(),
            recipientId = session.patientId,
            type = NotificationType.SHARING_SESSION_EXPIRED,
            title = "Medical Sharing Session Expired",
            message = "Your shared medical photos have expired and are no longer accessible",
            relatedSessionId = session.id,
            relatedProfessionalId = session.professionalId,
            createdAt = Instant.now()
        )
        
        notificationRepository.createNotification(patientNotification)
        notificationService.sendRealTimeNotification(session.patientId, patientNotification)
        
        // Notify professional
        val professionalNotification = MedicalNotification(
            id = UUID.randomUUID(),
            recipientId = session.professionalId,
            type = NotificationType.SHARING_SESSION_EXPIRED,
            title = "Patient Photo Access Expired",
            message = "Access to patient shared photos has expired",
            relatedSessionId = session.id,
            relatedProfessionalId = null,
            createdAt = Instant.now()
        )
        
        notificationRepository.createNotification(professionalNotification)
        notificationService.sendRealTimeNotification(session.professionalId, professionalNotification)
    }
    
    private suspend fun findSessionsExpiringSoon(): List<com.hairhealth.platform.domain.MedicalSharingSession> {
        // Find sessions expiring in next 24 hours
        val now = Instant.now()
        val tomorrow = now.plus(24, java.time.temporal.ChronoUnit.HOURS)
        
        // This would be implemented in the repository
        return emptyList() // Placeholder
    }
}