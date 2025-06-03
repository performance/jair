package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.ActionStatus
import com.hairhealth.platform.domain.PhaseAction
import com.hairhealth.platform.repository.PhaseActionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.UUID

@Repository
class JdbcPhaseActionRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : PhaseActionRepository {

    private val phaseActionRowMapper = RowMapper<PhaseAction> { rs: ResultSet, _: Int ->
        PhaseAction(
            id = UUID.fromString(rs.getString("id")),
            phaseId = UUID.fromString(rs.getString("phase_id")),
            actionDescription = rs.getString("action_description"),
            actionType = rs.getString("action_type"),
            actionDetails = rs.getString("action_details"),
            isKeyAction = rs.getBoolean("is_key_action"),
            status = ActionStatus.valueOf(rs.getString("status").uppercase()),
            dueDate = rs.getDate("due_date")?.toLocalDate(),
            completedAt = rs.getTimestamp("completed_at")?.toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }

    override suspend fun save(action: PhaseAction): PhaseAction = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO phase_actions (
                id, phase_id, action_description, action_type, action_details,
                is_key_action, status, due_date, completed_at, created_at, updated_at
            ) VALUES (
                :id, :phase_id, :action_description, :action_type, :action_details,
                :is_key_action, :status, :due_date, :completed_at, :created_at, :updated_at
            )
            ON CONFLICT (id) DO UPDATE SET
                phase_id = EXCLUDED.phase_id,
                action_description = EXCLUDED.action_description,
                action_type = EXCLUDED.action_type,
                action_details = EXCLUDED.action_details,
                is_key_action = EXCLUDED.is_key_action,
                status = EXCLUDED.status,
                due_date = EXCLUDED.due_date,
                completed_at = EXCLUDED.completed_at,
                updated_at = EXCLUDED.updated_at
            RETURNING *;
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", action.id)
            .addValue("phase_id", action.phaseId)
            .addValue("action_description", action.actionDescription)
            .addValue("action_type", action.actionType)
            .addValue("action_details", action.actionDetails, Types.VARCHAR)
            .addValue("is_key_action", action.isKeyAction)
            .addValue("status", action.status.name)
            .addValue("due_date", action.dueDate?.let { Date.valueOf(it) }, Types.DATE)
            .addValue("completed_at", action.completedAt?.let { Timestamp.from(it) }, Types.TIMESTAMP_WITH_TIMEZONE)
            .addValue("created_at", Timestamp.from(action.createdAt))
            .addValue("updated_at", Timestamp.from(action.updatedAt))

        jdbcTemplate.queryForObject(sql, params, phaseActionRowMapper)
            ?: throw IllegalStateException("Failed to save phase action and retrieve it.")
    }

    override suspend fun saveAll(actions: List<PhaseAction>): List<PhaseAction> = withContext(Dispatchers.IO) {
        // Simple iteration for now. Batch updates could be an optimization.
        actions.map { save(it) }
    }

    override suspend fun findById(id: UUID): PhaseAction? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM phase_actions WHERE id = :id"
        val params = MapSqlParameterSource().addValue("id", id)
        try {
            jdbcTemplate.queryForObject(sql, params, phaseActionRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override suspend fun findByPhaseId(phaseId: UUID): List<PhaseAction> = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM phase_actions WHERE phase_id = :phase_id ORDER BY created_at ASC" // Or by due_date
        val params = MapSqlParameterSource().addValue("phase_id", phaseId)
        jdbcTemplate.query(sql, params, phaseActionRowMapper)
    }
}
