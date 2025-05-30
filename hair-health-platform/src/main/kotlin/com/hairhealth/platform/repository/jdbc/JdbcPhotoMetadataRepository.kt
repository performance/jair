package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.PhotoAngle
import com.hairhealth.platform.domain.PhotoMetadata
import com.hairhealth.platform.repository.PhotoMetadataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Repository
class JdbcPhotoMetadataRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : PhotoMetadataRepository {

    override suspend fun create(photoMetadata: PhotoMetadata): PhotoMetadata = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO photo_metadata (
                id, user_id, filename, angle, capture_date, file_size,
                encryption_key_info, blob_path, uploaded_at, is_deleted
            )
            VALUES (
                :id, :userId, :filename, :angle, :captureDate, :fileSize,
                :encryptionKeyInfo, :blobPath, :uploadedAt, :isDeleted
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", photoMetadata.id)
            .addValue("userId", photoMetadata.userId)
            .addValue("filename", photoMetadata.filename)
            .addValue("angle", photoMetadata.angle.name)
            .addValue("captureDate", Timestamp.from(photoMetadata.captureDate))
            .addValue("fileSize", photoMetadata.fileSize)
            .addValue("encryptionKeyInfo", photoMetadata.encryptionKeyInfo)
            .addValue("blobPath", photoMetadata.blobPath)
            .addValue("uploadedAt", Timestamp.from(photoMetadata.uploadedAt))
            .addValue("isDeleted", photoMetadata.isDeleted)

        jdbcTemplate.update(sql, params)
        photoMetadata
    }

    override suspend fun findById(id: UUID): PhotoMetadata? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, filename, angle, capture_date, file_size,
                   encryption_key_info, blob_path, uploaded_at, is_deleted
            FROM photo_metadata
            WHERE id = :id AND is_deleted = false
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToPhotoMetadata(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByUserId(userId: UUID, limit: Int, offset: Int): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, filename, angle, capture_date, file_size,
                   encryption_key_info, blob_path, uploaded_at, is_deleted
            FROM photo_metadata
            WHERE user_id = :userId AND is_deleted = false
            ORDER BY capture_date DESC, uploaded_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", limit)
            .addValue("offset", offset)

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToPhotoMetadata(rs) }
    }

    override suspend fun findByUserIdAndAngle(userId: UUID, angle: PhotoAngle): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, filename, angle, capture_date, file_size,
                   encryption_key_info, blob_path, uploaded_at, is_deleted
            FROM photo_metadata
            WHERE user_id = :userId AND angle = :angle AND is_deleted = false
            ORDER BY capture_date DESC
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("angle", angle.name)

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToPhotoMetadata(rs) }
    }

    override suspend fun findByUserIdAndDateRange(
        userId: UUID,
        startDate: Instant,
        endDate: Instant
    ): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, filename, angle, capture_date, file_size,
                   encryption_key_info, blob_path, uploaded_at, is_deleted
            FROM photo_metadata
            WHERE user_id = :userId 
            AND capture_date BETWEEN :startDate AND :endDate
            AND is_deleted = false
            ORDER BY capture_date DESC
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("startDate", Timestamp.from(startDate))
            .addValue("endDate", Timestamp.from(endDate))

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToPhotoMetadata(rs) }
    }

    override suspend fun findByUserIdAngleAndDateRange(
        userId: UUID,
        angle: PhotoAngle,
        startDate: Instant,
        endDate: Instant
    ): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, filename, angle, capture_date, file_size,
                   encryption_key_info, blob_path, uploaded_at, is_deleted
            FROM photo_metadata
            WHERE user_id = :userId AND angle = :angle
            AND capture_date BETWEEN :startDate AND :endDate
            AND is_deleted = false
            ORDER BY capture_date DESC
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("angle", angle.name)
            .addValue("startDate", Timestamp.from(startDate))
            .addValue("endDate", Timestamp.from(endDate))

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToPhotoMetadata(rs) }
    }

    override suspend fun update(photoMetadata: PhotoMetadata): PhotoMetadata = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE photo_metadata 
            SET filename = :filename, angle = :angle, capture_date = :captureDate,
                file_size = :fileSize, encryption_key_info = :encryptionKeyInfo,
                blob_path = :blobPath, is_deleted = :isDeleted
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", photoMetadata.id)
            .addValue("filename", photoMetadata.filename)
            .addValue("angle", photoMetadata.angle.name)
            .addValue("captureDate", Timestamp.from(photoMetadata.captureDate))
            .addValue("fileSize", photoMetadata.fileSize)
            .addValue("encryptionKeyInfo", photoMetadata.encryptionKeyInfo)
            .addValue("blobPath", photoMetadata.blobPath)
            .addValue("isDeleted", photoMetadata.isDeleted)

        jdbcTemplate.update(sql, params)
        photoMetadata
    }

    override suspend fun markAsDeleted(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "UPDATE photo_metadata SET is_deleted = true WHERE id = :id"
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun delete(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM photo_metadata WHERE id = :id"
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun countByUserId(userId: UUID, includeDeleted: Boolean): Long = withContext(Dispatchers.IO) {
        val sql = if (includeDeleted) {
            "SELECT COUNT(*) FROM photo_metadata WHERE user_id = :userId"
        } else {
            "SELECT COUNT(*) FROM photo_metadata WHERE user_id = :userId AND is_deleted = false"
        }
        val params = MapSqlParameterSource("userId", userId)
        jdbcTemplate.queryForObject(sql, params, Long::class.java) ?: 0L
    }

    override suspend fun findLatestByUserIdAndAngle(userId: UUID, angle: PhotoAngle): PhotoMetadata? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, filename, angle, capture_date, file_size,
                   encryption_key_info, blob_path, uploaded_at, is_deleted
            FROM photo_metadata
            WHERE user_id = :userId AND angle = :angle AND is_deleted = false
            ORDER BY capture_date DESC
            LIMIT 1
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("angle", angle.name)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToPhotoMetadata(rs) }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapRowToPhotoMetadata(rs: ResultSet): PhotoMetadata {
        return PhotoMetadata(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            filename = rs.getString("filename"),
            angle = PhotoAngle.valueOf(rs.getString("angle")),
            captureDate = rs.getTimestamp("capture_date").toInstant(),
            fileSize = rs.getObject("file_size") as? Long,
            encryptionKeyInfo = rs.getString("encryption_key_info"),
            blobPath = rs.getString("blob_path"),
            uploadedAt = rs.getTimestamp("uploaded_at").toInstant(),
            isDeleted = rs.getBoolean("is_deleted")
        )
    }
}