package com.hairhealth.platform.service.dto

import com.hairhealth.platform.domain.Intervention // Assuming Intervention domain object
import java.util.UUID

// SiaService-specific DTOs

data class UserContext(
    val userId: UUID,
    val activeInterventions: List<Intervention>,
    val routinePreferences: Map<String, Any>?, // e.g., preferred times, existing habits
    val adherenceHistory: Map<String, Double>?, // e.g., interventionId to adherence score
    val lifestyleFactors: Map<String, Any>? // e.g., stress level, diet type
    // Add other relevant fields as SIA logic evolves
)

// Other DTOs related to SIA, like plan creation requests/responses, will go here later.
// For example:
// data class CreateImplementationPlanRequest( ... )
// data class ImplementationPlanResponse( ... )
// data class ImplementationPhaseResponse( ... )
// data class PhaseActionResponse( ... )
// etc.
