package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.*
import java.time.Instant
import java.util.UUID

interface MedicalSharingRepository {
    suspend fun createSession(session: MedicalSharingSession): MedicalSharingSession
    suspend fun findSessionById(id: UUID): MedicalSharingSession?
    suspend fun findSessionsByPatientId(patientId: UUID): List<MedicalSharingSession>
    suspend fun findSessionsByProfessionalId(professionalId: UUID): List<MedicalSharingSession>
    suspend fun updateSession(session: MedicalSharingSession): MedicalSharingSession
    suspend fun revokeSession(id: UUID, reason: String): Boolean
    suspend fun findExpiredSessions(): List<MedicalSharingSession>
}

interface DoctorAccessRepository {
    suspend fun createAccessSession(session: DoctorAccessSession): DoctorAccessSession
    suspend fun findAccessSessionById(id: UUID): DoctorAccessSession?
    suspend fun findActiveAccessSessionsByMedicalSession(medicalSessionId: UUID): List<DoctorAccessSession>
    suspend fun endAccessSession(id: UUID, endedAt: Instant): Boolean
    suspend fun countAccessSessionsByMedicalSession(medicalSessionId: UUID): Int
}

interface ViewingEventRepository {
    suspend fun createViewingEvent(event: ViewingEvent): ViewingEvent
    suspend fun endViewingEvent(id: UUID, endedAt: Instant, durationSeconds: Long): Boolean
    suspend fun findEventsByAccessSession(accessSessionId: UUID): List<ViewingEvent>
    suspend fun findEventsByPhotoId(photoId: UUID): List<ViewingEvent>
    suspend fun recordSuspiciousActivity(eventId: UUID, type: String): Boolean
}

interface EphemeralKeyRepository {
    suspend fun createKey(key: EphemeralDecryptionKey): EphemeralDecryptionKey
    suspend fun findKeyBySessionAndPhoto(sessionId: UUID, photoId: UUID): EphemeralDecryptionKey?
    suspend fun incrementKeyUsage(keyId: UUID): Boolean
    suspend fun revokeKey(keyId: UUID): Boolean
    suspend fun cleanupExpiredKeys(): Int
}

interface MedicalNotificationRepository {
    suspend fun createNotification(notification: MedicalNotification): MedicalNotification
    suspend fun findNotificationsByRecipient(recipientId: UUID, unreadOnly: Boolean = false): List<MedicalNotification>
    suspend fun markAsRead(notificationId: UUID): Boolean
    suspend fun markAllAsRead(recipientId: UUID): Int
}