package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.RecommendationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZoneId
import java.util.UUID

@Repository
class JdbcRecommendationRepository(private val jdbcTemplate: JdbcTemplate) : RecommendationRepository {

    private val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    private val recommendationRowMapper = RowMapper<Recommendation> { rs: ResultSet, _: Int ->
        Recommendation(
            id = UUID.fromString(rs.getString("id")),
            professionalId = UUID.fromString(rs.getString("professional_id")),
            userId = UUID.fromString(rs.getString("user_id")),
            consultationId = rs.getString("consultation_id"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            type = RecommendationType.valueOf(rs.getString("type").uppercase()),
            details = rs.getString("details"),
            status = RecommendationStatus.valueOf(rs.getString("status").uppercase()),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
            userAction = rs.getString("user_action")?.let { UserRecommendationAction.valueOf(it.uppercase()) },
            userActionNotes = rs.getString("user_action_notes"),
            userActionAt = rs.getTimestamp("user_action_at")?.toInstant()
        )
    }

    override suspend fun save(recommendation: Recommendation): Recommendation = withContext(Dispatchers.IO) {
        // Try to update first if an ID suggests it might exist, otherwise insert.
        // More robust "upsert" might require checking existence or using database-specific ON CONFLICT.
        // For simplicity, we'll use INSERT ON CONFLICT for PostgreSQL-like behavior or separate SELECT then INSERT/UPDATE.
        // Assuming PostgreSQL's ON CONFLICT for this example.
        // If not PostgreSQL, a separate findById then conditional insert/update is safer.

        val sql = """
            INSERT INTO recommendations (
                id, professional_id, user_id, consultation_id, title, description, type, details, status, created_at, updated_at,
                user_action, user_action_notes, user_action_at
            )
            VALUES (
                :id, :professional_id, :user_id, :consultation_id, :title, :description, :type, :details, :status, :created_at, :updated_at,
                :user_action, :user_action_notes, :user_action_at
            )
            ON CONFLICT (id) DO UPDATE SET
                professional_id = EXCLUDED.professional_id,
                user_id = EXCLUDED.user_id,
                consultation_id = EXCLUDED.consultation_id,
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                type = EXCLUDED.type,
                details = EXCLUDED.details,
                status = EXCLUDED.status,
                updated_at = EXCLUDED.updated_at,
                user_action = EXCLUDED.user_action,
                user_action_notes = EXCLUDED.user_action_notes,
                user_action_at = EXCLUDED.user_action_at
            RETURNING *; 
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", recommendation.id)
            .addValue("professional_id", recommendation.professionalId)
            .addValue("user_id", recommendation.userId)
            .addValue("consultation_id", recommendation.consultationId)
            .addValue("title", recommendation.title)
            .addValue("description", recommendation.description)
            .addValue("type", recommendation.type.name)
            .addValue("details", recommendation.details)
            .addValue("status", recommendation.status.name)
            .addValue("created_at", Timestamp.from(recommendation.createdAt))
            .addValue("updated_at", Timestamp.from(recommendation.updatedAt))
            .addValue("user_action", recommendation.userAction?.name)
            .addValue("user_action_notes", recommendation.userActionNotes)
            .addValue("user_action_at", recommendation.userActionAt?.let { Timestamp.from(it) })
        
        // For databases that don't support ON CONFLICT ... RETURNING, you would do:
        // 1. val existing = findById(recommendation.id)
        // 2. if (existing == null) { /* INSERT SQL */ } else { /* UPDATE SQL */ }
        // 3. Then execute the chosen SQL.
        // 4. Return the recommendation object (input, as it has all fields after update)

        // The following works if RETURNING * is supported and maps directly
         namedParameterJdbcTemplate.queryForObject(sql, params, recommendationRowMapper)
            ?: throw IllegalStateException("Failed to save recommendation and retrieve it.")
        // If RETURNING * is not available or doesn't map well, just execute update and return the input `recommendation` object
        // as it would reflect the intended state.
        // namedParameterJdbcTemplate.update(sql, params)
        // recommendation 
    }


    override suspend fun findById(id: UUID): Recommendation? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM recommendations WHERE id = :id"
        val params = MapSqlParameterSource().addValue("id", id)
        try {
            namedParameterJdbcTemplate.queryForObject(sql, params, recommendationRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override suspend fun findByProfessionalId(professionalId: UUID): List<Recommendation> = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM recommendations WHERE professional_id = :professional_id AND status <> 'DELETED' ORDER BY updated_at DESC"
        val params = MapSqlParameterSource().addValue("professional_id", professionalId)
        namedParameterJdbcTemplate.query(sql, params, recommendationRowMapper)
    }

    override suspend fun findByProfessionalIdAndUserId(professionalId: UUID, userId: UUID): List<Recommendation> = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM recommendations WHERE professional_id = :professional_id AND user_id = :user_id AND status <> 'DELETED' ORDER BY updated_at DESC"
        val params = MapSqlParameterSource()
            .addValue("professional_id", professionalId)
            .addValue("user_id", userId)
        namedParameterJdbcTemplate.query(sql, params, recommendationRowMapper)
    }
    
    override suspend fun findByProfessionalIdAndId(professionalId: UUID, id: UUID): Recommendation? = withContext(Dispatchers.IO) {
        val sql = "SELECT * FROM recommendations WHERE professional_id = :professional_id AND id = :id"
        val params = MapSqlParameterSource()
            .addValue("professional_id", professionalId)
            .addValue("id", id)
        try {
            namedParameterJdbcTemplate.queryForObject(sql, params, recommendationRowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override suspend fun findByUserIdAndStatus(
        userId: UUID,
        professionalStatus: RecommendationStatus,
        userActionStatus: UserRecommendationAction?
    ): List<Recommendation> = withContext(Dispatchers.IO) {
        var sql = "SELECT * FROM recommendations WHERE user_id = :user_id AND status = :professional_status"
        val params = MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("professional_status", professionalStatus.name)

        userActionStatus?.let {
            sql += " AND user_action = :user_action_status"
            params.addValue("user_action_status", it.name)
        }
        sql += " ORDER BY updated_at DESC"

        namedParameterJdbcTemplate.query(sql, params, recommendationRowMapper)
    }
}
