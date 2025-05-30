package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/me/medical-sharing")
class MedicalSharingController(
    private val medicalSharingService: CompleteMedicalSharingService
) {

    @PostMapping("/sessions")
    suspend fun createMedicalSharingSession(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateMedicalSharingControllerRequest
    ): CreateMedicalSharingResponse {
        
        // Convert controller DTO to service DTO
        val serviceRequest = com.hairhealth.platform.service.CreateMedicalSharingRequest(
            professionalId = request.professionalId,
            photoIds = request.photoIds,
            notes = request.notes,
            durationHours = request.durationHours,
            maxTotalViews = request.maxTotalViews,
            maxViewDurationMinutes = request.maxViewDurationMinutes
        )
        
        val result = medicalSharingService.createMedicalSharingSession(
            patientId = userPrincipal.userId,
            request = serviceRequest
        )
        
        return CreateMedicalSharingResponse(
            sessionId = result.session.id,
            professionalId = result.session.professionalId,
            photoCount = result.session.photoIds.size,
            maxTotalViews = result.session.maxTotalViews,
            maxViewDurationMinutes = result.session.maxViewDurationMinutes,
            expiresAt = result.session.expiresAt.toString(),
            secureAccessUrl = result.secureAccessUrl,
            status = result.session.status
        )
    }
    
    @GetMapping("/sessions")
    suspend fun getMedicalSharingSessions(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): List<MedicalSharingSessionSummaryResponse> {
        
        val sessions = medicalSharingService.getPatientSharingSessions(userPrincipal.userId)
        
        return sessions.map { summary ->
            MedicalSharingSessionSummaryResponse(
                sessionId = summary.session.id,
                professionalId = summary.session.professionalId,
                photoCount = summary.session.photoIds.size,
                status = summary.session.status,
                createdAt = summary.session.createdAt.toString(),
                expiresAt = summary.session.expiresAt.toString(),
                totalAccesses = summary.totalAccesses,
                lastAccessedAt = summary.lastAccessedAt?.toString(),
                remainingViews = summary.remainingViews,
                hoursUntilExpiry = summary.timeUntilExpiry
            )
        }
    }
    
    @PostMapping("/sessions/{sessionId}/revoke")
    suspend fun revokeMedicalSharingSession(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable sessionId: UUID,
        @RequestBody request: RevokeMedicalSharingRequest
    ): RevokeMedicalSharingResponse {
        
        val result = medicalSharingService.revokeMedicalSession(
            sessionId = sessionId,
            patientId = userPrincipal.userId,
            reason = request.reason
        )
        
        return RevokeMedicalSharingResponse(
            success = result.success,
            sessionId = result.sessionId,
            revokedAt = result.revokedAt?.toString(),
            reason = result.reason
        )
    }
    
    @GetMapping("/sessions/{sessionId}/access-log")
    suspend fun getAccessLog(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable sessionId: UUID
    ): List<AccessLogEntryResponse> {
        // Implementation would get access logs for this session
        return emptyList() // Placeholder
    }
}

