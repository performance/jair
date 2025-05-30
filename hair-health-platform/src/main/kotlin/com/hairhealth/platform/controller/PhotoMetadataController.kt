package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.PhotoAngle
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.PhotoMetadataService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/v1/me/progress-photos")
class PhotoMetadataController(
    private val photoMetadataService: PhotoMetadataService
) {

    @PostMapping("/upload-url")
    suspend fun requestUploadUrl(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: PhotoUploadRequest
    ): PhotoUploadResponse {
        val uploadSession = photoMetadataService.createPhotoMetadata(
            userId = userPrincipal.userId,
            filename = request.filename,
            angle = request.angle,
            captureDate = request.captureDate,
            encryptionKeyInfo = request.encryptionKeyInfo
        )

        return PhotoUploadResponse(
            photoMetadataId = uploadSession.photoMetadataId,
            uploadUrl = uploadSession.uploadUrl,
            expiresAt = uploadSession.expiresAt.toString()
        )
    }

    @GetMapping
    suspend fun getProgressPhotos(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(required = false) angle: PhotoAngle?,
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: Instant?
    ): List<PhotoMetadataResponse> {
        val photos = when {
            startDate != null && endDate != null -> {
                photoMetadataService.getPhotosByDateRange(userPrincipal.userId, startDate, endDate, angle)
            }
            angle != null -> {
                photoMetadataService.getPhotosByUserIdAndAngle(userPrincipal.userId, angle)
            }
            else -> {
                photoMetadataService.getPhotosByUserId(userPrincipal.userId, limit, offset)
            }
        }

        return photos.map { it.toResponse() }
    }

    @GetMapping("/{photoMetadataId}")
    suspend fun getPhotoMetadata(@PathVariable photoMetadataId: UUID): PhotoMetadataResponse? {
        val photo = photoMetadataService.getPhotoMetadataById(photoMetadataId)
        return photo?.toResponse()
    }

    @GetMapping("/{photoMetadataId}/view-url")
    suspend fun getViewUrl(@PathVariable photoMetadataId: UUID): PhotoViewResponse {
        val viewSession = photoMetadataService.generateViewUrl(photoMetadataId)
        
        return PhotoViewResponse(
            downloadUrl = viewSession.downloadUrl,
            encryptionKeyInfo = viewSession.encryptionKeyInfo,
            expiresAt = viewSession.expiresAt.toString()
        )
    }

    @DeleteMapping("/{photoMetadataId}")
    suspend fun deletePhoto(
        @PathVariable photoMetadataId: UUID,
        @RequestParam(defaultValue = "false") hardDelete: Boolean
    ): Map<String, String> {
        val deleted = photoMetadataService.deletePhoto(photoMetadataId, hardDelete)
        return if (deleted) {
            mapOf("status" to if (hardDelete) "permanently_deleted" else "deleted")
        } else {
            mapOf("status" to "not_found")
        }
    }

    @GetMapping("/stats")
    suspend fun getPhotoStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): PhotoStatsResponse {
        val stats = photoMetadataService.getPhotoStats(userPrincipal.userId)
        return PhotoStatsResponse(
            totalPhotos = stats.totalPhotos,
            photosByAngle = stats.photosByAngle,
            latestPhotosByAngle = stats.latestPhotosByAngle.mapValues { it.value.toResponse() },
            oldestPhotoDate = stats.oldestPhotoDate?.toString(),
            newestPhotoDate = stats.newestPhotoDate?.toString(),
            totalStorageUsedBytes = stats.totalStorageUsed
        )
    }

    @PostMapping("/{photoMetadataId}/finalize")
    suspend fun finalizePhotoUpload(
        @PathVariable photoMetadataId: UUID,
        @RequestBody request: FinalizeUploadRequest
    ): PhotoMetadataResponse {
        val photoMetadata = photoMetadataService.finalizePhotoUpload(
            photoId = photoMetadataId,
            fileSize = request.fileSize
        )

        return photoMetadata.toResponse()
    }
}

// Request/Response DTOs
data class PhotoUploadRequest(
    val filename: String,
    val angle: PhotoAngle,
    val captureDate: Instant,
    val encryptionKeyInfo: String
)

data class FinalizeUploadRequest(
    val fileSize: Long
)

data class PhotoUploadResponse(
    val photoMetadataId: UUID,
    val uploadUrl: String,
    val expiresAt: String
)

data class PhotoViewResponse(
    val downloadUrl: String,
    val encryptionKeyInfo: String,
    val expiresAt: String
)

data class PhotoMetadataResponse(
    val id: UUID,
    val userId: UUID,
    val filename: String,
    val angle: PhotoAngle,
    val captureDate: String,
    val fileSize: Long?,
    val uploadedAt: String,
    val isDeleted: Boolean
)

data class PhotoStatsResponse(
    val totalPhotos: Long,
    val photosByAngle: Map<PhotoAngle, Int>,
    val latestPhotosByAngle: Map<PhotoAngle, PhotoMetadataResponse>,
    val oldestPhotoDate: String?,
    val newestPhotoDate: String?,
    val totalStorageUsedBytes: Long
)

// Extension function
private fun com.hairhealth.platform.domain.PhotoMetadata.toResponse() = PhotoMetadataResponse(
    id = this.id,
    userId = this.userId,
    filename = this.filename,
    angle = this.angle,
    captureDate = this.captureDate.toString(),
    fileSize = this.fileSize,
    uploadedAt = this.uploadedAt.toString(),
    isDeleted = this.isDeleted
)