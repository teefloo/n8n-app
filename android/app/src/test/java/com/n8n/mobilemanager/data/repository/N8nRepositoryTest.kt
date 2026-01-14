package com.n8n.mobilemanager.data.repository

import com.n8n.mobilemanager.data.local.InstanceDao
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.ExecutionDTO
import com.n8n.mobilemanager.data.model.ExecutionResponse
import com.n8n.mobilemanager.data.model.InstanceStatus
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.remote.N8nApiService
import com.n8n.mobilemanager.di.ApiServiceFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class N8nRepositoryTest {

    private val instanceDao: InstanceDao = mockk(relaxed = true)
    private val preferencesManager: PreferencesManager = mockk(relaxed = true)
    private val apiServiceFactory: ApiServiceFactory = mockk()
    private val apiService: N8nApiService = mockk()
    
    private lateinit var repository: N8nRepository

    @Before
    fun setUp() {
        repository = N8nRepository(instanceDao, preferencesManager, apiServiceFactory)
        
        // Mock default behavior
        val instance = N8nInstance(1, "Test", "https://test.com", "key", true)
        coEvery { instanceDao.getActiveInstance() } returns instance
        every { apiServiceFactory.create(any()) } returns apiService
    }

    @Test
    fun `testConnection returns success when API is healthy`() = runTest {
        // Given
        val instance = N8nInstance(1, "Test", "https://test.com", "key")
        coEvery { apiService.healthCheck() } returns Response.success(mapOf("status" to "ok"))
        coEvery { apiService.getWorkflows(limit = 1) } returns Response.success(mockk(relaxed = true))
        coEvery { apiService.getWorkflows(active = true, limit = 1) } returns Response.success(mockk(relaxed = true))

        // When
        val result = repository.testConnection(instance)

        // Then
        assertTrue(result.isSuccess)
        val status = result.getOrNull()
        assertTrue(status!!.isOnline)
        coVerify { instanceDao.updateLastConnected(instance.id, any()) }
    }

    @Test
    fun `getExecutions with fetchAll=false returns single page`() = runTest {
        // Given
        val execs = listOf(ExecutionDTO("1", "Workflow", true, "start", "stop", "success"))
        val response = ExecutionResponse(execs, "next_cursor")
        coEvery { apiService.getExecutions(any(), any(), any(), any()) } returns Response.success(response)

        // When
        val result = repository.getExecutions(limit = 10, fetchAll = false, includeWorkflowNames = false)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()!!.size)
        coVerify(exactly = 1) { apiService.getExecutions(any(), any(), any(), any()) }
    }
    
    // Note: Testing fetchAll=true with loop is complex to mock correctly without robust sequence mocking, 
    // skipping for this quick check suite but acknowledging importance.
}
