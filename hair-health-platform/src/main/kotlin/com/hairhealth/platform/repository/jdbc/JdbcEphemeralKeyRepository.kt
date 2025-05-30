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
class JdbcEphemeralKeyRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : EphemeralKeyRepository {

    override suspend fun createKey(key: EphemeralDecryptionKey): EphemeralDecryptionKey = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO ephemeral_decryption_keys (
                id, session_id, photo_id, professional_id, encrypted_key, key_derivation_params,
                max_uses, current_uses, expires_at, created_at, is_revoked
            ) VALUES (
                :id, :sessionId, :photoId, :professionalId, :encryptedKey, :keyDerivationParams,
                :maxUses, :currentUses, :expiresAt, :createdAt, :isRevoked
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", key.id)
            .addValue("sessionId", key.sessionId)
            .addValue("photoId", key.photoId)
            .addValue("professionalId", key.professionalId)
            .addValue("encryptedKey", key.encryptedKey)
            .addValue("keyDerivationParams", key.keyDerivationParams)
            .addValue("maxUses", key.maxUses)
            .addValue("currentUses", key.currentUses)
            .addValue("expiresAt", Timestamp.from(key.expiresAt))
            .addValue("createdAt", Timestamp.from(key.createdAt))
            .addValue("isRevoked", key.isRevoked)

        jdbcTemplate.update(sql, params)
        key
    }

    override suspend fun findKeyBySessionAndPhoto(sessionId: UUID, photoId: UUID): EphemeralDecryptionKey? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, session_id, photo_id, professional_id, encrypted_key, key_derivation_params,
                   max_uses, current_uses, expires_at, created_at, last_used_at, is_revoked
            FROM ephemeral_decryption_keys
            WHERE session_id = :sessionId AND photo_id = :photoId AND is_revoked = false
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("sessionId", sessionId)
            .addValue("photoId", photoId)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToEphemeralKey(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun incrementKeyUsage(keyId: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE ephemeral_decryption_keys 
            SET current_uses = current_uses + 1, last_used_at = CURRENT_TIMESTAMP
            WHERE id = :keyId AND current_uses < max_uses AND expires_at > CURRENT_TIMESTAMP AND is_revoked = false
        """.trimIndent()

        val params = MapSqlParameterSource("keyId", keyId)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun revokeKey(keyId: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "UPDATE ephemeral_decryption_keys SET is_revoked = true WHERE id = :keyId"
        val params = MapSqlParameterSource("keyId", keyId)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun cleanupExpiredKeys(): Int = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE ephemeral_decryption_keys 
            SET is_revoked = true 
            WHERE expires_at < CURRENT_TIMESTAMP AND is_revoked = false
        """.trimIndent()

        jdbcTemplate.update(sql, MapSqlParameterSource())
    }

    private fun mapRowToEphemeralKey(rs: ResultSet): EphemeralDecryptionKey {
        return EphemeralDecryptionKey(
            id = UUID.fromString(rs.getString("id")),
            sessionId = UUID.fromString(rs.getString("session_id")),
            photoId = UUID.fromString(rs.getString("photo_id")),
            professionalId = UUID.fromString(rs.getString("professional_id")),
            encryptedKey = rs.getString("encrypted_key"),
            keyDerivationParams = rs.getString("key_derivation_params"),
            maxUses = rs.getInt("max_uses"),
            currentUses = rs.getInt("current_uses"),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            lastUsedAt = rs.getTimestamp("last_used_at")?.toInstant(),
            isRevoked = rs.getBoolean("is_revoked")
        )
    }
}
