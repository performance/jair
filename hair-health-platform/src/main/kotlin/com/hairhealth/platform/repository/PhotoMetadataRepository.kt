package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.PhotoAngle
import com.hairhealth.platform.domain.PhotoMetadata
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface PhotoMetadataRepository {
    suspend fun create(photoMetadata: PhotoMetadata): PhotoMetadata
    suspend fun findById(id: UUID): PhotoMetadata?
    suspend fun findByUserId(userId: UUID, limit: Int = 50, offset: Int = 0): List<PhotoMetadata>
    suspend fun findByUserIdAndAngle(userId: UUID, angle: PhotoAngle): List<PhotoMetadata>
    suspend fun findByUserIdAndDateRange(
        userId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<PhotoMetadata>
    suspend fun findByUserIdAngleAndDateRange(
        userId: UUID,
        angle: PhotoAngle,
        startDate: Instant,
        endDate: Instant
    ): List<PhotoMetadata>
    suspend fun update(photoMetadata: PhotoMetadata): PhotoMetadata
    suspend fun markAsDeleted(id: UUID): Boolean
    suspend fun delete(id: UUID): Boolean // Hard delete
    suspend fun countByUserId(userId: UUID, includeDeleted: Boolean = false): Long
    suspend fun findLatestByUserIdAndAngle(userId: UUID, angle: PhotoAngle): PhotoMetadata?
}