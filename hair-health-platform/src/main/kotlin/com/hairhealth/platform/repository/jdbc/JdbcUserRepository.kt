package com.hairhealth.platform.repository.jdbc

import com.fasterxml.jackson.databind.ObjectMapper
import com.hairhealth.platform.domain.*
import com.hairhealth.platform.repository.UserRepository
import com.hairhealth.platform.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Repository
class JdbcUserRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) : UserRepository {

    override suspend fun create(user: User): User = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO users (id, email, username, password_hash, is_email_verified, created_at, updated_at, is_active)
            VALUES (:id, :email, :username, :passwordHash, :isEmailVerified, :createdAt, :updatedAt, :isActive)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", user.id)
            .addValue("email", user.email)
            .addValue("username", user.username)
            .addValue("passwordHash", user.passwordHash)
            .addValue("isEmailVerified", user.isEmailVerified)
            .addValue("createdAt", Timestamp.from(user.createdAt))
            .addValue("updatedAt", Timestamp.from(user.updatedAt))
            .addValue("isActive", user.isActive)

        jdbcTemplate.update(sql, params)
        user
    }

    override suspend fun findById(id: UUID): User? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, email, username, password_hash, is_email_verified, created_at, updated_at, is_active
            FROM users
            WHERE id = :id AND is_active = true
        """.trimIndent()

        val params = MapSqlParameterSource("id", id)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToUser(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun findByEmail(email: String): User? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT id, email, username, password_hash, is_email_verified, created_at, updated_at, is_active
            FROM users
            WHERE email = :email AND is_active = true
        """.trimIndent()

        val params = MapSqlParameterSource("email", email)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToUser(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun update(user: User): User = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE users 
            SET email = :email, username = :username, password_hash = :passwordHash, 
                is_email_verified = :isEmailVerified, updated_at = :updatedAt, is_active = :isActive
            WHERE id = :id
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("id", user.id)
            .addValue("email", user.email)
            .addValue("username", user.username)
            .addValue("passwordHash", user.passwordHash)
            .addValue("isEmailVerified", user.isEmailVerified)
            .addValue("updatedAt", Timestamp.from(user.updatedAt))
            .addValue("isActive", user.isActive)

        jdbcTemplate.update(sql, params)
        user
    }

    override suspend fun delete(id: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "UPDATE users SET is_active = false, updated_at = CURRENT_TIMESTAMP WHERE id = :id"
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(sql, params) > 0
    }

    override suspend fun existsByEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        val sql = "SELECT COUNT(*) FROM users WHERE email = :email AND is_active = true"
        val params = MapSqlParameterSource("email", email)
        val count = jdbcTemplate.queryForObject(sql, params, Int::class.java) ?: 0
        count > 0
    }

    private fun mapRowToUser(rs: ResultSet): User {
        return User(
            id = UUID.fromString(rs.getString("id")),
            email = rs.getString("email"),
            username = rs.getString("username"),
            passwordHash = rs.getString("password_hash"),
            isEmailVerified = rs.getBoolean("is_email_verified"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant(),
            isActive = rs.getBoolean("is_active")
        )
    }
}

@Repository
class JdbcUserProfileRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper
) : UserProfileRepository {

    override suspend fun create(userProfile: UserProfile): UserProfile = withContext(Dispatchers.IO) {
        val sql = """
            INSERT INTO user_profiles (user_id, first_name, last_name, date_of_birth, gender, location, privacy_settings, updated_at)
            VALUES (:userId, :firstName, :lastName, :dateOfBirth, :gender, :location, :privacySettings::jsonb, :updatedAt)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userProfile.userId)
            .addValue("firstName", userProfile.firstName)
            .addValue("lastName", userProfile.lastName)
            .addValue("dateOfBirth", userProfile.dateOfBirth?.let { LocalDate.parse(it) })
            .addValue("gender", userProfile.gender?.name)
            .addValue("location", userProfile.location)
            .addValue("privacySettings", objectMapper.writeValueAsString(userProfile.privacySettings))
            .addValue("updatedAt", Timestamp.from(userProfile.updatedAt))

        jdbcTemplate.update(sql, params)
        userProfile
    }

    override suspend fun findByUserId(userId: UUID): UserProfile? = withContext(Dispatchers.IO) {
        val sql = """
            SELECT user_id, first_name, last_name, date_of_birth, gender, location, privacy_settings, updated_at
            FROM user_profiles
            WHERE user_id = :userId
        """.trimIndent()

        val params = MapSqlParameterSource("userId", userId)

        try {
            jdbcTemplate.queryForObject(sql, params) { rs, _ -> mapRowToUserProfile(rs) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun update(userProfile: UserProfile): UserProfile = withContext(Dispatchers.IO) {
        val sql = """
            UPDATE user_profiles 
            SET first_name = :firstName, last_name = :lastName, date_of_birth = :dateOfBirth,
                gender = :gender, location = :location, privacy_settings = :privacySettings::jsonb, updated_at = :updatedAt
            WHERE user_id = :userId
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userProfile.userId)
            .addValue("firstName", userProfile.firstName)
            .addValue("lastName", userProfile.lastName)
            .addValue("dateOfBirth", userProfile.dateOfBirth?.let { LocalDate.parse(it) })
            .addValue("gender", userProfile.gender?.name)
            .addValue("location", userProfile.location)
            .addValue("privacySettings", objectMapper.writeValueAsString(userProfile.privacySettings))
            .addValue("updatedAt", Timestamp.from(userProfile.updatedAt))

        jdbcTemplate.update(sql, params)
        userProfile
    }

    override suspend fun delete(userId: UUID): Boolean = withContext(Dispatchers.IO) {
        val sql = "DELETE FROM user_profiles WHERE user_id = :userId"
        val params = MapSqlParameterSource("userId", userId)
        jdbcTemplate.update(sql, params) > 0
    }

    private fun mapRowToUserProfile(rs: ResultSet): UserProfile {
        val privacySettingsJson = rs.getString("privacy_settings")
        val privacySettings = if (privacySettingsJson.isNullOrBlank()) {
            UserPrivacySettings()
        } else {
            objectMapper.readValue(privacySettingsJson, UserPrivacySettings::class.java)
        }

        return UserProfile(
            userId = UUID.fromString(rs.getString("user_id")),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            dateOfBirth = rs.getDate("date_of_birth")?.toString(),
            gender = rs.getString("gender")?.let { Gender.valueOf(it) },
            location = rs.getString("location"),
            privacySettings = privacySettings,
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )
    }
}