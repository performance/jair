package com.hairhealth.platform.controller

import com.hairhealth.platform.domain.HairFallCategory // Keep this if service expects enum
import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.HairFallLogService
import com.hairhealth.platform.service.dto.CreateHairFallLogRequest // Import new DTO
import com.hairhealth.platform.service.dto.HairFallLogResponse // Import new DTO
import com.hairhealth.platform.service.dto.toResponse // Import mapper
// It seems toDomain is not directly used here if service expects raw params for create
// import com.hairhealth.platform.service.dto.toDomain
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid // For @Valid if using Spring Boot 3+
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/api/v1/me/hair-fall-logs")
// @PreAuthorize("hasAuthority('ROLE_USER')") // Add once security config is confirmed
class HairFallLogController(
    private val hairFallLogService: HairFallLogService
) {

    @PostMapping
    suspend fun createHairFallLog(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CreateHairFallLogRequest // Use DTO from service.dto
    ): ResponseEntity<HairFallLogResponse> {
        // Adapt to existing service method signature
        val categoryEnum = try {
            HairFallCategory.valueOf(request.category.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category: ${request.category}")
        }

        val domainHairFallLog = hairFallLogService.createHairFallLog(
            userId = userPrincipal.userId,
            date = request.date,
            count = request.count,
            category = categoryEnum, // Use converted enum
            description = request.description,
            photoMetadataId = request.photoMetadataId
        )
        // Map domain object to DTO response
        return ResponseEntity.status(HttpStatus.CREATED).body(domainHairFallLog.toResponse())
    }

    @GetMapping
    suspend fun getHairFallLogs(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "50") limit: Int, // Default to 50 as per existing service
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<List<HairFallLogResponse>> {
        val logs = hairFallLogService.getHairFallLogsByUserId(
            userId = userPrincipal.userId,
            limit = limit,
            offset = offset
        )
        return ResponseEntity.ok(logs.map { it.toResponse() }) // Map list items
    }

    @GetMapping("/date-range")
    suspend fun getHairFallLogsByDateRange(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<HairFallLogResponse>> {
        val logs = hairFallLogService.getHairFallLogsByDateRange(
            userId = userPrincipal.userId,
            startDate = startDate,
            endDate = endDate
        )
        return ResponseEntity.ok(logs.map { it.toResponse() }) // Map list items
    }

    @GetMapping("/{id}")
    suspend fun getHairFallLogById(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<HairFallLogResponse> {
        val log = hairFallLogService.getHairFallLogById(id) // Service fetches by id only
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Hair fall log not found")

        // CRITICAL: Enforce user ownership in controller due to service layer limitation
        if (log.userId != userPrincipal.userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this hair fall log")
        }
        return ResponseEntity.ok(log.toResponse())
    }
}