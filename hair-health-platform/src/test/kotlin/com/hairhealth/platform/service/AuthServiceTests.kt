package com.hairhealth.platform.service

import com.hairhealth.platform.domain.User
import com.hairhealth.platform.repository.UserRepository
import com.hairhealth.platform.security.JwtService
import com.hairhealth.platform.security.UserPrincipal
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

// Example using @SpringBootTest might be too heavy for pure service unit tests
// Consider a more focused setup if not needing full context, or use @ExtendWith(SpringExtension::class) with @ContextConfiguration
// For simplicity here, we'll direct instantiate with mocks.
class AuthServiceTests {

    private lateinit var userRepository: UserRepository
    private lateinit var jwtService: JwtService
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var auditLogService: AuditLogService // Added mock for AuditLogService
    private lateinit var authService: AuthService

    private val testUser = User(
        id = UUID.randomUUID(),
        email = "test@example.com",
        username = "testuser",
        passwordHash = "hashedPassword",
        isEmailVerified = true,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        isActive = true
    )

    private val userPrincipal = UserPrincipal(testUser.id, testUser.email, testUser.username!!, listOf("USER"))

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        jwtService = mockk()
        passwordEncoder = mockk()
        auditLogService = mockk(relaxed = true) // relaxed = true to ignore void audit calls for now
        authService = AuthService(userRepository, jwtService, passwordEncoder, auditLogService)
    }

    @Test
    fun `testRegisterUser_Success`() = runBlocking {
        coEvery { userRepository.existsByEmail(any()) } returns false
        coEvery { passwordEncoder.encode(any()) } returns "hashedPassword"
        coEvery { userRepository.create(any()) } returns testUser
        every { jwtService.generateAccessToken(any()) } returns "accessToken"
        every { jwtService.generateRefreshToken(any()) } returns "refreshToken"

        val response = authService.register("test@example.com", "password", "testuser")

        assertNotNull(response)
        assertEquals("accessToken", response.accessToken)
        assertEquals(testUser.id, response.user.id)

        // Verify audit log was called for success (example)
        // coVerify { auditLogService.logEvent(actorId = testUser.id.toString(), action = "USER_REGISTRATION_SUCCESS", status = AuditEventStatus.SUCCESS, any(), any(), any(), any(), any() ) }
    }

    @Test
    fun `testRegisterUser_EmailExists_ThrowsIllegalArgumentException`() = runBlocking {
        coEvery { userRepository.existsByEmail("test@example.com") } returns true

        assertThrows<IllegalArgumentException> {
            authService.register("test@example.com", "password", "testuser")
        }
        // Verify audit log for failure (example)
        // coVerify { auditLogService.logEvent(actorId = "test@example.com", action = "USER_REGISTRATION_ATTEMPT_FAILURE", status = AuditEventStatus.FAILURE, any(), any(), any(), any(), any() ) }
    }

    @Test
    fun `testLoginUser_Success`() = runBlocking {
        coEvery { userRepository.findByEmail("test@example.com") } returns testUser
        every { passwordEncoder.matches("password", "hashedPassword") } returns true
        every { jwtService.generateAccessToken(any()) } returns "accessToken"
        every { jwtService.generateRefreshToken(any()) } returns "refreshToken"

        val response = authService.login("test@example.com", "password")

        assertNotNull(response)
        assertEquals("accessToken", response.accessToken)
    }

    @Test
    fun `testLoginUser_UserNotFound_ThrowsIllegalArgumentException`() = runBlocking {
        coEvery { userRepository.findByEmail(any()) } returns null

        assertThrows<IllegalArgumentException> {
            authService.login("unknown@example.com", "password")
        }
    }

    @Test
    fun `testLoginUser_InvalidPassword_ThrowsIllegalArgumentException`() = runBlocking {
        coEvery { userRepository.findByEmail("test@example.com") } returns testUser
        every { passwordEncoder.matches("wrongpassword", "hashedPassword") } returns false

        assertThrows<IllegalArgumentException> {
            authService.login("test@example.com", "wrongpassword")
        }
    }

    @Test
    fun `testLoginUser_InactiveUser_ThrowsIllegalArgumentException`() = runBlocking {
        val inactiveUser = testUser.copy(isActive = false)
        coEvery { userRepository.findByEmail("test@example.com") } returns inactiveUser
        every { passwordEncoder.matches("password", "hashedPassword") } returns true

        assertThrows<IllegalArgumentException> {
            authService.login("test@example.com", "password")
        }
    }


    @Test
    fun `testRefreshToken_Success`() = runBlocking {
        every { jwtService.validateToken("validRefreshToken") } returns true
        every { jwtService.isTokenExpired("validRefreshToken") } returns false
        every { jwtService.extractTokenType("validRefreshToken") } returns "refresh"
        every { jwtService.extractUserIdFromToken("validRefreshToken") } returns userPrincipal.userId
        coEvery { userRepository.findById(userPrincipal.userId) } returns testUser
        every { jwtService.generateAccessToken(any()) } returns "newAccessToken"
        every { jwtService.generateRefreshToken(any()) } returns "newRefreshToken"

        val response = authService.refreshToken("validRefreshToken")

        assertNotNull(response)
        assertEquals("newAccessToken", response.accessToken)
        assertEquals("newRefreshToken", response.refreshToken)
    }

    @Test
    fun `testRefreshToken_InvalidToken_ThrowsIllegalArgumentException`() = runBlocking {
        every { jwtService.validateToken("invalidToken") } returns false
         // No need to mock isTokenExpired if validateToken is false
        assertThrows<IllegalArgumentException> {
            authService.refreshToken("invalidToken")
        }
    }

    @Test
    fun `testRefreshToken_ExpiredToken_ThrowsIllegalArgumentException`() = runBlocking {
        every { jwtService.validateToken("expiredToken") } returns true // Valid structure
        every { jwtService.isTokenExpired("expiredToken") } returns true // But expired

        assertThrows<IllegalArgumentException> {
            authService.refreshToken("expiredToken")
        }
    }

    @Test
    fun `testRefreshToken_WrongTokenType_ThrowsIllegalArgumentException`() = runBlocking {
        every { jwtService.validateToken("accessTokenAsRefreshToken") } returns true
        every { jwtService.isTokenExpired("accessTokenAsRefreshToken") } returns false
        every { jwtService.extractTokenType("accessTokenAsRefreshToken") } returns "access" // Wrong type

        assertThrows<IllegalArgumentException> {
            authService.refreshToken("accessTokenAsRefreshToken")
        }
    }

    @Test
    fun `testRefreshToken_UserNotFoundForToken_ThrowsIllegalArgumentException`() = runBlocking {
        val nonExistentUserId = UUID.randomUUID()
        every { jwtService.validateToken("validTokenUserNotFound") } returns true
        every { jwtService.isTokenExpired("validTokenUserNotFound") } returns false
        every { jwtService.extractTokenType("validTokenUserNotFound") } returns "refresh"
        every { jwtService.extractUserIdFromToken("validTokenUserNotFound") } returns nonExistentUserId
        coEvery { userRepository.findById(nonExistentUserId) } returns null // User not found

        assertThrows<IllegalArgumentException> {
            authService.refreshToken("validTokenUserNotFound")
        }
    }
}
