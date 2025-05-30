package com.hairhealth.platform.domain

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val username: String?,
    val passwordHash: String,
    val isEmailVerified: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isActive: Boolean = true
)

data class UserProfile(
    val userId: UUID,
    val firstName: String?,
    val lastName: String?,
    val dateOfBirth: String?, // YYYY-MM-DD format for privacy
    val gender: Gender?,
    val location: String?, // City, Country format
    val privacySettings: UserPrivacySettings,
    val updatedAt: Instant
)

enum class Gender {
    MALE, FEMALE, NON_BINARY, PREFER_NOT_TO_SAY
}

data class UserPrivacySettings(
    val shareDataForResearch: Boolean = false,
    val allowAnonymousForumPosting: Boolean = true,
    val shareProgressWithProfessionals: Boolean = false,
    val dataRetentionPreference: DataRetentionPreference = DataRetentionPreference.STANDARD
)

enum class DataRetentionPreference {
    MINIMAL, STANDARD, EXTENDED
}