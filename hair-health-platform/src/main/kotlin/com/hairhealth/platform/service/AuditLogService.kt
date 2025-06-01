package com.hairhealth.platform.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.hairhealth.platform.domain.ActorType
import com.hairhealth.platform.domain.AuditLog
import com.hairhealth.platform.domain.AuditEventStatus
import com.hairhealth.platform.repository.AuditLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class AuditLogService(
    private val auditLogRepository: AuditLogRepository,
    // Using default jacksonObjectMapper from com.fasterxml.jackson.module.kotlin
    // Or inject a pre-configured ObjectMapper bean if available/customized
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    private val logger = LoggerFactory.getLogger(AuditLogService::class.java)

    // For truly async logging that doesn't block the caller,
    // you might launch this in a separate CoroutineScope with a dedicated dispatcher.
    // For this subtask, direct suspend fun call is acceptable as per assumption.
    // If using a separate scope, error handling within that scope becomes important.
    // private val auditScope = CoroutineScope(Dispatchers.IO) // Example for dedicated scope

    suspend fun logEvent(
        actorId: String?,
        actorType: ActorType?,
        action: String,
        targetEntityType: String? = null,
        targetEntityId: String? = null,
        status: AuditEventStatus,
        ipAddress: String? = null,
        deviceInfo: String? = null,
        details: Map<String, Any?>? = null // Changed to Any? to allow nulls in map values
    ) {
        try {
            val detailsJson = details?.let {
                try {
                    objectMapper.writeValueAsString(it)
                } catch (e: Exception) {
                    logger.error("Error serializing audit log details to JSON: $it", e)
                    "{\"error\":\"Failed to serialize details\"}" // Fallback JSON
                }
            }

            val auditLog = AuditLog(
                id = UUID.randomUUID(),
                timestamp = Instant.now(),
                actorId = actorId,
                actorType = actorType,
                action = action,
                targetEntityType = targetEntityType,
                targetEntityId = targetEntityId,
                ipAddress = ipAddress,
                deviceInfo = deviceInfo,
                status = status,
                details = detailsJson
            )
            auditLogRepository.save(auditLog)
        } catch (e: Exception) {
            // Critical: Audit logging should not interrupt the main application flow.
            logger.error("Failed to save audit log: ${e.message}", e)
            // Depending on policy, might re-throw if audit is absolutely critical and must block,
            // but generally it's better to log the failure and continue.
        }
    }

    // Overloaded version for convenience if details are already a JSON string
    suspend fun logEventWithJsonDetails(
        actorId: String?,
        actorType: ActorType?,
        action: String,
        targetEntityType: String? = null,
        targetEntityId: String? = null,
        status: AuditEventStatus,
        ipAddress: String? = null,
        deviceInfo: String? = null,
        detailsJson: String? = null
    ) {
         try {
            val auditLog = AuditLog(
                id = UUID.randomUUID(),
                timestamp = Instant.now(),
                actorId = actorId,
                actorType = actorType,
                action = action,
                targetEntityType = targetEntityType,
                targetEntityId = targetEntityId,
                ipAddress = ipAddress,
                deviceInfo = deviceInfo,
                status = status,
                details = detailsJson
            )
            auditLogRepository.save(auditLog)
        } catch (e: Exception) {
            logger.error("Failed to save audit log with JSON details: ${e.message}", e)
        }
    }
}
