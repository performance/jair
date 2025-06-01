package com.hairhealth.platform.controller

import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.InterventionService
import com.hairhealth.platform.service.InterventionNotFoundException
import com.hairhealth.platform.service.InterventionInteractionException
import com.hairhealth.platform.service.dto.CreateInterventionRequest
import com.hairhealth.platform.service.dto.InterventionApplicationResponse
import com.hairhealth.platform.service.dto.InterventionResponse
import com.hairhealth.platform.service.dto.LogApplicationRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import jakarta.validation.Valid
import java.util.UUID

@RestController
@RequestMapping("/api/v1/me/interventions")
// @PreAuthorize("hasAuthority('ROLE_USER')") // Add once security config is confirmed
class InterventionController(
    private val interventionService: InterventionService
) {

    @PostMapping
    suspend fun createIntervention(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody request: CreateInterventionRequest
    ): ResponseEntity<InterventionResponse> {
        try {
            val intervention = interventionService.createIntervention(userPrincipal.userId, request)
            return ResponseEntity.status(HttpStatus.CREATED).body(intervention)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(null) // Consider a proper error response DTO
        }
    }

    @GetMapping
    suspend fun getInterventions(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "false") includeInactive: Boolean
    ): ResponseEntity<List<InterventionResponse>> {
        val interventions = interventionService.getInterventionsForUser(userPrincipal.userId, includeInactive)
        return ResponseEntity.ok(interventions)
    }

    @GetMapping("/{id}")
    suspend fun getInterventionById(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: UUID
    ): ResponseEntity<InterventionResponse> {
        return interventionService.getInterventionById(userPrincipal.userId, id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PostMapping("/{id}/log-application")
    suspend fun logApplication(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: UUID, // This is interventionId
        @Valid @RequestBody request: LogApplicationRequest
    ): ResponseEntity<Any> { // Changed to ResponseEntity<Any> for error handling
        return try {
            val application = interventionService.logInterventionApplication(userPrincipal.userId, id, request)
            ResponseEntity.status(HttpStatus.CREATED).body(application)
        } catch (e: InterventionNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        } catch (e: InterventionInteractionException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to e.message))
        }
    }

    @GetMapping("/{id}/applications")
    suspend fun getApplications(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable id: UUID, // This is interventionId
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<Any> { // Changed to ResponseEntity<Any> for error handling
         return try {
            val applications = interventionService.getApplicationsForIntervention(userPrincipal.userId, id, limit, offset)
            ResponseEntity.ok(applications)
        } catch (e: InterventionNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
        }
    }
}