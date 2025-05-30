package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.HairFallCategory
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.HairFallLogService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/me/hair-fall-logs")
class HairFallLogController(
    private val hairFallLogService: HairFallLogService
) {

    @PostMapping
    suspend fun createHairFallLog(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody request: CreateHairFallLogRequest
    ): HairFallLogResponse {
        val hairFallLog = hairFallLogService.createHairFallLog(
            userId = userPrincipal.userId, // Extract from JWT instead of request
            date = request.date,
            count = request.count,
            category = request.category,
            description = request.description,
            photoMetadataId = request.photoMetadataId
        )

        return hairFallLog.toResponse()
    }

    @GetMapping
    suspend fun getHairFallLogs(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): List<HairFallLogResponse> {
        val logs = hairFallLogService.getHairFallLogsByUserId(
            userId = userPrincipal.userId, // Extract from JWT
            limit = limit,
            offset = offset
        )
        return logs.map { it.toResponse() }
    }

    @GetMapping("/date-range")
    suspend fun getHairFallLogsByDateRange(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): List<HairFallLogResponse> {
        val logs = hairFallLogService.getHairFallLogsByDateRange(
            userId = userPrincipal.userId, // Extract from JWT
            startDate = startDate,
            endDate = endDate
        )
        return logs.map { it.toResponse() }
    }

    @GetMapping("/{id}")
    suspend fun getHairFallLog(@PathVariable id: UUID): HairFallLogResponse? {
        val log = hairFallLogService.getHairFallLogById(id)
        return log?.toResponse()
    }

    @PutMapping("/{id}")
    suspend fun updateHairFallLog(
        @PathVariable id: UUID,
        @RequestBody request: UpdateHairFallLogRequest
    ): HairFallLogResponse {
        val updatedLog = hairFallLogService.updateHairFallLog(
            id = id,
            date = request.date,
            count = request.count,
            category = request.category,
            description = request.description,
            photoMetadataId = request.photoMetadataId
        )
        return updatedLog.toResponse()
    }

    @DeleteMapping("/{id}")
    suspend fun deleteHairFallLog(@PathVariable id: UUID): Map<String, String> {
        val deleted = hairFallLogService.deleteHairFallLog(id)
        return if (deleted) {
            mapOf("status" to "deleted")
        } else {
            mapOf("status" to "not_found")
        }
    }

    @GetMapping("/stats")
    suspend fun getHairFallStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): HairFallStatsResponse {
        return hairFallLogService.getHairFallStats(userPrincipal.userId) // Extract from JWT
    }
}

// Updated Request DTOs (removed userId)
data class CreateHairFallLogRequest(
    val date: LocalDate,
    val count: Int?,
    val category: HairFallCategory,
    val description: String?,
    val photoMetadataId: UUID?
)

data class UpdateHairFallLogRequest(
    val date: LocalDate?,
    val count: Int?,
    val category: HairFallCategory?,
    val description: String?,
    val photoMetadataId: UUID?
)

data class HairFallLogResponse(
    val id: UUID,
    val userId: UUID,
    val date: LocalDate,
    val count: Int?,
    val category: HairFallCategory,
    val description: String?,
    val photoMetadataId: UUID?,
    val createdAt: String,
    val updatedAt: String
)

data class HairFallStatsResponse(
    val totalLogs: Long,
    val averageCount: Double?,
    val mostCommonCategory: HairFallCategory?,
    val recentTrend: String
)

// Extension function to convert domain to response
private fun com.hairhealth.platform.domain.HairFallLog.toResponse() = HairFallLogResponse(
    id = this.id,
    userId = this.userId,
    date = this.date,
    count = this.count,
    category = this.category,
    description = this.description,
    photoMetadataId = this.photoMetadataId,
    createdAt = this.createdAt.toString(),
    updatedAt = this.updatedAt.toString()
)