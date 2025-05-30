package com.hairhealth.platform.controller

import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.AuthResponse
import com.hairhealth.platform.service.AuthService
import com.hairhealth.platform.service.UserResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    suspend fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(
            email = request.email,
            password = request.password,
            username = request.username
        )
    }

    @PostMapping("/login")
    suspend fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(
            email = request.email,
            password = request.password
        )
    }

    @PostMapping("/refresh-token")
    suspend fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse {
        return authService.refreshToken(request.refreshToken)
    }

    @GetMapping("/me")
    suspend fun getCurrentUser(@AuthenticationPrincipal userPrincipal: UserPrincipal): UserResponse {
        return authService.getCurrentUser(userPrincipal.userId)
    }

    @PostMapping("/logout")
    suspend fun logout(): Map<String, String> {
        // For JWT-based auth, logout is typically handled client-side by removing the token
        // In a more sophisticated system, you might maintain a blacklist of revoked tokens
        return mapOf("message" to "Logged out successfully")
    }
}

data class RegisterRequest(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    val username: String?
)

data class LoginRequest(
    @field:Email(message = "Email must be valid")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)