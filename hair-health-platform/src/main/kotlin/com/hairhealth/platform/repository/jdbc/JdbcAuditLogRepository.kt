package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.AuditLog
import com.hairhealth.platform.repository.AuditLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.sql.Types

@Repository
class JdbcAuditLogRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : AuditLogRepository {

    override suspend fun save(auditLog: AuditLog): AuditLog = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO audit_logs (
                id, "timestamp", actor_id, actor_type, action,
                target_entity_type, target_entity_id, ip_address, device_info,
                status, details
            ) VALUES (
                :id, :timestamp, :actor_id, :actor_type, :action,
                :target_entity_type, :target_entity_id, :ip_address, :device_info,
                :status, :details
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", auditLog.id)
            .addValue("timestamp", Timestamp.from(auditLog.timestamp))
            .addValue("actor_id", auditLog.actorId, Types.VARCHAR) // Explicitly set type for nullable String
            .addValue("actor_type", auditLog.actorType?.name, Types.VARCHAR)
            .addValue("action", auditLog.action)
            .addValue("target_entity_type", auditLog.targetEntityType, Types.VARCHAR)
            .addValue("target_entity_id", auditLog.targetEntityId, Types.VARCHAR)
            .addValue("ip_address", auditLog.ipAddress, Types.VARCHAR)
            .addValue("device_info", auditLog.deviceInfo, Types.VARCHAR)
            .addValue("status", auditLog.status.name)
            .addValue("details", auditLog.details, Types.VARCHAR)

        jdbcTemplate.update(sql, params)
        // Retrieving the saved object isn't strictly necessary for audit logging if not modifying it.
        // If RETURNING * was used (PostgreSQL specific), a row mapper would be needed here.
        auditLog
    }

    // RowMapper would be needed if we had find methods, but for save-only it's not essential
    // private val auditLogRowMapper = RowMapper<AuditLog> { rs, _ ->
    //     AuditLog(
    //         id = UUID.fromString(rs.getString("id")),
    //         timestamp = rs.getTimestamp("timestamp").toInstant(),
    //         actorId = rs.getString("actor_id"),
    //         actorType = rs.getString("actor_type")?.let { ActorType.valueOf(it) },
    //         action = rs.getString("action"),
    //         targetEntityType = rs.getString("target_entity_type"),
    //         targetEntityId = rs.getString("target_entity_id"),
    //         ipAddress = rs.getString("ip_address"),
    //         deviceInfo = rs.getString("device_info"),
    //         status = AuditEventStatus.valueOf(rs.getString("status")),
    //         details = rs.getString("details")
    //     )
    // }
}
