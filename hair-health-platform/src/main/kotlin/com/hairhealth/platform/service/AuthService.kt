package com.hairhealth.platform.service

import com.hairhealth.platform.domain.User
import com.hairhealth.platform.repository.UserRepository
import com.hairhealth.platform.security.JwtService
import com.hairhealth.platform.security.UserPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    suspend fun register(email: String, password: String, username: String?): AuthResponse {
        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        // Create user
        val hashedPassword = passwordEncoder.encode(password)
        val user = User(
            id = UUID.randomUUID(),
            email = email,
            username = username,
            passwordHash = hashedPassword,
            isEmailVerified = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            isActive = true
        )

        val createdUser = userRepository.create(user)
        
        // Generate tokens
        val userPrincipal = UserPrincipal(
            userId = createdUser.id,
            email = createdUser.email,
            username = createdUser.username,
            roles = listOf("USER") // Default role
        )

        val accessToken = jwtService.generateAccessToken(userPrincipal)
        val refreshToken = jwtService.generateRefreshToken(userPrincipal)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse(
                id = createdUser.id,
                email = createdUser.email,
                username = createdUser.username ?: "",
                isEmailVerified = createdUser.isEmailVerified
            )
        )
    }

    suspend fun login(email: String, password: String): AuthResponse {
        // Find user by email
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        // Verify password
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        // Check if user is active
        if (!user.isActive) {
            throw IllegalArgumentException("User account is deactivated")
        }

        // Generate tokens
        val userPrincipal = UserPrincipal(
            userId = user.id,
            email = user.email,
            username = user.username,
            roles = listOf("USER") // TODO: Add role management
        )

        val accessToken = jwtService.generateAccessToken(userPrincipal)
        val refreshToken = jwtService.generateRefreshToken(userPrincipal)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse(
                id = user.id,
                email = user.email,
                username = user.username ?: "",
                isEmailVerified = user.isEmailVerified
            )
        )
    }

    suspend fun refreshToken(refreshToken: String): AuthResponse {
        // Validate refresh token
        if (!jwtService.validateToken(refreshToken) || jwtService.isTokenExpired(refreshToken)) {
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        // Check token type
        if (jwtService.extractTokenType(refreshToken) != "refresh") {
            throw IllegalArgumentException("Invalid token type")
        }

        // Extract user info from token
        val userId = jwtService.extractUserIdFromToken(refreshToken)
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        // Generate new tokens
        val userPrincipal = UserPrincipal(
            userId = user.id,
            email = user.email,
            username = user.username,
            roles = listOf("USER")
        )

        val newAccessToken = jwtService.generateAccessToken(userPrincipal)
        val newRefreshToken = jwtService.generateRefreshToken(userPrincipal)

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            user = UserResponse(
                id = user.id,
                email = user.email,
                username = user.username ?: "",
                isEmailVerified = user.isEmailVerified
            )
        )
    }

    suspend fun getCurrentUser(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        return UserResponse(
            id = user.id,
            email = user.email,
            username = user.username ?: "",
            isEmailVerified = user.isEmailVerified
        )
    }

    suspend fun validateAccessToken(token: String): UserPrincipal? {
        return try {
            if (!jwtService.validateToken(token) || jwtService.isTokenExpired(token)) {
                return null
            }

            if (jwtService.extractTokenType(token) != "access") {
                return null
            }

            UserPrincipal(
                userId = jwtService.extractUserIdFromToken(token),
                email = jwtService.extractEmailFromToken(token),
                username = jwtService.extractUsernameFromToken(token),
                roles = jwtService.extractRolesFromToken(token)
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String,
    val isEmailVerified: Boolean
)