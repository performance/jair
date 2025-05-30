package com.hairhealth.platform.service

import com.hairhealth.platform.domain.*
import java.time.Instant
import java.util.UUID

// Service-level DTOs (used between controller and service)
data class CreateMedicalSharingRequest(
    val professionalId: UUID,
    val photoIds: List<UUID>,
    val notes: String?,
    val durationHours: Long = 24,
    val maxTotalViews: Int = 3,
    val maxViewDurationMinutes: Int = 5
)

data class DeviceInfo(
    val fingerprint: String,
    val ipAddress: String,
    val userAgent: String,
    val screenResolution: String? = null,
    val timeZone: String? = null
)

data class MedicalSharingSessionResult(
    val session: MedicalSharingSession,
    val ephemeralKeys: List<EphemeralDecryptionKey>,
    val secureAccessUrl: String
)

data class DoctorAccessResult(
    val accessSession: DoctorAccessSession,
    val secureViewerUrl: String,
    val photos: List<SecurePhotoInfo>,
    val restrictions: AccessRestrictions
)

data class PhotoViewingResult(
    val viewingEventId: UUID,
    val decryptionToken: TimeboxedDecryptionToken,
    val maxViewTimeSeconds: Int,
    val restrictions: ViewingRestrictions
)

data class ViewingEndResult(
    val success: Boolean,
    val finalDurationSeconds: Long,
    val endReason: ViewingEndReason
)

data class SuspiciousActivityResult(
    val recorded: Boolean,
    val activityType: SuspiciousActivityType,
    val autoRevokeTriggered: Boolean
)

data class SessionRevocationResult(
    val success: Boolean,
    val sessionId: UUID,
    val revokedAt: Instant?,
    val reason: String
)

data class MedicalSharingSessionSummary(
    val session: MedicalSharingSession,
    val totalAccesses: Int,
    val lastAccessedAt: Instant?,
    val remainingViews: Int,
    val timeUntilExpiry: Long
)

data class ProfessionalAccessibleSession(
    val session: MedicalSharingSession,
    val canAccess: Boolean,
    val remainingViews: Int,
    val urgency: SessionUrgency,
    val patientInfo: AnonymizedPatientInfo
)

data class SecurePhotoInfo(
    val photoId: UUID,
    val angle: com.hairhealth.platform.domain.PhotoAngle,
    val captureDate: Instant,
    val filename: String
)

data class AccessRestrictions(
    val allowScreenshots: Boolean = false,
    val allowDownload: Boolean = false,
    val allowPrint: Boolean = false,
    val requireContinuousAuth: Boolean = true,
    val maxViewTimeMinutes: Int = 5
)

data class ViewingRestrictions(
    val preventScreenshots: Boolean,
    val preventDownload: Boolean,
    val preventPrint: Boolean,
    val preventCopy: Boolean,
    val maxViewTimeSeconds: Int,
    val requireContinuousAuth: Boolean,
    val allowedDeviceFingerprint: String?
)

data class EphemeralKeyData(
    val encryptedKey: String,
    val derivationParams: String,
    val validityMinutes: Int
)

data class TimeboxedDecryptionToken(
    val tokenId: UUID,
    val encryptedTokenData: String,
    val expiresAt: Instant,
    val maxUses: Int,
    val bindToSession: UUID
)

data class AnonymizedPatientInfo(
    val patientInitials: String,
    val ageRange: String,
    val shareDate: Instant
)

enum class ViewingEndReason {
    USER_CLOSED,
    TIME_EXPIRED,
    SUSPICIOUS_ACTIVITY,
    SESSION_REVOKED,
    TECHNICAL_ERROR
}

enum class SuspiciousActivityType {
    SCREENSHOT_ATTEMPT,
    MULTIPLE_SCREENSHOT_ATTEMPTS,
    DOWNLOAD_ATTEMPT,
    PRINT_ATTEMPT,
    COPY_ATTEMPT,
    SCREEN_RECORDING_DETECTED,
    UNUSUAL_DEVICE_BEHAVIOR,
    RAPID_NAVIGATION,
    MULTIPLE_DEVICE_ACCESS
}

enum class SessionUrgency {
    CRITICAL, HIGH, MEDIUM, LOW
}

enum class SecurityAlertType {
    SUSPICIOUS_ACCESS,
    UNAUTHORIZED_ATTEMPT,
    POLICY_VIOLATION,
    TECHNICAL_BREACH
}