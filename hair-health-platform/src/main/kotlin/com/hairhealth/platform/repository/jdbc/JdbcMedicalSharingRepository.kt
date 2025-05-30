package com.hairhealth.platform.repository.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Array
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Repository
class JdbcMedicalSharingRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) : MedicalSharingRepository {

    override suspend fun createSession(session: MedicalSharingSession): MedicalSharingSession = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO medical_sharing_sessions (
                id, patient_id, professional_id, photo_ids, notes, max_total_views,
                max_view_duration_minutes, expires_at, allow_screenshots, allow_download,
                status, created_at, updated_at
            ) VALUES (
                :id, :patientId, :professionalId, :photoIds, :notes, :maxTotalViews,
                :maxViewDurationMinutes, :expiresAt, :allowScreenshots, :allowDownload,
                :status, :createdAt, :updatedAt
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", session.id)
            .addValue("patientId", session.patientId)
            .addValue("professionalId", session.professionalId)
            .addValue("photoIds", session.photoIds.toTypedArray())
            .addValue("notes", session.notes)
            .addValue("maxTotalViews", session.maxTotalViews)
            .addValue("maxViewDurationMinutes", session.maxViewDurationMinutes)
            .addValue("expiresAt", Timestamp.from(session.expiresAt))
            .addValue("allowScreenshots", session.allowScreenshots)
            .addValue("allowDownload", session.allowDownload)
            .addValue("status", session.status.name)
            .addValue("createdAt", Timestamp.from(session.createdAt))
            .addValue("updatedAt", Timestamp.from(session.updatedAt))

        jdbcTemplate.update(sql, params)
        session
    }

    override suspend fun findSessionById(id: UUID): MedicalSharingSession? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, patient_id, professional_id, photo_ids, notes, max_total_views,
                   max_view_duration_minutes, expires_at, allow_screenshots, allow_download,
                   status, created_at, updated_at, revoked_at, revoked_reason
            FROM medical_sharing_sessions
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToMedicalSession(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findSessionsByPatientId(patientId: UUID): List<MedicalSharingSession> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, patient_id, professional_id, photo_ids, notes, max_total_views,
                   max_view_duration_minutes, expires_at, allow_screenshots, allow_download,
                   status, created_at, updated_at, revoked_at, revoked_reason
            FROM medical_sharing_sessions
            WHERE patient_id = :patientId
            ORDER BY created_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("patientId", patientId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToMedicalSession(rs) }
    }

