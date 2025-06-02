package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.Intervention
import com.hairhealth.platform.domain.InterventionApplication
import com.hairhealth.platform.domain.InterventionType
import com.hairhealth.platform.repository.InterventionRepository
import com.hairhealth.platform.repository.InterventionApplicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.util.*

@Repository
class JdbcInterventionRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : InterventionRepository {

    override suspend fun create(intervention: Intervention): Intervention = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO interventions (
                id, user_id, type, product_name, dosage_amount, frequency, 
                application_time, start_date, end_date, is_active, provider, 
                notes, source_recommendation_id, created_at, updated_at
            )
            VALUES (
                :id, :userId, :type, :productName, :dosageAmount, :frequency,
                :applicationTime, :startDate, :endDate, :isActive, :provider,
                :notes, :sourceRecommendationId, :createdAt, :updatedAt
            )
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", intervention.id)
            .addValue("userId", intervention.userId)
            .addValue("type", intervention.type.name)
            .addValue("productName", intervention.productName)
            .addValue("dosageAmount", intervention.dosageAmount)
            .addValue("frequency", intervention.frequency)
            .addValue("applicationTime", intervention.applicationTime)
            .addValue("startDate", Date.valueOf(intervention.startDate))
            .addValue("endDate", intervention.endDate?.let { Date.valueOf(it) })
            .addValue("isActive", intervention.isActive)
            .addValue("provider", intervention.provider)
            .addValue("notes", intervention.notes)
            .addValue("sourceRecommendationId", intervention.sourceRecommendationId)
            .addValue("createdAt", Timestamp.from(intervention.createdAt))
            .addValue("updatedAt", Timestamp.from(intervention.updatedAt))

