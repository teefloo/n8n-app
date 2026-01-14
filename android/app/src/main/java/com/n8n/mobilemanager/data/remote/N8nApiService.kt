package com.n8n.mobilemanager.data.remote

import com.n8n.mobilemanager.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface Retrofit pour l'API REST n8n
 * Documentation: https://docs.n8n.io/api/
 */
interface N8nApiService {

    // ==================== Health Check ====================
    
    @GET("healthz")
    suspend fun healthCheck(): Response<HealthCheckResponse>

    // ==================== Workflows ====================
    
    @GET("api/v1/workflows")
    suspend fun getWorkflows(
        @Query("active") active: Boolean? = null,
        @Query("tags") tags: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null
    ): Response<ApiResponse<WorkflowDto>>

    @GET("api/v1/workflows/{id}")
    suspend fun getWorkflow(
        @Path("id") id: String
    ): Response<WorkflowDto>

    @PATCH("api/v1/workflows/{id}")
    suspend fun updateWorkflow(
        @Path("id") id: String,
        @Body workflow: WorkflowDto
    ): Response<WorkflowDto>

    @POST("api/v1/workflows/{id}/activate")
    suspend fun activateWorkflow(
        @Path("id") id: String
    ): Response<WorkflowActivationResponse>

    @POST("api/v1/workflows/{id}/deactivate")
    suspend fun deactivateWorkflow(
        @Path("id") id: String
    ): Response<WorkflowActivationResponse>

    @DELETE("api/v1/workflows/{id}")
    suspend fun deleteWorkflow(
        @Path("id") id: String
    ): Response<Unit>

    // ==================== Executions ====================
    
    @GET("api/v1/executions")
    suspend fun getExecutions(
        @Query("workflowId") workflowId: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null,
        @Query("includeData") includeData: Boolean = false
    ): Response<ApiResponse<ExecutionDto>>

    @GET("api/v1/executions/{id}")
    suspend fun getExecution(
        @Path("id") id: String,
        @Query("includeData") includeData: Boolean = true
    ): Response<ExecutionDto>

    @DELETE("api/v1/executions/{id}")
    suspend fun deleteExecution(
        @Path("id") id: String
    ): Response<Unit>

    @POST("api/v1/executions/{id}/retry")
    suspend fun retryExecution(
        @Path("id") id: String
    ): Response<ExecutionDto>

    @POST("api/v1/executions/{id}/stop")
    suspend fun stopExecution(
        @Path("id") id: String
    ): Response<ExecutionDto>

    // ==================== Credentials ====================
    
    @GET("api/v1/credentials")
    suspend fun getCredentials(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null
    ): Response<ApiResponse<CredentialDto>>

    @GET("rest/credentials")
    suspend fun getCredentialsRest(
        @Query("filter") filter: String? = null
    ): Response<ApiResponse<CredentialDto>>

    @GET("credentials")
    suspend fun getCredentialsInternal(): Response<ApiResponse<CredentialDto>>

    @GET("api/v1/credentials/{id}")
    suspend fun getCredential(
        @Path("id") id: String,
        @Query("includeData") includeData: Boolean = false
    ): Response<CredentialDto>

    @GET("rest/credentials/{id}")
    suspend fun getCredentialRest(
        @Path("id") id: String,
        @Query("includeData") includeData: Boolean = false
    ): Response<CredentialDto>

    @GET("credentials/{id}")
    suspend fun getCredentialInternal(
        @Path("id") id: String,
        @Query("includeData") includeData: Boolean = false
    ): Response<CredentialDto>

    @DELETE("api/v1/credentials/{id}")
    suspend fun deleteCredential(
        @Path("id") id: String
    ): Response<Unit>

    // ==================== Variables ====================
    
    @GET("api/v1/variables")
    suspend fun getVariables(): Response<ApiResponse<VariableDto>>

    @POST("api/v1/variables")
    suspend fun createVariable(
        @Body variable: VariableDto
    ): Response<VariableDto>

    @PATCH("api/v1/variables/{id}")
    suspend fun updateVariable(
        @Path("id") id: String,
        @Body variable: VariableDto
    ): Response<VariableDto>

    @DELETE("api/v1/variables/{id}")
    suspend fun deleteVariable(
        @Path("id") id: String
    ): Response<Unit>

    // ==================== Tags ====================
    
    @GET("api/v1/tags")
    suspend fun getTags(): Response<ApiResponse<TagDto>>

    // ==================== Workflow Execution (Triggers) ====================
    
    @POST("webhook/{webhookPath}")
    suspend fun triggerWebhook(
        @Path("webhookPath", encoded = true) webhookPath: String,
        @Body data: Map<String, Any>? = null
    ): Response<Any>

    @POST("webhook-test/{webhookPath}")
    suspend fun triggerTestWebhook(
        @Path("webhookPath", encoded = true) webhookPath: String,
        @Body data: Map<String, Any>? = null
    ): Response<Any>

    // ==================== Authentication ====================

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("rest/login")
    suspend fun loginRest(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}
