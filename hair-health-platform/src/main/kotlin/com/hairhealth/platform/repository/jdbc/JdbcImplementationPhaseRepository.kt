package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.ImplementationPhase
import com.hairhealth.platform.domain.PhaseStatus
import com.hairhealth.platform.repository.ImplementationPhaseRepository
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
class JdbcImplementationPhaseRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : ImplementationPhaseRepository {

    private val implementationPhaseRowMapper = RowMapper<ImplementationPhase> { rs: ResultSet, _: Int ->
        ImplementationPhase(
            id = UUID.fromString(rs.getString("id")),
            planId = UUID.fromString(rs.getString("plan_id")),
            phaseNumber = rs.getInt("phase_number"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            startDate = rs.getDate("start_date")?.toLocalDate(),
            endDate = rs.getDate("end_date")?.toLocalDate(),
            status = PhaseStatus.valueOf(rs.getString("status").uppercase()),
            goals = rs.getString("goals"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }

    override suspend fun save(phase: ImplementationPhase): ImplementationPhase = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO implementation_phases (
                id, plan_id, phase_number, title, description, start_date, end_date, status, goals, created_at, updated_at
            ) VALUES (
                :id, :plan_id, :phase_number, :title, :description, :start_date, :end_date, :status, :goals, :created_at, :updated_at
            )
            ON CONFLICT (id) DO UPDATE SET
                plan_id = EXCLUDED.plan_id,
                phase_number = EXCLUDED.phase_number,
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                start_date = EXCLUDED.start_date,
                end_date = EXCLUDED.end_date,
                status = EXCLUDED.status,
                goals = EXCLUDED.goals,
                updated_at = EXCLUDED.updated_at
            RETURNING *;
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", phase.id)
            .addValue("plan_id", phase.planId)
            .addValue("phase_number", phase.phaseNumber)
            .addValue("title", phase.title)
            .addValue("description", phase.description, Types.VARCHAR)
            .addValue("start_date", phase.startDate?.let { Date.valueOf(it) }, Types.DATE)
            .addValue("end_date", phase.endDate?.let { Date.valueOf(it) }, Types.DATE)
            .addValue("status", phase.status.name)
            .addValue("goals", phase.goals, Types.VARCHAR)
            .addValue("created_at", Timestamp.from(phase.createdAt))
            .addValue("updated_at", Timestamp.from(phase.updatedAt))

        jdbcTemplate.queryForObject(sql, params, implementationPhaseRowMapper)
            ?: throw IllegalStateException("Failed to save implementation phase and retrieve it.")
    }

    override suspend fun saveAll(phases: List<ImplementationPhase>): List<ImplementationPhase> = withContext(Dispatchers.IO) {
        // Simple iteration for now. Batch updates could be an optimization.
        phases.map { save(it) }
    }

    override suspend fun findById(id: UUID): ImplementationPhase? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM implementation_phases WHERE id = :id"
        val params = MapSqlParameterSource().addValue("id", id)
        try {
            jdbcTemplate.queryForObject(sql, params, implementationPhaseRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override suspend fun findByPlanId(planId: UUID): List<ImplementationPhase> = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM implementation_phases WHERE plan_id = :plan_id ORDER BY phase_number ASC"
        val params = MapSqlParameterSource().addValue("plan_id", planId)
        jdbcTemplate.query(sql, params, implementationPhaseRowMapper)
    }

    override suspend fun findByPlanIdAndPhaseNumber(planId: UUID, phaseNumber: Int): ImplementationPhase? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM implementation_phases WHERE plan_id = :plan_id AND phase_number = :phase_number"
        val params = MapSqlParameterSource()
            .addValue("plan_id", planId)
            .addValue("phase_number", phaseNumber)
        try {
            jdbcTemplate.queryForObject(sql, params, implementationPhaseRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }
}
