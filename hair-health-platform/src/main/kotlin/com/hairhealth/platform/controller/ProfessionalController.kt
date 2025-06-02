package com.hairhealth.platform.controller

import com.hairhealth.platform.security.UserPrincipal
import com.hairhealth.platform.service.AuthService // To reuse getCurrentUser
import com.hairhealth.platform.service.UserResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/professionals")
class ProfessionalController(
    private val authService: AuthService // Can reuse for fetching user details
) {
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_PROFESSIONAL')") // Security rule
    suspend fun getCurrentProfessionalProfile(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<UserResponse> {
        // For now, a professional's basic profile is their UserResponse
        // The @PreAuthorize annotation ensures only users with ROLE_PROFESSIONAL can access this.
        // The principal.userId is guaranteed to be that of the authenticated professional.
        try {
            val userResponse = authService.getCurrentUser(principal.userId)
            return ResponseEntity.ok(userResponse)
        } catch (e: IllegalArgumentException) { // e.g. if user not found, though unlikely for authenticated principal
            return ResponseEntity.notFound().build()
        }
    }
}