        jdbcTemplate.update(sql, params)
        intervention
    }

    override suspend fun findById(id: UUID): Intervention? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, type, product_name, dosage_amount, frequency,
                   application_time, start_date, end_date, is_active, provider,
                   notes, source_recommendation_id, created_at, updated_at
            FROM interventions
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToIntervention(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByUserId(userId: UUID, includeInactive: Boolean): List<Intervention> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, type, product_name, dosage_amount, frequency,
                   application_time, start_date, end_date, is_active, provider,
                   notes, source_recommendation_id, created_at, updated_at
            FROM interventions
            WHERE user_id = :userId
            ${if (!includeInactive) "AND is_active = true" else ""}
            ORDER BY created_at DESC
        """.trimIndent()

        val params = MapSqlParameterSource("userId", userId)
        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToIntervention(rs) }
    }

    override suspend fun findByIdAndUserId(id: UUID, userId: UUID): Intervention? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, type, product_name, dosage_amount, frequency,
                   application_time, start_date, end_date, is_active, provider,
                   notes, source_recommendation_id, created_at, updated_at
            FROM interventions
            WHERE id = :id AND user_id = :userId
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToIntervention(rs) }
        } catch (e: Exception) { // org.springframework.dao.EmptyResultDataAccessException more specific
            null
        }
    }

    override suspend fun findActiveByUserId(userId: UUID): List<Intervention> = withContext(Dispatchers.IO) {
        return@withContext findByUserId(userId, includeInactive = false)
    }

    override suspend fun update(intervention: Intervention): Intervention = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE interventions 
            SET type = :type, product_name = :productName, dosage_amount = :dosageAmount,
                frequency = :frequency, application_time = :applicationTime, 
                start_date = :startDate, end_date = :endDate, is_active = :isActive,
                provider = :provider, notes = :notes, source_recommendation_id = :sourceRecommendationId,
                updated_at = :updatedAt
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", intervention.id)
            .addValue("type", intervention.type.name)
            .addValue("productName", intervention.productName)
            .addValue("dosageAmount", intervention.dosageAmount)
            .addValue("frequency", intervention.frequency)
            .addValue("applicationTime", intervention.applicationTime)
            .addValue("startDate", Date.valueOf(intervention.startDate))
            .addValue("endDate", intervention.endDate?.let { Date.valueOf(it) })
            .addValue("isActive", intervention.isActive)
            .addValue("provider", intervention.provider)
            .addValue("notes", intervention.notes)
            .addValue("sourceRecommendationId", intervention.sourceRecommendationId)
            .addValue("updatedAt", Timestamp.from(intervention.updatedAt))

        jdbcTemplate.update(sql, params)
        intervention
    }

    override suspend fun deactivate(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE interventions 
            SET is_active = false, updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
        """.trimIndent()
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun delete(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM interventions WHERE id = :id"
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    private fun mapRowToIntervention(rs: ResultSet): Intervention {
        return Intervention(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            type = InterventionType.valueOf(rs.getString("type")),
            productName = rs.getString("product_name"),
            dosageAmount = rs.getString("dosage_amount"),
            frequency = rs.getString("frequency"),
            applicationTime = rs.getString("application_time"),
            startDate = rs.getDate("start_date").toLocalDate(),
            endDate = rs.getDate("end_date")?.toLocalDate(),
            isActive = rs.getBoolean("is_active"),
            provider = rs.getString("provider"),
            notes = rs.getString("notes"),
            sourceRecommendationId = rs.getString("source_recommendation_id")?.let { UUID.fromString(it) },
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}

@Repository
class JdbcInterventionApplicationRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : InterventionApplicationRepository {

    override suspend fun create(application: InterventionApplication): InterventionApplication = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO intervention_applications (id, intervention_id, user_id, timestamp, notes, created_at)
            VALUES (:id, :interventionId, :userId, :timestamp, :notes, :createdAt)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", application.id)
            .addValue("interventionId", application.interventionId)
            .addValue("userId", application.userId)
            .addValue("timestamp", Timestamp.from(application.timestamp))
            .addValue("notes", application.notes)
            .addValue("createdAt", Timestamp.from(application.createdAt))

        jdbcTemplate.update(sql, params)
        application
    }

    override suspend fun findById(id: UUID): InterventionApplication? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, intervention_id, user_id, timestamp, notes, created_at
            FROM intervention_applications
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToInterventionApplication(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByInterventionId(interventionId: UUID, limit: Int, offset: Int): List<InterventionApplication> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, intervention_id, user_id, timestamp, notes, created_at
            FROM intervention_applications
            WHERE intervention_id = :interventionId
            ORDER BY timestamp DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("interventionId", interventionId)
            .addValue("limit", limit)
            .addValue("offset", offset)

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToInterventionApplication(rs) }
    }

    override suspend fun findByUserIdAndDateRange(
        userId: UUID,
        startDate: java.time.Instant, // Changed from LocalDate
        endDate: java.time.Instant   // Changed from LocalDate
    ): List<InterventionApplication> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, intervention_id, user_id, timestamp, notes, created_at
            FROM intervention_applications
            WHERE user_id = :userId 
            AND timestamp >= :startDate AND timestamp < :endDate
            ORDER BY timestamp DESC
        """.trimIndent() // Adjusted query for Instant range

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("startDate", Timestamp.from(startDate))
            .addValue("endDate", Timestamp.from(endDate))

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToInterventionApplication(rs) }
    }

    override suspend fun countByInterventionId(interventionId: UUID): Long = withContext(Dispatchers.IO) {
        val sql = "SELECT COUNT(*) FROM intervention_applications WHERE intervention_id = :interventionId"
        val params = MapSqlParameterSource("interventionId", interventionId)
        jdbcTemplate.queryForObject(sql, params, Long::class.java) ?: 0L
    }

    override suspend fun delete(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM intervention_applications WHERE id = :id"
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    private fun mapRowToInterventionApplication(rs: ResultSet): InterventionApplication {
        return InterventionApplication(
            id = UUID.fromString(rs.getString("id")),
            interventionId = UUID.fromString(rs.getString("intervention_id")),
            userId = UUID.fromString(rs.getString("user_id")),
            timestamp = rs.getTimestamp("timestamp").toInstant(),
            notes = rs.getString("notes"),
            createdAt = rs.getTimestamp("created_at").toInstant()
        )
    }
}