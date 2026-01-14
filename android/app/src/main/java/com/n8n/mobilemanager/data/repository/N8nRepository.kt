package com.n8n.mobilemanager.data.repository

import android.util.Log
import com.n8n.mobilemanager.data.local.InstanceDao
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.*
import com.n8n.mobilemanager.data.remote.N8nApiService
import com.n8n.mobilemanager.di.ApiServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "N8nRepository"

data class PaginatedExecutions(
    val executions: List<Execution>,
    val nextCursor: String?
)

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
    
    /**
     * Récupère une page d'exécutions
     */
    suspend fun getExecutionsPage(
        workflowId: String? = null,
        status: ExecutionStatus? = null,
        limit: Int = 50,
        cursor: String? = null
    ): Result<PaginatedExecutions> {
        return withApiService { apiService ->
            val response = apiService.getExecutions(
                workflowId = workflowId,
                status = status?.name?.lowercase(),
                limit = limit,
                cursor = cursor
            )
            
            if (response.isSuccessful) {
                val body = response.body()
                val executions = body?.data?.map { it.toExecution() } ?: emptyList()
                Result.success(PaginatedExecutions(executions, body?.nextCursor))
            } else {
                Result.failure(Exception("Failed to fetch executions page: ${response.code()}"))
            }
        }
    }

    /**
     * Récupère les exécutions avec pagination pour obtenir toutes les données.
     * Si fetchAll est true, récupère toutes les exécutions via pagination.
     * Sinon, retourne seulement le nombre spécifié par limit.
     */
    suspend fun getExecutions(
        workflowId: String? = null,
        status: ExecutionStatus? = null,
        limit: Int = 50,
        fetchAll: Boolean = true,
        includeWorkflowNames: Boolean = false,
        startDate: Long? = null
    ): Result<List<Execution>> {
        return withApiService { apiService ->
            if (fetchAll) {
                // Récupérer TOUTES les exécutions via pagination
                val allExecutions = mutableListOf<Execution>()
                var cursor: String? = null
                var pageCount = 0
                val maxPages = 20 // Limite augmentée (env. 5000 items max)
                
                Log.d(TAG, "getExecutions: Fetching all executions with pagination...")
                
                do {
                    val response = apiService.getExecutions(
                        workflowId = workflowId,
                        status = status?.name?.lowercase(),
                        limit = 250,
                        cursor = cursor
                    )
                    
                    if (!response.isSuccessful) {
                        Log.e(TAG, "getExecutions: Failed at page $pageCount, code=${response.code()}")
                        return@withApiService Result.failure(Exception("Failed to fetch executions: ${response.code()}"))
                    }
                    
                    val body = response.body()
                    val executions = body?.data?.map { it.toExecution() } ?: emptyList()
                    allExecutions.addAll(executions)
                    cursor = body?.nextCursor
                    pageCount++
                    
                    Log.d(TAG, "getExecutions: Page $pageCount - ${executions.size} items, total: ${allExecutions.size}, nextCursor: ${cursor != null}")
                    
                } while (cursor != null && pageCount < maxPages)
                
                Log.d(TAG, "getExecutions: Complete! Total ${allExecutions.size} executions fetched")
                
                // Filtrer par date si nécessaire
                val filteredExecutions = if (startDate != null) {
                    allExecutions.filter { exec ->
                        val time = parseDateToMillis(exec.startedAt)
                        time != null && time >= startDate
                    }
                } else {
                    allExecutions
                }
                
                val finalExecutions = if (includeWorkflowNames) {
                    enrichExecutionsWithWorkflowNames(filteredExecutions, apiService)
                } else {
                    filteredExecutions
                }
                
                Result.success(finalExecutions)
            } else {
                // Mode simple sans pagination (mais potentiellement avec filtrage par date)
                // Si filtrage par date, on doit peut-être en récupérer plus pour en avoir assez après filtrage
                val effectiveLimit = if (startDate != null) limit * 5 else limit
                
                val response = apiService.getExecutions(
                    workflowId = workflowId,
                    status = status?.name?.lowercase(),
                    limit = effectiveLimit
                )
                if (response.isSuccessful) {
                    val executions = response.body()?.data?.map { it.toExecution() } ?: emptyList()
                    
                    // Filtrer par date si nécessaire
                    val filteredExecutions = if (startDate != null) {
                        executions.filter { exec ->
                            val time = parseDateToMillis(exec.startedAt)
                            time != null && time >= startDate
                        }
                    } else {
                        executions
                    }
                    
                    // Réappliquer la limite après filtrage
                    val limitedExecutions = filteredExecutions.take(limit)
                    
                    val finalExecutions = if (includeWorkflowNames) {
                        enrichExecutionsWithWorkflowNames(limitedExecutions, apiService)
                    } else {
                        limitedExecutions
                    }
                    
                    Result.success(finalExecutions)
                } else {
                    Result.failure(Exception("Failed to fetch executions: ${response.code()}"))
                }
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
            
            // 1. Essayer l'API REST (interne) qui retourne souvent plus de résultats (non utilisés)
            try {
                Log.d(TAG, "getCredentials: Trying REST endpoint...")
                val restResponse = apiService.getCredentialsRest()
                if (restResponse.isSuccessful) {
                    val body = restResponse.body()
                    if (body != null && body.data != null) {
                        val credentials = body.data.map { dto -> dto.toCredential() }
                        Log.d(TAG, "getCredentials: REST endpoint success, found ${credentials.size} items")
                        return@withApiService Result.success(credentials)
                    }
                }
                Log.w(TAG, "getCredentials: REST endpoint failed or empty (${restResponse.code()}), trying internal root endpoint...")
            } catch (e: Exception) {
                Log.w(TAG, "getCredentials: REST endpoint exception, trying internal root endpoint...", e)
            }

            // 2. Essayer l'endpoint interne racine (/credentials) - fallback
            try {
                val internalResponse = apiService.getCredentialsInternal()
                if (internalResponse.isSuccessful) {
                    val body = internalResponse.body()
                    if (body != null && body.data != null) {
                        val credentials = body.data.map { dto -> dto.toCredential() }
                        Log.d(TAG, "getCredentials: Internal root endpoint success, found ${credentials.size} items")
                        return@withApiService Result.success(credentials)
                    }
                }
                Log.w(TAG, "getCredentials: Internal root endpoint failed (${internalResponse.code()}), falling back to Public API...")
            } catch (e: Exception) {
                Log.w(TAG, "getCredentials: Internal root endpoint exception, falling back to Public API", e)
            }

            // 3. Fallback API Publique avec pagination
            Log.d(TAG, "getCredentials: Falling back to Public API with pagination...")
            val allCredentials = mutableListOf<Credential>()
            var cursor: String? = null
            var pageCount = 0
            val maxPages = 50 // Sécurité : max 50 pages * 100 items = 5000 credentials
            var lastResponse: retrofit2.Response<com.n8n.mobilemanager.data.remote.dto.ApiResponse<com.n8n.mobilemanager.data.remote.dto.CredentialDto>>? = null

            try {
                do {
                    val response = apiService.getCredentials(limit = 100, cursor = cursor)
                    lastResponse = response
                    
                    if (response.isSuccessful) {
                        val body = response.body()
                        val credentials = body?.data?.map { dto -> dto.toCredential() } ?: emptyList()
                        allCredentials.addAll(credentials)
                        cursor = body?.nextCursor
                        pageCount++
                        Log.d(TAG, "getCredentials: Page $pageCount fetched, ${credentials.size} items, total: ${allCredentials.size}, nextCursor: ${cursor != null}")
                    } else {
                        // Si une page échoue, on arrête la pagination
                        Log.e(TAG, "getCredentials: Failed at page $pageCount, code=${response.code()}")
                        if (allCredentials.isEmpty()) {
                            // Si c'est la première page qui échoue, on traitera l'erreur plus bas
                            break
                        } else {
                            // Si on a déjà des données, on retourne ce qu'on a (best effort)
                            Log.w(TAG, "getCredentials: Partial success, returning ${allCredentials.size} credentials")
                            return@withApiService Result.success(allCredentials)
                        }
                    }
                } while (cursor != null && pageCount < maxPages)
                
                if (allCredentials.isNotEmpty()) {
                    Log.d(TAG, "getCredentials: Complete! Total ${allCredentials.size} credentials")
                    Result.success(allCredentials)
                } else {
                    // Si on arrive ici et qu'on a pas de credentials, c'est que la première page a échoué (ou liste vide)
                    if (lastResponse?.isSuccessful == true) {
                         Result.success(emptyList())
                    } else {
                        // Erreur lors de la première requête
                        val response = lastResponse!!
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "getCredentials: Failed! Code=${response.code()}, Error=$errorBody")
                        
                        // Messages d'erreur plus explicites
                        val errorMessage = when (response.code()) {
                            401 -> "Invalid or expired API key"
                            403 -> "Access to credentials denied. Check your API key permissions."
                            405 -> "Access to credentials is not allowed on this n8n instance. " +
                                   "Make sure your API key has the necessary scopes (credential:read) " +
                                   "or that your n8n plan allows it."
                            404 -> "Credentials endpoint not found. Check your n8n version."
                            else -> "Error ${response.code()}: $errorBody"
                        }
                        
                        Result.failure(Exception(errorMessage))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "getCredentials: Error during fetch", e)
                
                Result.failure(e)
            }
        }
    }
    


    suspend fun getCredential(id: String): Result<Credential> {
        return withApiService { apiService ->
            // 1. Essayer l'API REST (interne)
            try {
                val restResponse = apiService.getCredentialRest(id)
                if (restResponse.isSuccessful && restResponse.body() != null) {
                    return@withApiService Result.success(restResponse.body()!!.toCredential())
                }
            } catch (e: Exception) {
                Log.w(TAG, "getCredential: REST endpoint failed", e)
            }

            // 2. Essayer l'endpoint interne racine
            try {
                val internalResponse = apiService.getCredentialInternal(id)
                if (internalResponse.isSuccessful && internalResponse.body() != null) {
                    return@withApiService Result.success(internalResponse.body()!!.toCredential())
                }
            } catch (e: Exception) {
                Log.w(TAG, "getCredential: Internal endpoint failed", e)
            }

            // 3. Fallback API Publique
            val response = apiService.getCredential(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toCredential())
            } else {
                Result.failure(Exception("Failed to fetch credential details: ${response.code()}"))
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

    // ==================== Fallback Auth ====================

    suspend fun getCredentialsWithLogin(email: String, password: String): Result<List<Credential>> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "getCredentialsWithLogin: Starting login...")
            val instance = getActiveInstance() ?: return@withContext Result.failure(Exception("No active instance"))
            
            try {
                // 1. Authentification pour récupérer le cookie
                val loginService = apiServiceFactory.createWithCookie(instance.baseUrl, null)
                val request = com.n8n.mobilemanager.data.remote.dto.LoginRequest(emailOrLdapLoginId = email.trim(), password = password)
                
                var loginResponse = loginService.login(request)
                Log.d(TAG, "getCredentialsWithLogin: Login response=${loginResponse.code()}")
                
                if (!loginResponse.isSuccessful) {
                    // Essayer l'autre endpoint
                    loginResponse = loginService.loginRest(request)
                    Log.d(TAG, "getCredentialsWithLogin: LoginRest response=${loginResponse.code()}")
                }
                
                if (!loginResponse.isSuccessful) {
                    val errorBody = loginResponse.errorBody()?.string()
                    Log.e(TAG, "getCredentialsWithLogin: Login failed: $errorBody")
                    return@withContext Result.failure(Exception("Login failed: ${loginResponse.code()} - Check your credentials"))
                }
                
                // 2. Extraction et nettoyage du cookie
                val headers = loginResponse.headers()
                val cookies = headers.values("Set-Cookie")
                val rawCookie = cookies.find { it.contains("n8n-auth") }
                
                if (rawCookie == null) {
                    Log.e(TAG, "getCredentialsWithLogin: Cookie not found. Cookies=${cookies}")
                    return@withContext Result.failure(Exception("Authentication cookie not found"))
                }
                
                // Garder uniquement la partie clé=valeur (supprimer Path, HttpOnly, etc.)
                val authCookie = rawCookie.split(";").firstOrNull { it.trim().startsWith("n8n-auth=") } ?: rawCookie
                
                Log.d(TAG, "getCredentialsWithLogin: Cookie found and cleaned")
                
                // 3. Récupération des credentials avec le cookie
                val authenticatedService = apiServiceFactory.createWithCookie(instance.baseUrl, authCookie)
                
                Log.d(TAG, "getCredentialsWithLogin: Fetching credentials with cookie...")
                
                // 1. Essayer l'endpoint REST interne (standard pour le dashboard)
                // GET /rest/credentials
                var response = authenticatedService.getCredentialsRest()
                
                // 2. Si échec, essayer l'endpoint racine
                // GET /credentials
                if (!response.isSuccessful) {
                     Log.w(TAG, "getCredentialsWithLogin: REST endpoint failed (${response.code()}), trying root...")
                     response = authenticatedService.getCredentialsInternal()
                }
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val credentials = body?.data?.map { dto -> dto.toCredential() } ?: emptyList()
                    Result.success(credentials)
                } else {
                    // 3. Dernier recours : API Publique (si elle accepte le cookie)
                    // GET /api/v1/credentials
                    Log.w(TAG, "getCredentialsWithLogin: Internal endpoints failed, trying Public API...")
                    val allCredentials = mutableListOf<Credential>()
                    var cursor: String? = null
                    var pageCount = 0
                    val maxPages = 50
                    
                    do {
                        val publicResponse = authenticatedService.getCredentials(limit = 100, cursor = cursor)
                        
                        if (publicResponse.isSuccessful) {
                            val body = publicResponse.body()
                            val credentials = body?.data?.map { dto -> dto.toCredential() } ?: emptyList()
                            allCredentials.addAll(credentials)
                            cursor = body?.nextCursor
                            pageCount++
                        } else {
                             Log.e(TAG, "getCredentialsWithLogin: Public API failed at page $pageCount, code=${publicResponse.code()}")
                             if (allCredentials.isEmpty()) {
                                 // Tout a échoué
                                 return@withContext Result.failure(Exception("Error fetching credentials: ${response.code()}"))
                             }
                             break
                        }
                    } while (cursor != null && pageCount < maxPages)
                    
                    Result.success(allCredentials)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "getCredentialsWithLogin: Error", e)
                Result.failure(e)
            }
        }
    }
    
    // ==================== Stats ====================
    
    suspend fun getInstanceStats(startDate: Long? = null): Result<InstanceStats> {
        return withApiService { apiService ->
            try {
                Log.d(TAG, "getInstanceStats: Fetching all stats (startDate=$startDate)...")
                
                val workflowsResponse = apiService.getWorkflows()
                val activeWorkflowsResponse = apiService.getWorkflows(active = true)
                
                val workflows = workflowsResponse.body()?.data ?: emptyList()
                val activeWorkflows = activeWorkflowsResponse.body()?.data ?: emptyList()
                
                // Récupérer toutes les exécutions pour les stats (pagination)
                val allExecutions = mutableListOf<Execution>()
                var statsCursor: String? = null
                var statsPageCount = 0
                val maxStatsPages = 40 // 40 * 250 = 10000 exécutions max pour les stats (augmenté pour plus de précision)
                
                do {
                    try {
                        val executionsResponse = apiService.getExecutions(limit = 250, cursor = statsCursor)
                        if (!executionsResponse.isSuccessful) {
                            Log.w(TAG, "getInstanceStats: Failed to fetch page $statsPageCount, stopping pagination")
                            break
                        }
                        val body = executionsResponse.body()
                        val executions = body?.data?.map { it.toExecution() } ?: emptyList()
                        allExecutions.addAll(executions)
                        statsCursor = body?.nextCursor
                        statsPageCount++
                        Log.d(TAG, "getInstanceStats: Page $statsPageCount fetched, ${executions.size} items, total: ${allExecutions.size}")
                    } catch (e: Exception) {
                        Log.w(TAG, "getInstanceStats: Error fetching page $statsPageCount, using data collected so far", e)
                        break
                    }
                } while (statsCursor != null && statsPageCount < maxStatsPages)
                
                Log.d(TAG, "getInstanceStats: Fetched ${allExecutions.size} executions for stats")
                
                // Filtrer par date si nécessaire
                val statsExecutions = if (startDate != null) {
                    allExecutions.filter { exec ->
                        val time = parseDateToMillis(exec.startedAt)
                        time != null && time >= startDate
                    }
                } else {
                    allExecutions
                }
                
                Log.d(TAG, "getInstanceStats: Calculating stats on ${statsExecutions.size} executions (filtered from ${allExecutions.size})")
                
                val successCount = statsExecutions.count { it.status == ExecutionStatus.SUCCESS }
                val failedCount = statsExecutions.count { it.status == ExecutionStatus.ERROR || it.status == ExecutionStatus.CRASHED }
                
                // Calculer le temps moyen d'exécution
                val executionsWithDuration = statsExecutions.filter { it.stoppedAt != null }
                val avgExecutionTime = if (executionsWithDuration.isNotEmpty()) {
                    calculateAverageExecutionTime(executionsWithDuration)
                } else 0L
                
                Log.d(TAG, "getInstanceStats: Success=$successCount, Failed=$failedCount, AvgTime=${avgExecutionTime}ms")
                
                Result.success(
                    InstanceStats(
                        totalWorkflows = workflows.size,
                        activeWorkflows = activeWorkflows.size,
                        totalExecutions = statsExecutions.size,
                        isTotalExecutionsEstimated = statsCursor != null,
                        successfulExecutions = successCount,
                        failedExecutions = failedCount,
                        averageExecutionTime = avgExecutionTime
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "getInstanceStats: Error", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Version optimisée de getInstanceStats qui utilise le chargement parallèle
     * et limite le nombre de pages selon la période pour de meilleures performances.
     * 
     * @param startDate Date de début pour filtrer les exécutions (timestamp en ms)
     * @param maxPages Nombre maximum de pages à charger (défini dans StatsPeriod)
     */
    suspend fun getInstanceStatsOptimized(startDate: Long? = null, maxPages: Int = 10): Result<InstanceStats> {
        return withApiService { apiService ->
            try {
                Log.d(TAG, "getInstanceStatsOptimized: Starting parallel load (startDate=$startDate, maxPages=$maxPages)")
                val startTime = System.currentTimeMillis()
                
                // Charger les workflows et exécutions en parallèle
                coroutineScope {
                    val workflowsDeferred = async {
                        apiService.getWorkflows()
                    }
                    
                    val activeWorkflowsDeferred = async {
                        apiService.getWorkflows(active = true)
                    }
                    
                    val executionsDeferred = async {
                        fetchExecutionsWithLimit(apiService, maxPages, startDate)
                    }
                    
                    // Attendre tous les résultats
                    val workflowsResponse = workflowsDeferred.await()
                    val activeWorkflowsResponse = activeWorkflowsDeferred.await()
                    val (allExecutions, reachedEnd) = executionsDeferred.await()
                    
                    val workflows = workflowsResponse.body()?.data ?: emptyList()
                    val activeWorkflows = activeWorkflowsResponse.body()?.data ?: emptyList()
                    
                    // Filtrer par date si nécessaire (les données de l'API sont déjà triées)
                    val statsExecutions = if (startDate != null) {
                        allExecutions.filter { exec ->
                            val time = parseDateToMillis(exec.startedAt)
                            time != null && time >= startDate
                        }
                    } else {
                        allExecutions
                    }
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d(TAG, "getInstanceStatsOptimized: Loaded ${statsExecutions.size} filtered executions in ${elapsed}ms")
                    
                    val successCount = statsExecutions.count { it.status == ExecutionStatus.SUCCESS }
                    val failedCount = statsExecutions.count { it.status == ExecutionStatus.ERROR || it.status == ExecutionStatus.CRASHED }
                    
                    // Calculer le temps moyen d'exécution
                    val executionsWithDuration = statsExecutions.filter { it.stoppedAt != null }
                    val avgExecutionTime = if (executionsWithDuration.isNotEmpty()) {
                        calculateAverageExecutionTime(executionsWithDuration)
                    } else 0L
                    
                    Log.d(TAG, "getInstanceStatsOptimized: Success=$successCount, Failed=$failedCount, AvgTime=${avgExecutionTime}ms, Total time=${elapsed}ms")
                    
                    Result.success(
                        InstanceStats(
                            totalWorkflows = workflows.size,
                            activeWorkflows = activeWorkflows.size,
                            totalExecutions = statsExecutions.size,
                            isTotalExecutionsEstimated = !reachedEnd, // True si on n'a pas tout chargé
                            successfulExecutions = successCount,
                            failedExecutions = failedCount,
                            averageExecutionTime = avgExecutionTime
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "getInstanceStatsOptimized: Error", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Récupère les exécutions avec une limite de pages et une optimisation de date.
     * Retourne la liste des exécutions et un booléen indiquant si on a atteint la fin.
     */
    private suspend fun fetchExecutionsWithLimit(
        apiService: N8nApiService,
        maxPages: Int,
        startDate: Long?
    ): Pair<List<Execution>, Boolean> {
        val allExecutions = mutableListOf<Execution>()
        var cursor: String? = null
        var pageCount = 0
        var reachedEnd = false
        var foundOlderData = false
        
        Log.d(TAG, "fetchExecutionsWithLimit: Starting with maxPages=$maxPages, startDate=$startDate")
        
        do {
            try {
                val response = apiService.getExecutions(limit = 250, cursor = cursor)
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchExecutionsWithLimit: Failed at page $pageCount, code=${response.code()}")
                    break
                }
                
                val body = response.body()
                val executions = body?.data?.map { it.toExecution() } ?: emptyList()
                allExecutions.addAll(executions)
                cursor = body?.nextCursor
                pageCount++
                
                // Vérifier si on a dépassé la date de début (optimisation Early Exit)
                if (startDate != null && executions.isNotEmpty()) {
                    val oldestInPage = executions.lastOrNull()?.let { exec ->
                        parseDateToMillis(exec.startedAt)
                    }
                    if (oldestInPage != null && oldestInPage < startDate) {
                        Log.d(TAG, "fetchExecutionsWithLimit: Found data older than startDate, stopping early at page $pageCount")
                        foundOlderData = true
                        break
                    }
                }
                
                Log.d(TAG, "fetchExecutionsWithLimit: Page $pageCount fetched, ${executions.size} items, total: ${allExecutions.size}")
                
            } catch (e: Exception) {
                Log.w(TAG, "fetchExecutionsWithLimit: Error at page $pageCount", e)
                break
            }
        } while (cursor != null && pageCount < maxPages)
        
        // On a atteint la fin si:
        // - Plus de curseur (fin des données)
        // - Ou on a trouvé des données plus anciennes que startDate
        reachedEnd = cursor == null || foundOlderData
        
        Log.d(TAG, "fetchExecutionsWithLimit: Complete! ${allExecutions.size} executions, reachedEnd=$reachedEnd")
        return Pair(allExecutions, reachedEnd)
    }
    
    private fun parseDateToMillis(dateString: String): Long? {
        if (dateString.isBlank()) return null
        
        // Try standard ISO-8601 formats
        val dateFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        
        for (pattern in dateFormats) {
            try {
                val format = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
                if (pattern.endsWith("'Z'")) {
                    format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                return format.parse(dateString)?.time
            } catch (e: Exception) {
                // Continue
            }
        }
        return null
    }

    private fun calculateAverageExecutionTime(executions: List<Execution>): Long {
        var totalDuration = 0L
        var count = 0
        
        for (exec in executions) {
            if (exec.startedAt.isBlank() || exec.stoppedAt.isNullOrBlank()) continue
            
            val startTime = parseDateToMillis(exec.startedAt)
            val stopTime = parseDateToMillis(exec.stoppedAt)
            
            if (startTime != null && stopTime != null) {
                val duration = stopTime - startTime
                if (duration > 0) {
                    totalDuration += duration
                    count++
                }
            }
        }
        
        return if (count > 0) totalDuration / count else 0L
    }
    
    private suspend fun enrichExecutionsWithWorkflowNames(
        executions: List<Execution>,
        apiService: N8nApiService
    ): List<Execution> {
        if (executions.isEmpty()) return executions

        // Identify executions with missing workflow names
        val missingNameExecutions = executions.filter { it.workflowName.isNullOrBlank() }
        if (missingNameExecutions.isEmpty()) return executions

        val uniqueWorkflowIds = missingNameExecutions.map { it.workflowId }.distinct()
        
        // If we have few workflows to look up, fetch them individually (faster/lighter than fetching all)
        if (uniqueWorkflowIds.size <= 20) {
            return try {
                Log.d(TAG, "enrichExecutionsWithWorkflowNames: Fetching ${uniqueWorkflowIds.size} workflows individually")
                
                val workflowNames = coroutineScope {
                    uniqueWorkflowIds.map { id ->
                        async {
                            try {
                                val response = apiService.getWorkflow(id)
                                if (response.isSuccessful) {
                                    id to response.body()?.name
                                } else {
                                    id to null
                                }
                            } catch (e: Exception) {
                                id to null
                            }
                        }
                    }.awaitAll().toMap()
                }

                executions.map { execution ->
                    if (execution.workflowName.isNullOrBlank() && workflowNames.containsKey(execution.workflowId)) {
                        execution.copy(workflowName = workflowNames[execution.workflowId])
                    } else {
                        execution
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "enrichExecutionsWithWorkflowNames: Error fetching individual workflows", e)
                executions
            }
        } else {
            // If many workflows, fetch all (paginated)
            return try {
                Log.d(TAG, "enrichExecutionsWithWorkflowNames: Fetching all workflows to match names")
                
                val allWorkflows = mutableListOf<Workflow>()
                var cursor: String? = null
                var pageCount = 0
                val maxPages = 10 // Max 10 pages * 250 = 2500 workflows
                
                do {
                    val response = apiService.getWorkflows(limit = 250, cursor = cursor)
                    if (response.isSuccessful) {
                        val body = response.body()
                        val workflows = body?.data?.map { it.toWorkflow() } ?: emptyList()
                        allWorkflows.addAll(workflows)
                        cursor = body?.nextCursor
                        pageCount++
                    } else {
                        break
                    }
                } while (cursor != null && pageCount < maxPages)
                
                val workflowMap = allWorkflows.associate { it.id to it.name }
                
                executions.map { execution ->
                    if (execution.workflowName.isNullOrBlank() && workflowMap.containsKey(execution.workflowId)) {
                        execution.copy(workflowName = workflowMap[execution.workflowId])
                    } else {
                        execution
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "enrichExecutionsWithWorkflowNames: Exception while fetching workflows", e)
                executions
            }
        }
    }
    
    private suspend fun <T> withApiService(block: suspend (N8nApiService) -> Result<T>): Result<T> {
        return withContext(Dispatchers.IO) {
            val instance = getActiveInstance()
            Log.d(TAG, "withApiService: Active instance = $instance")
            
            if (instance == null) {
                Log.e(TAG, "withApiService: No active instance configured!")
                return@withContext Result.failure(Exception("No active instance configured"))
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
