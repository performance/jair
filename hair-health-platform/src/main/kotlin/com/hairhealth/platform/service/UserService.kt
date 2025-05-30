package com.hairhealth.platform.service

import com.hairhealth.platform.domain.User
import com.hairhealth.platform.domain.UserProfile
import com.hairhealth.platform.repository.UserRepository
import com.hairhealth.platform.repository.UserProfileRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository
) {

    suspend fun createUser(email: String, username: String?, passwordHash: String): User {
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        val user = User(
            id = UUID.randomUUID(),
            email = email,
            username = username,
            passwordHash = passwordHash,
            isEmailVerified = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isActive = true
        )

        return userRepository.create(user)
    }

    suspend fun findUserById(id: UUID): User? {
        return userRepository.findById(id)
    }

    suspend fun findUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    suspend fun createUserProfile(userProfile: UserProfile): UserProfile {
        return userProfileRepository.create(userProfile)
    }

    suspend fun getUserProfile(userId: UUID): UserProfile? {
        return userProfileRepository.findByUserId(userId)
    }

    suspend fun updateUserProfile(userProfile: UserProfile): UserProfile {
        return userProfileRepository.update(userProfile.copy(updatedAt = Instant.now()))
    }
}