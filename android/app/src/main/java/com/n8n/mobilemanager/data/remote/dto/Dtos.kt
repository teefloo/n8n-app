package com.n8n.mobilemanager.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.n8n.mobilemanager.data.model.*

/**
 * Réponse générique de l'API n8n avec pagination
 * Note: Le champ data est nullable car certaines versions de l'API peuvent retourner
 * une structure différente
 */
data class ApiResponse<T>(
    val data: List<T>? = null,
    val nextCursor: String? = null
)

/**
 * DTO pour un workflow depuis l'API
 */
data class WorkflowDto(
    val id: String,
    val name: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<TagDto>? = null,
    val nodes: List<NodeDto>? = null,
    val connections: Map<String, Any>? = null,
    val settings: WorkflowSettingsDto? = null
) {
    fun toWorkflow(): Workflow = Workflow(
        id = id,
        name = name,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tags = tags?.map { it.toTag() } ?: emptyList(),
        nodes = nodes?.map { it.toNode() } ?: emptyList(),
        connections = connections,
        settings = settings?.toWorkflowSettings()
    )
}

data class TagDto(
    val id: String,
    val name: String
) {
    fun toTag(): Tag = Tag(id = id, name = name)
}

data class NodeDto(
    val id: String,
    val name: String,
    val type: String,
    val position: List<Double>? = null,
    val parameters: Map<String, Any>? = null,
    val typeVersion: Double? = 1.0,
    val disabled: Boolean? = false
) {
    fun toNode(): Node = Node(
        id = id,
        name = name,
        type = type,
        position = position ?: emptyList(),
        parameters = parameters,
        typeVersion = typeVersion ?: 1.0,
        disabled = disabled ?: false
    )
}

data class WorkflowSettingsDto(
    val saveExecutionProgress: Boolean? = null,
    val saveManualExecutions: Boolean? = null,
    val saveDataErrorExecution: String? = null,
    val saveDataSuccessExecution: String? = null,
    val executionTimeout: Int? = null,
    val timezone: String? = null
) {
    fun toWorkflowSettings(): WorkflowSettings = WorkflowSettings(
        saveExecutionProgress = saveExecutionProgress,
        saveManualExecutions = saveManualExecutions,
        saveDataErrorExecution = saveDataErrorExecution,
        saveDataSuccessExecution = saveDataSuccessExecution,
        executionTimeout = executionTimeout,
        timezone = timezone
    )
}

/**
 * DTO pour une exécution depuis l'API
 */
data class ExecutionDto(
    val id: String,
    @SerializedName("workflowId")
    val workflowId: String,
    @SerializedName("workflowName")
    val workflowName: String? = null,
    val finished: Boolean,
    val mode: String,
    val status: String? = null,
    val startedAt: String,
    val stoppedAt: String? = null,
    val data: ExecutionDataDto? = null,
    val retryOf: String? = null,
    val retrySuccessId: String? = null
) {
    fun toExecution(): Execution = Execution(
        id = id,
        workflowId = workflowId,
        workflowName = workflowName,
        finished = finished,
        mode = ExecutionMode.fromString(mode),
        status = status?.let { ExecutionStatus.fromString(it) } 
            ?: if (finished) ExecutionStatus.SUCCESS else ExecutionStatus.RUNNING,
        startedAt = startedAt,
        stoppedAt = stoppedAt,
        retryOf = retryOf,
        retrySuccessId = retrySuccessId
    )
}

data class ExecutionDataDto(
    val resultData: ResultDataDto? = null
)

data class ResultDataDto(
    val runData: Map<String, Any>? = null,
    val lastNodeExecuted: String? = null,
    val error: ExecutionErrorDto? = null
)

data class ExecutionErrorDto(
    val message: String,
    val name: String? = null,
    val node: String? = null,
    val stack: String? = null,
    val timestamp: Long? = null
) {
    fun toExecutionError(): ExecutionError = ExecutionError(
        message = message,
        name = name,
        node = node,
        stack = stack,
        timestamp = timestamp
    )
}

/**
 * DTO pour un credential depuis l'API
 * Note: Certains champs sont rendus nullables ou ont des valeurs par défaut
 * pour gérer les différentes versions de l'API n8n
 */
data class CredentialDto(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val nodesAccess: List<NodeAccessDto>? = null,
    val sharedWith: List<Any>? = null,  // Peut être List<String> ou List<Object>
    @SerializedName("ownedBy")
    val ownedBy: Any? = null,  // Structure variable selon la version de n8n
    @SerializedName("sharedWithProjects")
    val sharedWithProjects: List<Any>? = null
) {
    fun toCredential(): Credential = Credential(
        id = id ?: "",
        name = name ?: "Sans nom",
        type = type ?: "unknown",
        createdAt = createdAt ?: "",
        updatedAt = updatedAt ?: createdAt ?: "",
        nodesAccess = nodesAccess?.map { it.toNodeAccess() } ?: emptyList(),
        sharedWith = sharedWith?.mapNotNull { it as? String } ?: emptyList()
    )
}

data class NodeAccessDto(
    val nodeType: String? = null,
    val date: String? = null
) {
    fun toNodeAccess(): NodeAccess = NodeAccess(
        nodeType = nodeType ?: "",
        date = date
    )
}

/**
 * DTO pour une variable
 */
data class VariableDto(
    val id: String,
    val key: String,
    val value: String,
    val type: String = "string"
) {
    fun toVariable(): Variable = Variable(
        id = id,
        key = key,
        value = value,
        type = type
    )
}

/**
 * Réponse du health check
 */
data class HealthCheckResponse(
    val status: String
)

/**
 * Réponse de l'activation/désactivation d'un workflow
 */
data class WorkflowActivationResponse(
    val id: String,
    val active: Boolean
)

/**
 * Corps de requête pour déclencher un workflow
 */
data class TriggerWorkflowRequest(
    val workflowId: String? = null
)

/**
 * Réponse du déclenchement d'un workflow
 */
data class TriggerWorkflowResponse(
    val data: Any? = null,
    val executionId: String? = null
)
