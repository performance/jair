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
class JdbcMedicalNotificationRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : MedicalNotificationRepository {

    override suspend fun createNotification(notification: MedicalNotification): MedicalNotification = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO medical_notifications (
                id, recipient_id, type, title, message, related_session_id, 
                related_professional_id, is_read, created_at
            ) VALUES (
                :id, :recipientId, :type, :title, :message, :relatedSessionId,
                :relatedProfessionalId, :isRead, :createdAt
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", notification.id)
            .addValue("recipientId", notification.recipientId)
            .addValue("type", notification.type.name)
            .addValue("title", notification.title)
            .addValue("message", notification.message)
            .addValue("relatedSessionId", notification.relatedSessionId)
            .addValue("relatedProfessionalId", notification.relatedProfessionalId)
            .addValue("isRead", notification.isRead)
            .addValue("createdAt", Timestamp.from(notification.createdAt))

        jdbcTemplate.update(sql, params)
        notification
    }

    override suspend fun findNotificationsByRecipient(recipientId: UUID, unreadOnly: Boolean): List<MedicalNotification> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, recipient_id, type, title, message, related_session_id,
                   related_professional_id, is_read, created_at, read_at
            FROM medical_notifications
            WHERE recipient_id = :recipientId
            ${if (unreadOnly) "AND is_read = false" else ""}
            ORDER BY created_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("recipientId", recipientId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToNotification(rs) }
    }

    override suspend fun markAsRead(notificationId: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE medical_notifications 
            SET is_read = true, read_at = CURRENT_TIMESTAMP
            WHERE id = :notificationId
        """.trimIndent()

        val params = MapSqlParameterSource("notificationId", notificationId)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun markAllAsRead(recipientId: UUID): Int = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE medical_notifications 
            SET is_read = true, read_at = CURRENT_TIMESTAMP
            WHERE recipient_id = :recipientId AND is_read = false
        """.trimIndent()

        val params = MapSqlParameterSource("recipientId", recipientId)
        jdbcTemplate.update(sql, params)
    }

    private fun mapRowToNotification(rs: ResultSet): MedicalNotification {
        return MedicalNotification(
            id = UUID.fromString(rs.getString("id")),
            recipientId = UUID.fromString(rs.getString("recipient_id")),
            type = NotificationType.valueOf(rs.getString("type")),
            title = rs.getString("title"),
            message = rs.getString("message"),
            relatedSessionId = rs.getString("related_session_id")?.let { UUID.fromString(it) },
            relatedProfessionalId = rs.getString("related_professional_id")?.let { UUID.fromString(it) },
            isRead = rs.getBoolean("is_read"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            readAt = rs.getTimestamp("read_at")?.toInstant()
        )
    }
}