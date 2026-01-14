package com.n8n.mobilemanager.ui.screens.login

import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.data.model.InstanceStatus
import com.n8n.mobilemanager.data.repository.N8nRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val repository: N8nRepository = mockk(relaxed = true)
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Default behavior for active instance check
        coEvery { repository.getActiveInstanceFlow() } returns flowOf(null)
        
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.instanceName)
        assertEquals("", state.instanceUrl)
        assertEquals("", state.instanceApiKey)
        assertFalse(state.isLoggedIn)
        assertNull(state.error)
    }

    @Test
    fun `update fields updates state`() {
        viewModel.updateInstanceName("My Instance")
        assertEquals("My Instance", viewModel.uiState.value.instanceName)

        viewModel.updateInstanceUrl("https://n8n.com")
        assertEquals("https://n8n.com", viewModel.uiState.value.instanceUrl)

        viewModel.updateInstanceApiKey("key123")
        assertEquals("key123", viewModel.uiState.value.instanceApiKey)
    }

    @Test
    fun `testConnection fails with empty url`() {
        viewModel.updateInstanceApiKey("key")
        viewModel.testConnection()
        assertEquals("URL requise", viewModel.uiState.value.error)
    }

    @Test
    fun `testConnection success updates state`() = runTest {
        // Given
        viewModel.updateInstanceUrl("https://valid.com")
        viewModel.updateInstanceApiKey("valid_key")
        
        coEvery { repository.testConnection(any()) } returns Result.success(
            InstanceStatus(true, 0, 0)
        )

        // When
        viewModel.testConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.connectionTestResult)
        assertTrue(state.connectionTestResult!!.isSuccess)
        assertFalse(state.isTesting)
    }

    @Test
    fun `testConnection failure updates state with error`() = runTest {
        // Given
        viewModel.updateInstanceUrl("https://invalid.com")
        viewModel.updateInstanceApiKey("key")
        
        coEvery { repository.testConnection(any()) } returns Result.failure(Exception("Network Error"))

        // When
        viewModel.testConnection()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNotNull(state.connectionTestResult)
        assertFalse(state.connectionTestResult!!.isSuccess)
        assertEquals("Network Error", state.connectionTestResult!!.errorMessage)
        assertFalse(state.isTesting)
    }

    @Test
    fun `saveInstance success logs in user`() = runTest {
        // Given
        viewModel.updateInstanceName("Name")
        viewModel.updateInstanceUrl("https://url.com")
        viewModel.updateInstanceApiKey("key")
        
        coEvery { repository.addInstance(any(), any(), any()) } returns Result.success(
            N8nInstance(1, "Name", "https://url.com", "key")
        )

        // When
        viewModel.saveInstance()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertFalse(viewModel.uiState.value.isSaving)
    }
}
