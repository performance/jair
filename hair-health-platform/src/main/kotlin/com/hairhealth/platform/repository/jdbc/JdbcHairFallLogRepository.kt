package com.hairhealth.platform.repository.jdbc

import com.hairhealth.platform.domain.HairFallCategory
import com.hairhealth.platform.domain.HairFallLog
import com.hairhealth.platform.repository.HairFallLogRepository
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
class JdbcHairFallLogRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : HairFallLogRepository {

    override suspend fun create(hairFallLog: HairFallLog): HairFallLog = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO hair_fall_logs (id, user_id, date, count, category, description, photo_metadata_id, created_at, updated_at)
            VALUES (:id, :userId, :date, :count, :category, :description, :photoMetadataId, :createdAt, :updatedAt)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", hairFallLog.id)
            .addValue("userId", hairFallLog.userId)
            .addValue("date", Date.valueOf(hairFallLog.date))
            .addValue("count", hairFallLog.count)
            .addValue("category", hairFallLog.category.name)
            .addValue("description", hairFallLog.description)
            .addValue("photoMetadataId", hairFallLog.photoMetadataId)
            .addValue("createdAt", Timestamp.from(hairFallLog.createdAt))
            .addValue("updatedAt", Timestamp.from(hairFallLog.updatedAt))

        jdbcTemplate.update(sql, params)
        hairFallLog
    }

    override suspend fun findById(id: UUID): HairFallLog? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, date, count, category, description, photo_metadata_id, created_at, updated_at
            FROM hair_fall_logs
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToHairFallLog(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByIdAndUserId(id: UUID, userId: UUID): HairFallLog? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, date, count, category, description, photo_metadata_id, created_at, updated_at
            FROM hair_fall_logs
            WHERE id = :id AND user_id = :userId
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", id)
            .addValue("userId", userId)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToHairFallLog(rs) }
        } catch (e: Exception) { // More specific: EmptyResultDataAccessException
            null
        }
    }

    override suspend fun findByUserId(userId: UUID, limit: Int, offset: Int): List<HairFallLog> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, date, count, category, description, photo_metadata_id, created_at, updated_at
            FROM hair_fall_logs
            WHERE user_id = :userId
            ORDER BY date DESC, created_at DESC
            LIMIT :limit OFFSET :offset
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", limit)
            .addValue("offset", offset)

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToHairFallLog(rs) }
    }

    override suspend fun findByUserIdAndDateRange(
        userId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HairFallLog> = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, user_id, date, count, category, description, photo_metadata_id, created_at, updated_at
            FROM hair_fall_logs
            WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate
            ORDER BY date DESC
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("startDate", Date.valueOf(startDate))
            .addValue("endDate", Date.valueOf(endDate))

        jdbcTemplate.query(sql, params) { rs, _ -> mapRowToHairFallLog(rs) }
    }

    override suspend fun update(hairFallLog: HairFallLog): HairFallLog = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE hair_fall_logs 
            SET date = :date, count = :count, category = :category, description = :description,
                photo_metadata_id = :photoMetadataId, updated_at = :updatedAt
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", hairFallLog.id)
            .addValue("date", Date.valueOf(hairFallLog.date))
            .addValue("count", hairFallLog.count)
            .addValue("category", hairFallLog.category.name)
            .addValue("description", hairFallLog.description)
            .addValue("photoMetadataId", hairFallLog.photoMetadataId)
            .addValue("updatedAt", Timestamp.from(hairFallLog.updatedAt))

        jdbcTemplate.update(sql, params)
        hairFallLog
    }

    override suspend fun delete(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM hair_fall_logs WHERE id = :id"
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun countByUserId(userId: UUID): Long = withContext(Dispatchers.IO) {
        val sql = "SELECT COUNT(*) FROM hair_fall_logs WHERE user_id = :userId"
        val params = MapSqlParameterSource("userId", userId)
        jdbcTemplate.queryForObject(sql, params, Long::class.java) ?: 0L
    }

    private fun mapRowToHairFallLog(rs: ResultSet): HairFallLog {
        return HairFallLog(
            id = UUID.fromString(rs.getString("id")),
            userId = UUID.fromString(rs.getString("user_id")),
            date = rs.getDate("date").toLocalDate(),
            count = rs.getObject("count") as? Int,
            category = HairFallCategory.valueOf(rs.getString("category")),
            description = rs.getString("description"),
            photoMetadataId = rs.getString("photo_metadata_id")?.let { UUID.fromString(it) },
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}