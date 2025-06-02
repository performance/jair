package com.hairhealth.platform.repository

import com.hairhealth.platform.domain.AuditLog

interface AuditLogRepository {
    /**
     * Saves an audit log entry to the database.
     * @param auditLog The AuditLog object to save.
     * @return The saved AuditLog object.
     */
    suspend fun save(auditLog: AuditLog): AuditLog
}
