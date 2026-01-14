package com.n8n.mobilemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Représente une instance n8n configurée
 */
@Entity(tableName = "instances")
data class N8nInstance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val isActive: Boolean = false,
    val lastConnectedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Statut de connexion d'une instance
 */
data class InstanceStatus(
    val isOnline: Boolean,
    val version: String? = null,
    val databaseType: String? = null,
    val executionMode: String? = null,
    val activeWorkflows: Int = 0,
    val totalWorkflows: Int = 0,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

/**
 * Représente un workflow n8n
 */
data class Workflow(
    val id: String,
    val name: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<Tag> = emptyList(),
    val nodes: List<Node> = emptyList(),
    val connections: Map<String, Any>? = null,
    val settings: WorkflowSettings? = null
)

/**
 * Tag associé à un workflow
 */
data class Tag(
    val id: String,
    val name: String
)

/**
 * Nœud dans un workflow
 */
data class Node(
    val id: String,
    val name: String,
    val type: String,
    val position: List<Double>,
    val parameters: Map<String, Any>? = null,
    val typeVersion: Double = 1.0,
    val disabled: Boolean = false,
    val credentials: Map<String, Any>? = null
)

/**
 * Paramètres d'un workflow
 */
data class WorkflowSettings(
    val saveExecutionProgress: Boolean? = null,
    val saveManualExecutions: Boolean? = null,
    val saveDataErrorExecution: String? = null,
    val saveDataSuccessExecution: String? = null,
    val executionTimeout: Int? = null,
    val timezone: String? = null
)

/**
 * Représente une exécution de workflow
 */
data class Execution(
    val id: String,
    val workflowId: String,
    val workflowName: String? = null,
    val finished: Boolean,
    val mode: ExecutionMode,
    val status: ExecutionStatus,
    val startedAt: String,
    val stoppedAt: String? = null,
    val data: ExecutionData? = null,
    val retryOf: String? = null,
    val retrySuccessId: String? = null
)

/**
 * Mode d'exécution
 */
enum class ExecutionMode {
    MANUAL,
    TRIGGER,
    WEBHOOK,
    CLI,
    RETRY,
    INTERNAL;
    
    companion object {
        fun fromString(value: String): ExecutionMode {
            return when (value.lowercase()) {
                "manual" -> MANUAL
                "trigger" -> TRIGGER
                "webhook" -> WEBHOOK
                "cli" -> CLI
                "retry" -> RETRY
                "internal" -> INTERNAL
                else -> MANUAL
            }
        }
    }
}

/**
 * Statut d'une exécution
 */
enum class ExecutionStatus {
    RUNNING,
    SUCCESS,
    ERROR,
    CANCELED,
    WAITING,
    CRASHED,
    QUEUED;
    
    companion object {
        fun fromString(value: String): ExecutionStatus {
            return when (value.lowercase()) {
                "running" -> RUNNING
                "success" -> SUCCESS
                "error" -> ERROR
                "canceled" -> CANCELED
                "waiting" -> WAITING
                "crashed" -> CRASHED
                "queued" -> QUEUED
                else -> ERROR
            }
        }
    }
}

/**
 * Données d'exécution détaillées
 */
data class ExecutionData(
    val resultData: ResultData? = null,
    val executionData: Any? = null
)

/**
 * Résultat d'exécution
 */
data class ResultData(
    val runData: Map<String, Any>? = null,
    val lastNodeExecuted: String? = null,
    val error: ExecutionError? = null
)

/**
 * Erreur d'exécution
 */
data class ExecutionError(
    val message: String,
    val name: String? = null,
    val node: String? = null,
    val stack: String? = null,
    val timestamp: Long? = null
)

/**
 * Représente un credential stocké dans n8n
 */
data class Credential(
    val id: String,
    val name: String,
    val type: String,
    val createdAt: String,
    val updatedAt: String,
    val nodesAccess: List<NodeAccess> = emptyList(),
    val sharedWith: List<String> = emptyList()
)

/**
 * Accès d'un nœud à un credential
 */
data class NodeAccess(
    val nodeType: String,
    val date: String? = null
)

/**
 * Variable d'environnement n8n
 */
data class Variable(
    val id: String,
    val key: String,
    val value: String,
    val type: String = "string"
)

/**
 * Statistiques globales de l'instance
 */
data class InstanceStats(
    val totalWorkflows: Int = 0,
    val activeWorkflows: Int = 0,
    val totalExecutions: Int = 0,
    val isTotalExecutionsEstimated: Boolean = false,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0,
    val averageExecutionTime: Long = 0,
    val executionsLastHour: Int = 0,
    val executionsLast24Hours: Int = 0,
    val mostUsedWorkflows: List<WorkflowUsage> = emptyList()
)

/**
 * Usage d'un workflow
 */
data class WorkflowUsage(
    val workflowId: String,
    val workflowName: String,
    val executionCount: Int,
    val successRate: Float
)
