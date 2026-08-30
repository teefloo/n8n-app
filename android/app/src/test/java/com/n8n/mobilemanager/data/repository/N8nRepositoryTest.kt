package com.n8n.mobilemanager.data.repository

import com.n8n.mobilemanager.data.local.InstanceDao
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.remote.dto.ApiResponse
import com.n8n.mobilemanager.data.remote.dto.ExecutionDto
import com.n8n.mobilemanager.data.remote.dto.HealthCheckResponse
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.remote.N8nApiService
import com.n8n.mobilemanager.di.ApiServiceFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class N8nRepositoryTest {

    private val instanceDao: InstanceDao = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val apiServiceFactory: ApiServiceFactory = mockk()
    private val apiService: N8nApiService = mockk()
    
    private lateinit var repository: N8nRepository

    @Before
    fun setUp() {
        repository = N8nRepository(instanceDao, preferencesManager, apiServiceFactory)
        
        val instance = N8nInstance(1, "Test", "https://test.com", "key", true)
        coEvery { instanceDao.getActiveInstance() } returns instance
        every { apiServiceFactory.create(any()) } returns apiService
    }

    @Test
    fun `testConnection returns success when API is healthy`() = runTest {
        val instance = N8nInstance(1, "Test", "https://test.com", "key")
        coEvery { apiService.healthCheck() } returns Response.success(HealthCheckResponse("ok"))
        coEvery { apiService.getWorkflows(limit = 1) } returns Response.success(ApiResponse(emptyList(), null))
        coEvery { apiService.getWorkflows(active = true, limit = 1) } returns Response.success(ApiResponse(emptyList(), null))

        val result = repository.testConnection(instance)

        assertTrue(result.isSuccess)
        val status = result.getOrNull()
        assertTrue(status!!.isOnline)
        coVerify { instanceDao.updateLastConnected(instance.id, any()) }
        coVerify(exactly = 0) { apiService.getWorkflows(any(), any(), any(), any()) }
    }

    @Test
    fun `getExecutions with fetchAll=false returns single page`() = runTest {
        val execs = listOf(
            ExecutionDto(
                id = "1",
                workflowId = "wf1",
                workflowName = "Workflow",
                finished = true,
                mode = "manual",
                status = "success",
                startedAt = "2024-01-01T00:00:00.000Z",
                stoppedAt = "2024-01-01T00:00:01.000Z"
            )
        )
        val response = ApiResponse(execs, "next_cursor")
        coEvery { apiService.getExecutions(any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.getExecutions(limit = 10, fetchAll = false, includeWorkflowNames = false)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isNotEmpty())
        coVerify(exactly = 1) { apiService.getExecutions(any(), any(), any(), any()) }
    }
}
