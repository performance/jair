package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.ImplementationPlan
import com.hairhealth.platform.domain.PlanStatus
import com.hairhealth.platform.repository.ImplementationPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.util.UUID

@Repository
class JdbcImplementationPlanRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : ImplementationPlanRepository {

    private val implementationPlanRowMapper = RowMapper<ImplementationPlan> { rs: ResultSet, _: Int ->
        ImplementationPlan(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            sourceRecommendationIds = rs.getString("source_recommendation_ids"),
            status = PlanStatus.valueOf(rs.getString("status").uppercase()),
            userContextSnapshot = rs.getString("user_context_snapshot"),
            riskAssessmentResults = rs.getString("risk_assessment_results"),
            successProbability = rs.getObject("success_probability") as? Double,
            currentPhaseNumber = rs.getInt("current_phase_number"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }

    override suspend fun save(plan: ImplementationPlan): ImplementationPlan = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO implementation_plans (
                id, user_id, source_recommendation_ids, status, user_context_snapshot,
                risk_assessment_results, success_probability, current_phase_number, created_at, updated_at
            ) VALUES (
                :id, :user_id, :source_recommendation_ids, :status, :user_context_snapshot,
                :risk_assessment_results, :success_probability, :current_phase_number, :created_at, :updated_at
            )
            ON CONFLICT (id) DO UPDATE SET
                user_id = EXCLUDED.user_id,
                source_recommendation_ids = EXCLUDED.source_recommendation_ids,
                status = EXCLUDED.status,
                user_context_snapshot = EXCLUDED.user_context_snapshot,
                risk_assessment_results = EXCLUDED.risk_assessment_results,
                success_probability = EXCLUDED.success_probability,
                current_phase_number = EXCLUDED.current_phase_number,
                updated_at = EXCLUDED.updated_at
            RETURNING *;
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", plan.id)
            .addValue("user_id", plan.userId)
            .addValue("source_recommendation_ids", plan.sourceRecommendationIds, Types.VARCHAR)
            .addValue("status", plan.status.name)
            .addValue("user_context_snapshot", plan.userContextSnapshot, Types.VARCHAR)
            .addValue("risk_assessment_results", plan.riskAssessmentResults, Types.VARCHAR)
            .addValue("success_probability", plan.successProbability, Types.DOUBLE)
            .addValue("current_phase_number", plan.currentPhaseNumber)
            .addValue("created_at", Timestamp.from(plan.createdAt))
            .addValue("updated_at", Timestamp.from(plan.updatedAt))

        jdbcTemplate.queryForObject(sql, params, implementationPlanRowMapper)
            ?: throw IllegalStateException("Failed to save implementation plan and retrieve it.")
    }

    override suspend fun findById(id: UUID): ImplementationPlan? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM implementation_plans WHERE id = :id"
        val params = MapSqlParameterSource().addValue("id", id)
        try {
            jdbcTemplate.queryForObject(sql, params, implementationPlanRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override suspend fun findByUserIdAndStatus(userId: UUID, status: PlanStatus): List<ImplementationPlan> = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM implementation_plans WHERE user_id = :user_id AND status = :status ORDER BY created_at DESC"
        val params = MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("status", status.name)
        jdbcTemplate.query(sql, params, implementationPlanRowMapper)
    }

    override suspend fun findActiveByUserId(userId: UUID): ImplementationPlan? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM implementation_plans WHERE user_id = :user_id AND status = :status ORDER BY created_at DESC LIMIT 1"
        val params = MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("status", PlanStatus.ACTIVE.name) // Hardcoded to ACTIVE status
        try {
            jdbcTemplate.queryForObject(sql, params, implementationPlanRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }
}
