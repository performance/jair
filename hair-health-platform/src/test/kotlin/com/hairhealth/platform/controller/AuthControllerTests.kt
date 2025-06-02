package com.hairhealth.platform.controller

import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.AuthRequest // Assuming AuthRequest DTO exists for login
import com.hairhealth.platform.service.AuthResponse
import com.hairhealth.platform.service.AuthService
import com.hairhealth.platform.service.UserResponse
import com.hairhealth.platform.service.RegisterRequest // Assuming RegisterRequest DTO exists
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.util.UUID
import com.hairhealth.platform.security.JwtService // Needed for mock principal context potentially
import com.hairhealth.platform.config.SecurityConfig // Assuming SecurityConfig is where WebFluxSecurity is set up

// Assuming AuthRequest, RegisterRequest DTOs are defined something like this:
// package com.hairhealth.platform.service
// data class AuthRequest(val email: String, val password: String)
// data class RegisterRequest(val email: String, val password: String, val username: String?)

@WebFluxTest(AuthController::class)
@Import(SecurityConfig::class) // Import security config for WebFluxTest context
class AuthControllerTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean // Use @MockBean for Spring Boot DI, not manual mockk() for controller tests
    private lateinit var authService: AuthService

    // Mock JwtService if needed for creating mock UserPrincipal in SecurityContext
    @MockBean 
    private lateinit var jwtService: JwtService


    @Test
    fun `testRegister_ValidRequest_ReturnsAuthResponse`() {
        val registerRequest = RegisterRequest("new@example.com", "password123", "newuser")
        val userResponse = UserResponse(UUID.randomUUID(), "new@example.com", "newuser", false)
        val authResponse = AuthResponse("accessToken", "refreshToken", userResponse)

        coEvery { authService.register(registerRequest.email, registerRequest.password, registerRequest.username) } returns authResponse

        webTestClient.post().uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isCreated // Expect 201 Created for registration
            .expectBody(AuthResponse::class.java)
            .isEqualTo(authResponse)
    }
    
    @Test
    fun `testRegister_InvalidEmail_ReturnsBadRequest`() {
        // Assuming RegisterRequest has validation annotations for email, or service throws specific error
        val registerRequest = RegisterRequest("invalidemail", "password123", "newuser")
        // If service throws IllegalArgumentException for bad email format (before DB check)
        coEvery { authService.register(registerRequest.email, registerRequest.password, registerRequest.username) } throws IllegalArgumentException("Invalid email format")


        webTestClient.post().uri("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(registerRequest))
            .exchange()
            .expectStatus().isBadRequest // Or based on actual exception handling
    }


    @Test
    fun `testLogin_ValidCredentials_ReturnsAuthResponse`() {
        val authRequest = AuthRequest("test@example.com", "password")
        val userResponse = UserResponse(UUID.randomUUID(), "test@example.com", "testuser", true)
        val authResponse = AuthResponse("accessToken", "refreshToken", userResponse)

        coEvery { authService.login(authRequest.email, authRequest.password) } returns authResponse

        webTestClient.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(authRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody(AuthResponse::class.java)
            .isEqualTo(authResponse)
    }
    
    @Test
    fun `testLogin_InvalidCredentials_ReturnsUnauthorizedOrBadRequest`() {
        val authRequest = AuthRequest("test@example.com", "wrongpassword")
        coEvery { authService.login(authRequest.email, authRequest.password) } throws IllegalArgumentException("Invalid email or password")

        webTestClient.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(authRequest))
            .exchange()
            .expectStatus().isBadRequest // Or isUnauthorized based on actual exception handling
    }


    @Test
    fun `testGetMe_Authenticated_ReturnsUserResponse`() {
        val userId = UUID.randomUUID()
        val userEmail = "test@example.com"
        val userName = "testuser"
        val userResponse = UserResponse(userId, userEmail, userName, true)

        // Mock the service call that getCurrentUser would make
        coEvery { authService.getCurrentUser(userId) } returns userResponse
        
        // Setup mock UserPrincipal for the test request
        val mockPrincipal = UserPrincipal(userId, userEmail, userName, listOf("USER"))

        webTestClient
            .mutateWith(mockUser().principal(mockPrincipal)) // Mock the authenticated principal
            .get().uri("/api/v1/auth/me")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .isEqualTo(userResponse)
    }
    
    // Placeholder for RefreshToken test if endpoint exists
    // @Test
    // fun `testRefreshToken_Valid_ReturnsNewTokens`() {
    //     val refreshTokenRequest = mapOf("refreshToken" to "validRefreshToken") // Assuming simple map request
    //     val userResponse = UserResponse(UUID.randomUUID(), "test@example.com", "testuser", true)
    //     val authResponse = AuthResponse("newAccessToken", "newRefreshToken", userResponse)
    // 
    //     coEvery { authService.refreshToken("validRefreshToken") } returns authResponse
    // 
    //     webTestClient.post().uri("/api/v1/auth/refresh-token")
    //         .contentType(MediaType.APPLICATION_JSON)
    //         .body(BodyInserters.fromValue(refreshTokenRequest))
    //         .exchange()
    //         .expectStatus().isOk
    //         .expectBody(AuthResponse::class.java)
    //         .isEqualTo(authResponse)
    // }
}
