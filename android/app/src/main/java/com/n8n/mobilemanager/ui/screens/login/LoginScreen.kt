package com.n8n.mobilemanager.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.R
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Observer le changement d'état de connexion
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo placeholder (ou icône)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(N8nPrimary.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = "Logo",
                    tint = N8nPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Connect your n8n instance",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Formulaire
            NeumorphicTextField(
                value = uiState.instanceName,
                onValueChange = viewModel::updateInstanceName,
                placeholder = "Instance name",
                leadingIcon = Icons.AutoMirrored.Outlined.Label,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            NeumorphicTextField(
                value = uiState.instanceUrl,
                onValueChange = viewModel::updateInstanceUrl,
                placeholder = "URL (ex: https://n8n.example.com)",
                leadingIcon = Icons.Outlined.Link,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            NeumorphicTextField(
                value = uiState.instanceApiKey,
                onValueChange = viewModel::updateInstanceApiKey,
                placeholder = "API Key",
                leadingIcon = Icons.Outlined.Key,
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                        Icon(
                            imageVector = if (uiState.showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.showApiKey) "Hide" else "Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (uiState.showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Connection Test Result
            uiState.connectionTestResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                    backgroundColor = if (result.isSuccess) StatusSuccess.copy(alpha = 0.1f) else StatusError.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (result.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (result.isSuccess) StatusSuccess else StatusError
                        )
                        Text(
                            text = if (result.isSuccess) "Connection successful" else (result.errorMessage ?: "Error"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (result.isSuccess) StatusSuccess else StatusError
                        )
                    }
                }
            }
            
            // Error
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusError,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NeumorphicButton(
                    text = if (uiState.isTesting) "Testing..." else "Test",
                    onClick = viewModel::testConnection,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTesting && !uiState.isSaving,
                    isPrimary = false
                )
                
                NeumorphicButton(
                    text = if (uiState.isSaving) "Connecting..." else "Connect",
                    onClick = viewModel::saveInstance,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTesting && !uiState.isSaving,
                    isPrimary = true
                )
            }
        }
    }
}