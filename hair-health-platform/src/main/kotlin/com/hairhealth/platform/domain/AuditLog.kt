package com.hairhealth.platform.domain

import java.time.Instant
import java.util.UUID

enum class ActorType {
    USER,
    PROFESSIONAL,
    SYSTEM
}

enum class AuditEventStatus {
    SUCCESS,
    FAILURE
}

data class AuditLog(
    val id: UUID = UUID.randomUUID(),
    val timestamp: Instant = Instant.now(),
    val actorId: String?, // User ID, Professional ID, or system identifier like "SYSTEM_SCHEDULER"
    val actorType: ActorType?,
    val action: String, // e.g., "USER_LOGIN_SUCCESS", "CREATE_RECOMMENDATION"
    val targetEntityType: String?, // e.g., "USER", "RECOMMENDATION", "INTERVENTION"
    val targetEntityId: String?, // ID of the entity being acted upon
    val ipAddress: String?,
    val deviceInfo: String?, // e.g., User-Agent
    val status: AuditEventStatus,
    val details: String? // JSON string for additional context-specific information
)