    override suspend fun findSessionsByProfessionalId(professionalId: UUID): List<MedicalSharingSession> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, patient_id, professional_id, photo_ids, notes, max_total_views,
                   max_view_duration_minutes, expires_at, allow_screenshots, allow_download,
                   status, created_at, updated_at, revoked_at, revoked_reason
            FROM medical_sharing_sessions
            WHERE professional_id = :professionalId AND status IN ('PENDING_DOCTOR_ACCESS', 'ACTIVE')
            ORDER BY created_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("professionalId", professionalId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToMedicalSession(rs) }
    }

    override suspend fun updateSession(session: MedicalSharingSession): MedicalSharingSession = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE medical_sharing_sessions 
            SET status = :status, updated_at = :updatedAt, revoked_at = :revokedAt, revoked_reason = :revokedReason
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", session.id)
            .addValue("status", session.status.name)
            .addValue("updatedAt", Timestamp.from(session.updatedAt))
            .addValue("revokedAt", session.revokedAt?.let { Timestamp.from(it) })
            .addValue("revokedReason", session.revokedReason)

        jdbcTemplate.update(sql, params)
        session
    }

    override suspend fun revokeSession(id: UUID, reason: String): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE medical_sharing_sessions 
            SET status = 'REVOKED_BY_PATIENT', revoked_at = CURRENT_TIMESTAMP, revoked_reason = :reason, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id AND status IN ('PENDING_DOCTOR_ACCESS', 'ACTIVE')
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("reason", reason)

        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun findExpiredSessions(): List<MedicalSharingSession> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, patient_id, professional_id, photo_ids, notes, max_total_views,
                   max_view_duration_minutes, expires_at, allow_screenshots, allow_download,
                   status, created_at, updated_at, revoked_at, revoked_reason
            FROM medical_sharing_sessions
            WHERE expires_at < CURRENT_TIMESTAMP AND status IN ('PENDING_DOCTOR_ACCESS', 'ACTIVE')
        """.trimIndent()

        jdbcTemplate.query(sql, MapSqlParameterSource()) { rs, _ -> mapRowToMedicalSession(rs) }
    }

    private fun mapRowToMedicalSession(rs: ResultSet): MedicalSharingSession {
        val photoIdsArray = rs.getArray("photo_ids")
        val photoIds = mutableListOf<UUID>()
        
        if (photoIdsArray != null) {
            val arrayData = photoIdsArray.array as? kotlin.Array<*>
            if (arrayData != null) {
                for (element: Any? in arrayData) {
                    element?.toString()?.let { str ->
                        try {
                            photoIds.add(UUID.fromString(str))
                        } catch (e: Exception) {
                            // Skip invalid UUIDs
                        }
                    }
                }
            }
        }

        return MedicalSharingSession(
            id = UUID.fromString(rs.getString("id")),
            patientId = UUID.fromString(rs.getString("patient_id")),
            professionalId = UUID.fromString(rs.getString("professional_id")),
            photoIds = photoIds,
            notes = rs.getString("notes"),
            maxTotalViews = rs.getInt("max_total_views"),
            maxViewDurationMinutes = rs.getInt("max_view_duration_minutes"),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            allowScreenshots = rs.getBoolean("allow_screenshots"),
            allowDownload = rs.getBoolean("allow_download"),
            status = MedicalSharingStatus.valueOf(rs.getString("status")),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
            revokedAt = rs.getTimestamp("revoked_at")?.toInstant(),
            revokedReason = rs.getString("revoked_reason")
        )
    }
}

@Repository
class JdbcDoctorAccessRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : DoctorAccessRepository {

    override suspend fun createAccessSession(session: DoctorAccessSession): DoctorAccessSession = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO doctor_access_sessions (
                id, medical_session_id, professional_id, device_fingerprint, ip_address,
                user_agent, started_at, expires_at, is_active
            ) VALUES (
                :id, :medicalSessionId, :professionalId, :deviceFingerprint, :ipAddress,
                :userAgent, :startedAt, :expiresAt, :isActive
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", session.id)
            .addValue("medicalSessionId", session.medicalSessionId)
            .addValue("professionalId", session.professionalId)
            .addValue("deviceFingerprint", session.deviceFingerprint)
            .addValue("ipAddress", session.ipAddress)
            .addValue("userAgent", session.userAgent)
            .addValue("startedAt", Timestamp.from(session.startedAt))
            .addValue("expiresAt", Timestamp.from(session.expiresAt))
            .addValue("isActive", session.isActive)

        jdbcTemplate.update(sql, params)
        session
    }

    override suspend fun findAccessSessionById(id: UUID): DoctorAccessSession? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, medical_session_id, professional_id, device_fingerprint, ip_address,
                   user_agent, started_at, expires_at, ended_at, is_active
            FROM doctor_access_sessions
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToDoctorAccessSession(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findActiveAccessSessionsByMedicalSession(medicalSessionId: UUID): List<DoctorAccessSession> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, medical_session_id, professional_id, device_fingerprint, ip_address,
                   user_agent, started_at, expires_at, ended_at, is_active
            FROM doctor_access_sessions
            WHERE medical_session_id = :medicalSessionId AND is_active = true
            ORDER BY started_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("medicalSessionId", medicalSessionId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToDoctorAccessSession(rs) }
    }

    override suspend fun endAccessSession(id: UUID, endedAt: Instant): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE doctor_access_sessions 
            SET ended_at = :endedAt, is_active = false
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("endedAt", Timestamp.from(endedAt))

        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun countAccessSessionsByMedicalSession(medicalSessionId: UUID): Int = withContext(Dispatchers.IO) {
        val sql = "SELECT COUNT(*) FROM doctor_access_sessions WHERE medical_session_id = :medicalSessionId"
        val params = MapSqlParameterSource("medicalSessionId", medicalSessionId)
        jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0
    }

    private fun mapRowToDoctorAccessSession(rs: ResultSet): DoctorAccessSession {
        return DoctorAccessSession(
            id = UUID.fromString(rs.getString("id")),
            medicalSessionId = UUID.fromString(rs.getString("medical_session_id")),
            professionalId = UUID.fromString(rs.getString("professional_id")),
            deviceFingerprint = rs.getString("device_fingerprint"),
            ipAddress = rs.getString("ip_address"),
            userAgent = rs.getString("user_agent"),
            startedAt = rs.getTimestamp("started_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            endedAt = rs.getTimestamp("ended_at")?.toInstant(),
            isActive = rs.getBoolean("is_active")
        )
    }
}