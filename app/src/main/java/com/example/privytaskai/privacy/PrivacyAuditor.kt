package com.example.privytaskai.privacy

data class PrivacyReport(
    val dataFlowAudits: List<DataFlowAudit>,
    val permissionAudits: List<PermissionAudit>,
    val localProcessingValidations: List<ProcessingValidation>,
    val timestamp: Long = System.currentTimeMillis()
)

data class DataFlowAudit(
    val operation: String,
    val dataType: String,
    val destination: String,
    val timestamp: Long,
    val isCompliant: Boolean
)

data class PermissionAudit(
    val permission: String,
    val justification: String,
    val timestamp: Long,
    val isApproved: Boolean
)

data class ProcessingValidation(
    val operation: String,
    val isLocal: Boolean,
    val timestamp: Long
)

interface PrivacyAuditor {
    suspend fun auditDataFlow(operation: String, dataType: String, destination: String)
    suspend fun validateLocalProcessing(operation: String): Boolean
    suspend fun checkPermissionUsage(permission: String, justification: String)
    fun getPrivacyReport(): PrivacyReport
}