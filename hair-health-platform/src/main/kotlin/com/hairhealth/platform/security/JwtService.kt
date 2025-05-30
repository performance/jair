package com.hairhealth.platform.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService {

    @Value("\${app.jwt.secret:hair-health-platform-super-secret-key-for-development-only-change-in-production}")
    private lateinit var jwtSecret: String

    @Value("\${app.jwt.access-token-expiration:3600}")
    private var accessTokenExpiration: Long = 3600 // 1 hour

    @Value("\${app.jwt.refresh-token-expiration:604800}")
    private var refreshTokenExpiration: Long = 604800 // 7 days

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateAccessToken(userDetails: UserPrincipal): String {
        val now = Instant.now()
        val expiry = now.plus(accessTokenExpiration, ChronoUnit.SECONDS)

        return Jwts.builder()
            .subject(userDetails.userId.toString())
            .claim("email", userDetails.email)
            .claim("username", userDetails.username)
            .claim("roles", userDetails.roles)
            .claim("type", "access")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(userDetails: UserPrincipal): String {
        val now = Instant.now()
        val expiry = now.plus(refreshTokenExpiration, ChronoUnit.SECONDS)

        return Jwts.builder()
            .subject(userDetails.userId.toString())
            .claim("email", userDetails.email)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractUserIdFromToken(token: String): UUID {
        val claims = extractAllClaims(token)
        return UUID.fromString(claims.subject)
    }

    fun extractEmailFromToken(token: String): String {
        val claims = extractAllClaims(token)
        return claims["email"] as String
    }

    fun extractUsernameFromToken(token: String): String? {
        val claims = extractAllClaims(token)
        return claims["username"] as String?
    }

    fun extractRolesFromToken(token: String): List<String> {
        val claims = extractAllClaims(token)
        @Suppress("UNCHECKED_CAST")
        return claims["roles"] as? List<String> ?: emptyList()
    }

    fun extractTokenType(token: String): String {
        val claims = extractAllClaims(token)
        return claims["type"] as? String ?: "access"
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val claims = extractAllClaims(token)
            claims.expiration.before(Date())
        } catch (e: Exception) {
            true
        }
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}

data class UserPrincipal(
    val userId: UUID,
    val email: String,
    val username: String?,
    val roles: List<String>
)