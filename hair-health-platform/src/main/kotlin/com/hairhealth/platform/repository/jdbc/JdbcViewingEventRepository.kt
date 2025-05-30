package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.ViewingEvent
import com.hairhealth.platform.repository.EphemeralKeyRepository
import com.hairhealth.platform.repository.MedicalNotificationRepository
import com.hairhealth.platform.repository.ViewingEventRepository
import com.hairhealth.platform.domain.EphemeralDecryptionKey 
import com.hairhealth.platform.domain.MedicalNotification
import com.hairhealth.platform.domain.NotificationType
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
class JdbcViewingEventRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : ViewingEventRepository{

    override suspend fun createViewingEvent(event: ViewingEvent): ViewingEvent = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO viewing_events (
                id, access_session_id, photo_id, started_at, device_fingerprint, ip_address,
                screenshot_attempts, download_attempts, suspicious_activity
            ) VALUES (
                :id, :accessSessionId, :photoId, :startedAt, :deviceFingerprint, :ipAddress,
                :screenshotAttempts, :downloadAttempts, :suspiciousActivity
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", event.id)
            .addValue("accessSessionId", event.accessSessionId)
            .addValue("photoId", event.photoId)
            .addValue("startedAt", Timestamp.from(event.startedAt))
            .addValue("deviceFingerprint", event.deviceFingerprint)
            .addValue("ipAddress", event.ipAddress)
            .addValue("screenshotAttempts", event.screenshotAttempts)
            .addValue("downloadAttempts", event.downloadAttempts)
            .addValue("suspiciousActivity", event.suspiciousActivity)

        jdbcTemplate.update(sql, params)
        event
    }

    override suspend fun endViewingEvent(id: UUID, endedAt: Instant, durationSeconds: Long): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE viewing_events 
            SET ended_at = :endedAt, duration_seconds = :durationSeconds
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("endedAt", Timestamp.from(endedAt))
            .addValue("durationSeconds", durationSeconds)

        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun findEventsByAccessSession(accessSessionId: UUID): List<ViewingEvent> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, access_session_id, photo_id, started_at, ended_at, duration_seconds,
                   device_fingerprint, ip_address, screenshot_attempts, download_attempts, suspicious_activity
            FROM viewing_events
            WHERE access_session_id = :accessSessionId
            ORDER BY started_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("accessSessionId", accessSessionId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToViewingEvent(rs) }
    }

    override suspend fun findEventsByPhotoId(photoId: UUID): List<ViewingEvent> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, access_session_id, photo_id, started_at, ended_at, duration_seconds,
                   device_fingerprint, ip_address, screenshot_attempts, download_attempts, suspicious_activity
            FROM viewing_events
            WHERE photo_id = :photoId
            ORDER BY started_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("photoId", photoId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToViewingEvent(rs) }
    }

    override suspend fun recordSuspiciousActivity(eventId: UUID, type: String): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE viewing_events 
            SET suspicious_activity = true,
                screenshot_attempts = CASE WHEN :type = 'SCREENSHOT' THEN screenshot_attempts + 1 ELSE screenshot_attempts END,
                download_attempts = CASE WHEN :type = 'DOWNLOAD' THEN download_attempts + 1 ELSE download_attempts END
            WHERE id = :eventId
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("eventId", eventId)
            .addValue("type", type)

        jdbcTemplate.update(sql, params) > 0
    }

    private fun mapRowToViewingEvent(rs: ResultSet): ViewingEvent {
        return ViewingEvent(
            id = UUID.fromString(rs.getString("id")),
            accessSessionId = UUID.fromString(rs.getString("access_session_id")),
            photoId = UUID.fromString(rs.getString("photo_id")),
            startedAt = rs.getTimestamp("started_at").toInstant(),
            endedAt = rs.getTimestamp("ended_at")?.toInstant(),
            durationSeconds = rs.getObject("duration_seconds") as? Long,
            deviceFingerprint = rs.getString("device_fingerprint"),
            ipAddress = rs.getString("ip_address"),
            screenshotAttempts = rs.getInt("screenshot_attempts"),
            downloadAttempts = rs.getInt("download_attempts"),
            suspiciousActivity = rs.getBoolean("suspicious_activity")
        )
    }
}
