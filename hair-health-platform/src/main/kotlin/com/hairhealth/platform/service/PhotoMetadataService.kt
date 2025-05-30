package com.hairhealth.platform.service

import com.hairhealth.platform.domain.PhotoAngle
import com.hairhealth.platform.domain.PhotoMetadata
import com.hairhealth.platform.repository.PhotoMetadataRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class PhotoMetadataService(
    private val photoMetadataRepository: PhotoMetadataRepository
) {

    suspend fun createPhotoMetadata(
        userId: UUID,
        filename: String,
        angle: PhotoAngle,
        captureDate: Instant,
        encryptionKeyInfo: String
    ): PhotoUploadSession {
        val photoId = UUID.randomUUID()
        val blobPath = generateBlobPath(userId, photoId, filename)
        val uploadUrl = generateUploadUrl(blobPath) // Mock for now
        
        val photoMetadata = PhotoMetadata(
            id = photoId,
            userId = userId,
            filename = filename,
            angle = angle,
            captureDate = captureDate,
            fileSize = null, // Will be updated after upload
            encryptionKeyInfo = encryptionKeyInfo,
            blobPath = blobPath,
            uploadedAt = Instant.now(),
            isDeleted = false
        )

        val created = photoMetadataRepository.create(photoMetadata)
        
        return PhotoUploadSession(
            photoMetadataId = created.id,
            uploadUrl = uploadUrl,
            blobPath = blobPath,
            expiresAt = Instant.now().plusSeconds(3600) // 1 hour expiry
        )
    }

    suspend fun finalizePhotoUpload(photoId: UUID, fileSize: Long): PhotoMetadata {
        val existing = photoMetadataRepository.findById(photoId)
            ?: throw IllegalArgumentException("Photo metadata not found")

        val updated = existing.copy(fileSize = fileSize)
        return photoMetadataRepository.update(updated)
    }

    suspend fun getPhotoMetadataById(id: UUID): PhotoMetadata? {
        return photoMetadataRepository.findById(id)
    }

    suspend fun getPhotosByUserId(
        userId: UUID,
        limit: Int = 50,
        offset: Int = 0
    ): List<PhotoMetadata> {
        return photoMetadataRepository.findByUserId(userId, limit, offset)
    }

    suspend fun getPhotosByUserIdAndAngle(userId: UUID, angle: PhotoAngle): List<PhotoMetadata> {
        return photoMetadataRepository.findByUserIdAndAngle(userId, angle)
    }

    suspend fun getPhotosByDateRange(
        userId: UUID,
        startDate: Instant,
        endDate: Instant,
        angle: PhotoAngle? = null
    ): List<PhotoMetadata> {
        return if (angle != null) {
            photoMetadataRepository.findByUserIdAngleAndDateRange(userId, angle, startDate, endDate)
        } else {
            photoMetadataRepository.findByUserIdAndDateRange(userId, startDate, endDate)
        }
    }

    suspend fun generateViewUrl(photoId: UUID): PhotoViewSession {
        val photoMetadata = photoMetadataRepository.findById(photoId)
            ?: throw IllegalArgumentException("Photo not found")

        val viewUrl = generateDownloadUrl(photoMetadata.blobPath) // Mock for now

        return PhotoViewSession(
            photoMetadataId = photoId,
            downloadUrl = viewUrl,
            encryptionKeyInfo = photoMetadata.encryptionKeyInfo,
            expiresAt = Instant.now().plusSeconds(1800) // 30 minutes expiry
        )
    }

    suspend fun deletePhoto(photoId: UUID, hardDelete: Boolean = false): Boolean {
        return if (hardDelete) {
            photoMetadataRepository.delete(photoId)
        } else {
            photoMetadataRepository.markAsDeleted(photoId)
        }
    }

    suspend fun getPhotoStats(userId: UUID): PhotoStats {
        val totalPhotos = photoMetadataRepository.countByUserId(userId, includeDeleted = false)
        val allPhotos = photoMetadataRepository.findByUserId(userId, limit = 1000)
        
        val photosByAngle = allPhotos.groupBy { it.angle }.mapValues { it.value.size }
        val latestPhotosByAngle = PhotoAngle.values().associateWith { angle ->
            photoMetadataRepository.findLatestByUserIdAndAngle(userId, angle)
        }.filterValues { it != null }

        val oldestPhoto = allPhotos.minByOrNull { it.captureDate }
        val newestPhoto = allPhotos.maxByOrNull { it.captureDate }

        return PhotoStats(
            totalPhotos = totalPhotos,
            photosByAngle = photosByAngle,
            latestPhotosByAngle = latestPhotosByAngle.mapValues { it.value!! },
            oldestPhotoDate = oldestPhoto?.captureDate,
            newestPhotoDate = newestPhoto?.captureDate,
            totalStorageUsed = allPhotos.mapNotNull { it.fileSize }.sum()
        )
    }

    private fun generateBlobPath(userId: UUID, photoId: UUID, filename: String): String {
        return "photos/$userId/$photoId/$filename"
    }

    private fun generateUploadUrl(blobPath: String): String {
        // Mock implementation - in real system this would generate GCS pre-signed URL
        return "https://mock-storage.example.com/upload/$blobPath?token=mock-upload-token"
    }

    private fun generateDownloadUrl(blobPath: String): String {
        // Mock implementation - in real system this would generate GCS pre-signed URL
        return "https://mock-storage.example.com/download/$blobPath?token=mock-download-token"
    }
}

data class PhotoUploadSession(
    val photoMetadataId: UUID,
    val uploadUrl: String,
    val blobPath: String,
    val expiresAt: Instant
)

data class PhotoViewSession(
    val photoMetadataId: UUID,
    val downloadUrl: String,
    val encryptionKeyInfo: String,
    val expiresAt: Instant
)

data class PhotoStats(
    val totalPhotos: Long,
    val photosByAngle: Map<PhotoAngle, Int>,
    val latestPhotosByAngle: Map<PhotoAngle, PhotoMetadata>,
    val oldestPhotoDate: Instant?,
    val newestPhotoDate: Instant?,
    val totalStorageUsed: Long
)