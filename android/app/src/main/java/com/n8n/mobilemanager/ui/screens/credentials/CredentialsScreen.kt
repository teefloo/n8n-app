package com.n8n.mobilemanager.ui.screens.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import com.n8n.mobilemanager.utils.DateUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(
    viewModel: CredentialsViewModel = hiltViewModel(),
    onNavigateToCredential: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Derive filtered credentials from uiState to be reactive
    val filteredCredentials = remember(uiState.credentials, uiState.searchQuery) {
        if (uiState.searchQuery.isEmpty()) {
            uiState.credentials
        } else {
            uiState.credentials.filter { credential ->
                credential.name.contains(uiState.searchQuery, ignoreCase = true) ||
                credential.type.contains(uiState.searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Login Dialog
    if (uiState.showLoginDialog) {
        LoginDialog(
            isLoading = uiState.isLoggingIn,
            onDismiss = { viewModel.hideLoginDialog() },
            onLogin = { email, password ->
                viewModel.loadCredentialsWithPassword(email, password)
            }
        )
    }
    
    Scaffold(
        topBar = {
            CredentialsTopBar(onBackClick = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Search bar with neumorphic style
            NeumorphicTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = "Search credentials…",
                leadingIcon = Icons.Default.Search,
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Credentials count (only show when not loading and no error)
            if (!uiState.isLoading && uiState.error == null) {
                Text(
                    text = "${filteredCredentials.size} credential(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Content
            PullToRefreshBox(
                isRefreshing = uiState.isLoading || uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    // État de chargement initial
                    uiState.isLoading && uiState.credentials.isEmpty() -> {
                        LoadingState(
                            message = "Loading credentials...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Erreur avec liste vide
                    uiState.error != null && uiState.credentials.isEmpty() -> {
                        val isAccessDenied = uiState.error?.contains("405") == true || 
                                             uiState.error?.contains("autorisé") == true ||
                                             uiState.error?.contains("scopes") == true
                        
                        EmptyState(
                            icon = if (isAccessDenied) Icons.Outlined.Lock else Icons.Outlined.ErrorOutline,
                            title = if (isAccessDenied) "Access denied" else "Loading error",
                            message = uiState.error ?: "Unknown error",
                            modifier = Modifier.fillMaxSize(),
                            action = {
                                NeumorphicButton(
                                    text = "Login",
                                    icon = Icons.AutoMirrored.Filled.Login,
                                    onClick = { viewModel.showLoginDialog() },
                                    modifier = Modifier.width(240.dp)
                                )
                            }
                        )
                    }
                    // Liste vide sans erreur
                    filteredCredentials.isEmpty() && !uiState.isLoading -> {
                        EmptyState(
                            icon = Icons.Outlined.Key,
                            title = if (uiState.searchQuery.isNotEmpty()) "No results" else "No credentials",
                            message = if (uiState.searchQuery.isNotEmpty()) 
                                "Try with other terms" 
                            else 
                                "Configure your credentials in n8n",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Liste avec des credentials
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = filteredCredentials,
                                key = { it.id }
                            ) { credential ->
                                CredentialCard(
                                    name = credential.name,
                                    type = formatCredentialType(credential.type),
                                    lastUpdated = "Updated on ${DateUtils.formatFullDate(credential.updatedAt)}",
                                    onClick = { onNavigateToCredential(credential.id) }
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(60.dp))
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
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        NeumorphicCard(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Authentication Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Please login to access credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Email
                NeumorphicTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    leadingIcon = Icons.Default.Email,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Password
                NeumorphicTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide" else "Show",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    NeumorphicButton(
                        text = if (isLoading) "Connecting..." else "Login",
                        onClick = { onLogin(email, password) },
                        modifier = Modifier.width(140.dp),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialsTopBar(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeumorphicIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBackClick,
                size = 44.dp,
                iconSize = 22.dp,
                tint = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "Credentials",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun formatCredentialType(type: String): String {
    return type
        .split(Regex("(?=[A-Z])"))
        .joinToString(" ")
        .trim()
        .replace("Api", "API")
        .replace("Oauth2", "OAuth 2.0")
        .replace(" My Sql", " MySQL")
        .replace(" Ai", " AI")
}
