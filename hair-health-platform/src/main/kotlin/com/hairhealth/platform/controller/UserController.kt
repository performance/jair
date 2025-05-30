package com.hairhealth.platform.controller

import com.hairhealth.platform.service.UserService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {

    @PostMapping("/test")
    suspend fun createTestUser(@RequestBody request: CreateTestUserRequest): Map<String, Any> {
        val user = userService.createUser(
            email = request.email,
            username = request.username,
            passwordHash = "dummy_hash_${System.currentTimeMillis()}"
        )

        return mapOf(
            "id" to user.id,
            "email" to user.email,
            "username" to (user.username ?: ""),
            "createdAt" to user.createdAt
        )
    }

    @GetMapping("/{id}")
    suspend fun getUser(@PathVariable id: UUID): Map<String, Any>? {
        val user = userService.findUserById(id)
        return user?.let {
            mapOf(
                "id" to it.id,
                "email" to it.email,
                "username" to (user.username ?: ""),
                "createdAt" to it.createdAt,
                "isEmailVerified" to it.isEmailVerified
            )
        }
    }
}

data class CreateTestUserRequest(
    val email: String,
    val username: String?
)