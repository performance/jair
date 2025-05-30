package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.*
import com.hairhealth.platform.service.HairFallLogService
import com.hairhealth.platform.service.InterventionService
import com.hairhealth.platform.service.UserService
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/dev")
class DevController(
    private val userService: UserService,
    private val hairFallLogService: HairFallLogService,
    private val interventionService: InterventionService
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

    @PostMapping("/setup-intervention-data")
    suspend fun setupInterventionData(@RequestParam userId: String): Map<String, Any> {
    val userUuid = UUID.fromString(userId)
    
    // Create sample interventions
    val minoxidil = interventionService.createIntervention(
        userId = userUuid,
        type = InterventionType.TOPICAL,
        productName = "Minoxidil 5%",
        dosageAmount = "1ml",
        frequency = "Twice Daily",
        applicationTime = "08:00, 20:00",
        startDate = LocalDate.now().minusDays(30),
        endDate = null,
        provider = null,
        notes = "Apply to dry scalp, massage gently",
        sourceRecommendationId = null
    )
    
    val finasteride = interventionService.createIntervention(
        userId = userUuid,
        type = InterventionType.ORAL,
        productName = "Finasteride 1mg",
        dosageAmount = "1mg",
        frequency = "Once Daily",
        applicationTime = "08:00",
        startDate = LocalDate.now().minusDays(60),
        endDate = null,
        provider = "Dr. Smith",
        notes = "Take with breakfast",
        sourceRecommendationId = null
    )
    
    // Create sample applications for the last week
    val sampleApplications = mutableListOf<Map<String, Any>>()
    for (i in 1..7) {
        // Minoxidil applications (twice daily)
        val morningApp = interventionService.logApplication(
            interventionId = minoxidil.id,
            userId = userUuid,
            timestamp = LocalDate.now().minusDays(i.toLong()).atTime(8, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
            notes = if (i == 1) "Forgot evening dose" else null
        )
        
        if (i != 1) { // Skip one evening application to simulate missed dose
            val eveningApp = interventionService.logApplication(
                interventionId = minoxidil.id,
                userId = userUuid,
                timestamp = LocalDate.now().minusDays(i.toLong()).atTime(20, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
                notes = null
            )
            sampleApplications.add(mapOf("type" to "evening", "date" to LocalDate.now().minusDays(i.toLong())))
       }
       
       sampleApplications.add(mapOf("type" to "morning", "date" to LocalDate.now().minusDays(i.toLong())))
       
       // Finasteride applications (once daily)
       val finasterideApp = interventionService.logApplication(
           interventionId = finasteride.id,
           userId = userUuid,
           timestamp = LocalDate.now().minusDays(i.toLong()).atTime(8, 30).atZone(java.time.ZoneId.systemDefault()).toInstant(),
           notes = null
       )
    }
   
    return mapOf(
        "interventions" to listOf(
            mapOf(
                "id" to minoxidil.id,
                "productName" to minoxidil.productName,
                "type" to minoxidil.type,
                "frequency" to minoxidil.frequency
            ),
            mapOf(
                "id" to finasteride.id,
                "productName" to finasteride.productName,
                "type" to finasteride.type,
                "frequency" to finasteride.frequency
            )
        ),
        "applicationsCreated" to sampleApplications.size,
        "note" to "Sample intervention data created for testing"
    )
  }
}