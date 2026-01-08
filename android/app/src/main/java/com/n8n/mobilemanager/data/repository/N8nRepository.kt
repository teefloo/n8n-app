package com.n8n.mobilemanager.data.repository

import android.util.Log
import com.n8n.mobilemanager.data.local.InstanceDao
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.*
import com.n8n.mobilemanager.data.remote.N8nApiService
import com.n8n.mobilemanager.di.ApiServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "N8nRepository"

/**
 * Repository principal pour accéder aux données n8n
 */
@Singleton
class N8nRepository @Inject constructor(
    private val instanceDao: InstanceDao,
    private val preferencesManager: PreferencesManager,
    private val apiServiceFactory: ApiServiceFactory
) {
    
    // ==================== Instances ====================
    
    fun getAllInstances(): Flow<List<N8nInstance>> = instanceDao.getAllInstances()
    
    fun getActiveInstanceFlow(): Flow<N8nInstance?> = instanceDao.getActiveInstanceFlow()
    
    suspend fun getActiveInstance(): N8nInstance? = instanceDao.getActiveInstance()
    
    suspend fun getInstanceById(id: Long): N8nInstance? = instanceDao.getInstanceById(id)
    
    suspend fun addInstance(name: String, baseUrl: String, apiKey: String): Result<N8nInstance> {
        return withContext(Dispatchers.IO) {
            try {
                // On récupère toutes les instances pour vérifier si c'est la première
                val allInstances = instanceDao.getAllInstances().first()
                val hasActive = allInstances.any { it.isActive }
                val isFirst = allInstances.isEmpty()
                
                val instance = N8nInstance(
                    name = name.ifBlank { "Instance ${allInstances.size + 1}" },
                    baseUrl = baseUrl.trimEnd('/'),
                    apiKey = apiKey,
                    isActive = isFirst || !hasActive // Active si c'est la première ou si aucune n'est active
                )
                
                val id = instanceDao.insertInstance(instance)
                
                // On synchronise aussi dans les préférences si c'est l'instance active
                if (isFirst || !hasActive) {
                    preferencesManager.setActiveInstanceId(id)
                }
                
                val saved = instanceDao.getInstanceById(id)
                Result.success(saved!!)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateInstance(instance: N8nInstance): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                instanceDao.updateInstance(instance.copy(baseUrl = instance.baseUrl.trimEnd('/')))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun deleteInstance(instance: N8nInstance): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                instanceDao.deleteInstance(instance)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun setActiveInstance(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                instanceDao.setAsActive(id)
                preferencesManager.setActiveInstanceId(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== Connection Test ====================
    
    suspend fun testConnection(instance: N8nInstance): Result<InstanceStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val apiService = apiServiceFactory.create(instance)
                val response = apiService.healthCheck()
                
                if (response.isSuccessful) {
                    instanceDao.updateLastConnected(instance.id, System.currentTimeMillis())
                    
                    // Get workflow count for status
                    val workflowsResponse = apiService.getWorkflows(limit = 1)
                    val activeWorkflowsResponse = apiService.getWorkflows(active = true, limit = 1)
                    
                    Result.success(
                        InstanceStatus(
                            isOnline = true,
                            totalWorkflows = 0, // Would need to parse from headers or make additional call
                            activeWorkflows = 0
                        )
                    )
                } else {
                    Result.failure(Exception("Connection failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== Workflows ====================
    
    suspend fun getWorkflows(activeOnly: Boolean? = null): Result<List<Workflow>> {
        return withApiService { apiService ->
            val response = apiService.getWorkflows(active = activeOnly)
            if (response.isSuccessful) {
                val workflows = response.body()?.data?.map { it.toWorkflow() } ?: emptyList()
                Result.success(workflows)
            } else {
                Result.failure(Exception("Failed to fetch workflows: ${response.code()}"))
            }
        }
    }
    
    suspend fun getWorkflow(id: String): Result<Workflow> {
        return withApiService { apiService ->
            val response = apiService.getWorkflow(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toWorkflow())
            } else {
                Result.failure(Exception("Failed to fetch workflow: ${response.code()}"))
            }
        }
    }
    
    suspend fun activateWorkflow(id: String): Result<Boolean> {
        return withApiService { apiService ->
            val response = apiService.activateWorkflow(id)
            if (response.isSuccessful) {
                Result.success(response.body()?.active ?: true)
            } else {
                Result.failure(Exception("Failed to activate workflow: ${response.code()}"))
            }
        }
    }
    
    suspend fun deactivateWorkflow(id: String): Result<Boolean> {
        return withApiService { apiService ->
            val response = apiService.deactivateWorkflow(id)
            if (response.isSuccessful) {
                Result.success(response.body()?.active ?: false)
            } else {
                Result.failure(Exception("Failed to deactivate workflow: ${response.code()}"))
            }
        }
    }
    
    // ==================== Executions ====================
    
    suspend fun getExecutions(
        workflowId: String? = null,
        status: ExecutionStatus? = null,
        limit: Int = 50
    ): Result<List<Execution>> {
        return withApiService { apiService ->
            val response = apiService.getExecutions(
                workflowId = workflowId,
                status = status?.name?.lowercase(),
                limit = limit
            )
            if (response.isSuccessful) {
                val executions = response.body()?.data?.map { it.toExecution() } ?: emptyList()
                Result.success(executions)
            } else {
                Result.failure(Exception("Failed to fetch executions: ${response.code()}"))
            }
        }
    }
    
    suspend fun getExecution(id: String): Result<Execution> {
        return withApiService { apiService ->
            val response = apiService.getExecution(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toExecution())
            } else {
                Result.failure(Exception("Failed to fetch execution: ${response.code()}"))
            }
        }
    }
    
    suspend fun retryExecution(id: String): Result<Execution> {
        return withApiService { apiService ->
            val response = apiService.retryExecution(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toExecution())
            } else {
                Result.failure(Exception("Failed to retry execution: ${response.code()}"))
            }
        }
    }
    
    suspend fun stopExecution(id: String): Result<Execution> {
        return withApiService { apiService ->
            val response = apiService.stopExecution(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toExecution())
            } else {
                Result.failure(Exception("Failed to stop execution: ${response.code()}"))
            }
        }
    }
    
    // ==================== Credentials ====================
    
    suspend fun getCredentials(): Result<List<Credential>> {
        return withApiService { apiService ->
            Log.d(TAG, "getCredentials: Fetching credentials from API...")
            val response = apiService.getCredentials()
            Log.d(TAG, "getCredentials: Response code=${response.code()}, isSuccessful=${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "getCredentials: Response body=$body")
                Log.d(TAG, "getCredentials: Response body data=${body?.data}")
                Log.d(TAG, "getCredentials: Response body data size=${body?.data?.size}")
                
                val credentials = body?.data?.map { dto ->
                    Log.d(TAG, "getCredentials: Mapping DTO - id=${dto.id}, name=${dto.name}, type=${dto.type}")
                    dto.toCredential()
                } ?: emptyList()
                
                Log.d(TAG, "getCredentials: Returning ${credentials.size} credentials")
                Result.success(credentials)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "getCredentials: Failed! Code=${response.code()}, Error=$errorBody")
                
                // Messages d'erreur plus explicites selon le code
                val errorMessage = when (response.code()) {
                    401 -> "Clé API invalide ou expirée"
                    403 -> "Accès aux credentials refusé. Vérifiez les permissions de votre clé API."
                    405 -> "L'accès aux credentials n'est pas autorisé sur cette instance n8n. " +
                           "Assurez-vous que votre clé API a les scopes nécessaires (credential:read) " +
                           "ou que votre plan n8n le permet."
                    404 -> "Endpoint credentials non trouvé. Vérifiez la version de n8n."
                    else -> "Erreur ${response.code()}: $errorBody"
                }
                
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    suspend fun getCredential(id: String): Result<Credential> {
        return withApiService { apiService ->
            val response = apiService.getCredential(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toCredential())
            } else {
                Result.failure(Exception("Failed to fetch credential: ${response.code()}"))
            }
        }
    }
    
    // ==================== Variables ====================
    
    suspend fun getVariables(): Result<List<Variable>> {
        return withApiService { apiService ->
            val response = apiService.getVariables()
            if (response.isSuccessful) {
                val variables = response.body()?.data?.map { it.toVariable() } ?: emptyList()
                Result.success(variables)
            } else {
                Result.failure(Exception("Failed to fetch variables: ${response.code()}"))
            }
        }
    }
    
    // ==================== Stats ====================
    
    suspend fun getInstanceStats(): Result<InstanceStats> {
        return withApiService { apiService ->
            try {
                val workflowsResponse = apiService.getWorkflows()
                val activeWorkflowsResponse = apiService.getWorkflows(active = true)
                val executionsResponse = apiService.getExecutions(limit = 100)
                
                val workflows = workflowsResponse.body()?.data ?: emptyList()
                val activeWorkflows = activeWorkflowsResponse.body()?.data ?: emptyList()
                val executions = executionsResponse.body()?.data?.map { it.toExecution() } ?: emptyList()
                
                val successCount = executions.count { it.status == ExecutionStatus.SUCCESS }
                val failedCount = executions.count { it.status == ExecutionStatus.ERROR }
                
                Result.success(
                    InstanceStats(
                        totalWorkflows = workflows.size,
                        activeWorkflows = activeWorkflows.size,
                        totalExecutions = executions.size,
                        successfulExecutions = successCount,
                        failedExecutions = failedCount
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ==================== Helper ====================
    
    private suspend fun <T> withApiService(block: suspend (N8nApiService) -> Result<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            val instance = getActiveInstance()
            Log.d(TAG, "withApiService: Active instance = $instance")
            
            if (instance == null) {
                Log.e(TAG, "withApiService: No active instance configured!")
                return@withContext Result.failure(Exception("Aucune instance active configurée"))
            }
            
            Log.d(TAG, "withApiService: Using instance=${instance.name}, baseUrl=${instance.baseUrl}")
            
            try {
                val apiService = apiServiceFactory.create(instance)
                block(apiService)
            } catch (e: Exception) {
                Log.e(TAG, "withApiService: Exception occurred", e)
                Result.failure(e)
            }
        }
    }
}