@RestController
@RequestMapping("/api/v1/professionals/me/medical-access")
class ProfessionalMedicalAccessController(
    private val medicalSharingService: CompleteMedicalSharingService
) {

    @GetMapping("/sessions")
    suspend fun getAccessibleSessions(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): List<ProfessionalAccessibleSessionResponse> {
        
        val sessions = medicalSharingService.getProfessionalAccessibleSessions(userPrincipal.userId)
        
        return sessions.map { session ->
            ProfessionalAccessibleSessionResponse(
                sessionId = session.session.id,
                patientInfo = session.patientInfo,
                photoCount = session.session.photoIds.size,
                sharedAt = session.session.createdAt.toString(),
                expiresAt = session.session.expiresAt.toString(),
                canAccess = session.canAccess,
                remainingViews = session.remainingViews,
                urgency = session.urgency,
                notes = session.session.notes
            )
        }
    }

    @PostMapping("/sessions/{sessionId}/request-access")
    suspend fun requestAccess(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable sessionId: UUID,
        @RequestBody request: RequestAccessRequest
    ): DoctorAccessResponse {
        
        val deviceInfo = com.hairhealth.platform.service.DeviceInfo(
            fingerprint = request.deviceFingerprint,
            ipAddress = request.ipAddress ?: "unknown",
            userAgent = request.userAgent,
            screenResolution = request.screenResolution,
            timeZone = request.timeZone
        )
        
        val result = medicalSharingService.requestDoctorAccess(
            sessionId = sessionId,
            professionalId = userPrincipal.userId,
            deviceInfo = deviceInfo
        )
        
        return DoctorAccessResponse(
            accessSessionId = result.accessSession.id,
            secureViewerUrl = result.secureViewerUrl,
            maxDurationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(
                Instant.now(), 
                result.accessSession.expiresAt
            ).toInt(),
            expiresAt = result.accessSession.expiresAt.toString(),
            photos = result.photos.map { photo ->
                SecurePhotoInfoResponse(
                    photoId = photo.photoId,
                    angle = photo.angle,
                    captureDate = photo.captureDate.toString(),
                    filename = photo.filename
                )
            },
            restrictions = AccessRestrictionsResponse(
                allowScreenshots = result.restrictions.allowScreenshots,
                allowDownload = result.restrictions.allowDownload,
                allowPrint = result.restrictions.allowPrint,
                maxViewTimeMinutes = result.restrictions.maxViewTimeMinutes,
                requireContinuousAuth = result.restrictions.requireContinuousAuth
            )
        )
    }
}

// Controller-level Request/Response DTOs (different from service DTOs)
data class CreateMedicalSharingControllerRequest(
    val professionalId: UUID,
    val photoIds: List<UUID>,
    val notes: String?,
    val durationHours: Long = 24,
    val maxTotalViews: Int = 3,
    val maxViewDurationMinutes: Int = 5
)

data class CreateMedicalSharingResponse(
    val sessionId: UUID,
    val professionalId: UUID,
    val photoCount: Int,
    val maxTotalViews: Int,
    val maxViewDurationMinutes: Int,
    val expiresAt: String,
    val secureAccessUrl: String,
    val status: MedicalSharingStatus
)

data class MedicalSharingSessionSummaryResponse(
    val sessionId: UUID,
    val professionalId: UUID,
    val photoCount: Int,
    val status: MedicalSharingStatus,
    val createdAt: String,
    val expiresAt: String,
    val totalAccesses: Int,
    val lastAccessedAt: String?,
    val remainingViews: Int,
    val hoursUntilExpiry: Long
)

data class ProfessionalAccessibleSessionResponse(
    val sessionId: UUID,
    val patientInfo: AnonymizedPatientInfo,
    val photoCount: Int,
    val sharedAt: String,
    val expiresAt: String,
    val canAccess: Boolean,
    val remainingViews: Int,
    val urgency: SessionUrgency,
    val notes: String?
)

data class RequestAccessRequest(
    val deviceFingerprint: String,
    val userAgent: String,
    val ipAddress: String?,
    val screenResolution: String?,
    val timeZone: String?
)

data class DoctorAccessResponse(
    val accessSessionId: UUID,
    val secureViewerUrl: String,
    val maxDurationMinutes: Int,
    val expiresAt: String,
    val photos: List<SecurePhotoInfoResponse>,
    val restrictions: AccessRestrictionsResponse
)

data class SecurePhotoInfoResponse(
    val photoId: UUID,
    val angle: com.hairhealth.platform.domain.PhotoAngle,
    val captureDate: String,
    val filename: String
)

data class AccessRestrictionsResponse(
    val allowScreenshots: Boolean,
    val allowDownload: Boolean,
    val allowPrint: Boolean,
    val maxViewTimeMinutes: Int,
    val requireContinuousAuth: Boolean
)

data class RevokeMedicalSharingRequest(
    val reason: String
)

data class RevokeMedicalSharingResponse(
    val success: Boolean,
    val sessionId: UUID,
    val revokedAt: String?,
    val reason: String
)

data class AccessLogEntryResponse(
    val timestamp: String,
    val professionalId: UUID,
    val action: String,
    val duration: String?,
    val deviceInfo: String,
    val ipAddress: String,
    val status: String
)