package com.n8n.mobilemanager.ui.screens.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.LoadingState
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nSectionHeader
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicTextField
import com.n8n.mobilemanager.ui.components.CredentialCard
import com.n8n.mobilemanager.utils.DateUtils
import androidx.compose.foundation.text.KeyboardOptions

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun CredentialsScreen(
    viewModel: CredentialsViewModel = hiltViewModel(),
    onNavigateToCredential: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showLoginDialog) {
        LoginDialog(
            isLoading = uiState.isLoggingIn,
            errorMessage = uiState.error,
            onDismiss = viewModel::hideLoginDialog,
            onLogin = viewModel::loadCredentialsWithPassword
        )
    }

    Scaffold(
        topBar = { N8nTopAppBar(title = "Credentials", onBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NeumorphicTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = "Search credentials",
                leadingIcon = Icons.Filled.Search,
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.filteredCredentials.size} credentials",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.isRefreshing) {
                    Text("Updating…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            uiState.error?.takeIf { uiState.credentials.isNotEmpty() }?.let { message ->
                N8nErrorBanner(
                    message = message,
                    actionLabel = "Retry",
                    onAction = viewModel::refresh,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.credentials.isEmpty() -> {
                        LoadingState(message = "Loading credentials…")
                    }
                    uiState.error != null && uiState.credentials.isEmpty() -> {
                        val accessDenied = uiState.error?.contains("denied", ignoreCase = true) == true ||
                            uiState.error?.contains("scope", ignoreCase = true) == true
                        EmptyState(
                            icon = if (accessDenied) Icons.Outlined.Lock else Icons.Outlined.ErrorOutline,
                            title = if (accessDenied) "Access denied" else "Unable to load credentials",
                            message = uiState.error ?: "An unexpected error occurred.",
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            action = {
                                Button(onClick = viewModel::showLoginDialog) {
                                    Text("Sign in")
                                }
                            }
                        )
                    }
                    uiState.filteredCredentials.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Outlined.Key,
                            title = if (uiState.searchQuery.isNotBlank()) "No matching credentials" else "No credentials",
                            message = if (uiState.searchQuery.isNotBlank()) "Try a different search." else "Configure credentials in n8n.",
                            modifier = Modifier.fillMaxWidth().padding(20.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.filteredCredentials, key = { it.id }) { credential ->
                                CredentialCard(
                                    name = credential.name,
                                    type = formatCredentialType(credential.type),
                                    lastUpdated = "Updated ${DateUtils.formatFullDate(credential.updatedAt)}",
                                    onClick = { onNavigateToCredential(credential.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Sign in to n8n") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Use your n8n account only when the API key does not have credential access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NeumorphicTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    leadingIcon = Icons.Filled.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                NeumorphicTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    leadingIcon = Icons.Filled.Lock,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    N8nErrorBanner(message = it)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(email.trim(), password) },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                Text(if (isLoading) "Signing in…" else "Sign in")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}

private fun formatCredentialType(type: String): String = type
    .split(Regex("(?=[A-Z])"))
    .joinToString(" ")
    .trim()
    .replace("Api", "API")
    .replace("Oauth2", "OAuth 2.0")
    .replace(" My Sql", " MySQL")
    .replace(" Ai", " AI")
