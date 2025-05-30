package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.service.HairFallLogService
import com.hairhealth.platform.service.UserService
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/dev")
class DevController(
    private val userService: UserService,
    private val hairFallLogService: HairFallLogService
) {

    @PostMapping("/setup-test-user")
    suspend fun setupTestUser(): Map<String, Any> {
        // Create a test user
        val user = userService.createUser(
            email = "testuser@hairhealth.com",
            username = "testuser",
            passwordHash = "dummy_hash_dev"
        )

        // Create a user profile
        val userProfile = UserProfile(
            userId = user.id,
            firstName = "Test",
            lastName = "User",
            dateOfBirth = "1990-01-01",
            gender = Gender.MALE,
            location = "San Francisco, CA",
            privacySettings = UserPrivacySettings(
                shareDataForResearch = true,
                allowAnonymousForumPosting = true
            ),
            updatedAt = Instant.now()
        )
        userService.createUserProfile(userProfile)

        // Create some sample hair fall logs
        val sampleLogs = mutableListOf<Map<String, Any>>()
        
        for (i in 1..10) {
            val log = hairFallLogService.createHairFallLog(
                userId = user.id,
                date = LocalDate.now().minusDays(i.toLong()),
                count = (20..80).random(),
                category = HairFallCategory.values().random(),
                description = "Sample log entry $i",
                photoMetadataId = null
            )
            sampleLogs.add(
                mapOf(
                    "id" to (log.id ?: 0) as Any,
                    "date" to log.date as Any,
                    "count" to log.count as Any,
                    "category" to log.category as Any
                )
            )
        }

        return mapOf(
            "user" to mapOf(
                "id" to user.id,
                "email" to user.email,
                "username" to (user.username ?: "")
            ),
            "profile" to mapOf(
                "firstName" to (userProfile.firstName ?: ""),
                "lastName" to (userProfile.lastName ?: ""),
                "location" to (userProfile.location ?: "")
            ),
            "sampleLogs" to sampleLogs
        )
    }
}