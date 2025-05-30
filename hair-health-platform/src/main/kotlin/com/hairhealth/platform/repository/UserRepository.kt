package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.User
import com.hairhealth.platform.domain.UserProfile
import java.util.UUID

interface UserRepository {
    suspend fun create(user: User): User
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun update(user: User): User
    suspend fun delete(id: UUID): Boolean
    suspend fun existsByEmail(email: String): Boolean
}

interface UserProfileRepository {
    suspend fun create(userProfile: UserProfile): UserProfile
    suspend fun findByUserId(userId: UUID): UserProfile?
    suspend fun update(userProfile: UserProfile): UserProfile
    suspend fun delete(userId: UUID): Boolean
}