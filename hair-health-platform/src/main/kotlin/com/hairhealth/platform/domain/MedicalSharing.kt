package com.hairhealth.platform.domain

import java.time.Instant
import java.util.UUID

data class MedicalSharingSession(
    val id: UUID,
    val patientId: UUID,
    val professionalId: UUID,
    val photoIds: List<UUID>,
    val notes: String?,
    val maxTotalViews: Int,
    val maxViewDurationMinutes: Int,
    val expiresAt: Instant,
    val allowScreenshots: Boolean = false,
    val allowDownload: Boolean = false,
    val status: MedicalSharingStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val revokedAt: Instant? = null,
    val revokedReason: String? = null
)

data class DoctorAccessSession(
    val id: UUID,
    val medicalSessionId: UUID,
    val professionalId: UUID,
    val deviceFingerprint: String,
    val ipAddress: String,
    val userAgent: String,
    val startedAt: Instant,
    val expiresAt: Instant,
    val endedAt: Instant? = null,
    val isActive: Boolean = true,
    val viewingEvents: List<ViewingEvent> = emptyList()
)

data class ViewingEvent(
    val id: UUID,
    val accessSessionId: UUID,
    val photoId: UUID,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val durationSeconds: Long? = null,
    val deviceFingerprint: String,
    val ipAddress: String,
    val screenshotAttempts: Int = 0,
    val downloadAttempts: Int = 0,
    val suspiciousActivity: Boolean = false
)

data class MedicalNotification(
    val id: UUID,
    val recipientId: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val relatedSessionId: UUID?,
    val relatedProfessionalId: UUID?,
    val isRead: Boolean = false,
    val createdAt: Instant,
    val readAt: Instant? = null
)

data class EphemeralDecryptionKey(
    val id: UUID,
    val sessionId: UUID,
    val photoId: UUID,
    val professionalId: UUID,
    val encryptedKey: String,
    val keyDerivationParams: String,
    val maxUses: Int,
    val currentUses: Int = 0,
    val expiresAt: Instant,
    val createdAt: Instant,
    val lastUsedAt: Instant? = null,
    val isRevoked: Boolean = false
)

enum class MedicalSharingStatus {
    PENDING_DOCTOR_ACCESS,
    ACTIVE,
    EXPIRED,
    REVOKED_BY_PATIENT,
    EXHAUSTED_ATTEMPTS,
    COMPLETED
}

enum class NotificationType {
    MEDICAL_SHARING_CREATED,
    DOCTOR_ACCESSED_PHOTOS,
    DOCTOR_VIEWING_STARTED,
    DOCTOR_VIEWING_ENDED,
    SHARING_SESSION_EXPIRED,
    SHARING_SESSION_REVOKED,
    SUSPICIOUS_ACCESS_DETECTED
}