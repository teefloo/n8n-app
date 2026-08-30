package com.n8n.mobilemanager.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.ui.components.NeumorphicButton
import com.n8n.mobilemanager.ui.components.NeumorphicTextField
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.ui.theme.StatusSuccess
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountTree,
                    contentDescription = "n8n Mobile Manager",
                    modifier = Modifier.padding(20.dp).size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.size(20.dp))
            Text("Connect an n8n instance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(6.dp))
            Text(
                "Add an API key to monitor workflows and executions from your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp)
            )
            Spacer(Modifier.size(24.dp))

            NeumorphicTextField(
                value = uiState.instanceName,
                onValueChange = viewModel::updateInstanceName,
                placeholder = "Instance name",
                leadingIcon = Icons.AutoMirrored.Outlined.Label,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(12.dp))
            NeumorphicTextField(
                value = uiState.instanceUrl,
                onValueChange = viewModel::updateInstanceUrl,
                placeholder = "https://n8n.example.com",
                leadingIcon = Icons.Outlined.Link,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(12.dp))
            NeumorphicTextField(
                value = uiState.instanceApiKey,
                onValueChange = viewModel::updateInstanceApiKey,
                placeholder = "API key",
                leadingIcon = Icons.Outlined.Key,
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                        Icon(
                            imageVector = if (uiState.showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.showApiKey) "Hide API key" else "Show API key"
                        )
                    }
                },
                visualTransformation = if (uiState.showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            uiState.connectionTestResult?.let { result ->
                Spacer(Modifier.size(12.dp))
                val success = result.isSuccess
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (success) StatusSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (success) StatusSuccess else MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (success) Icons.Filled.CheckCircle else Icons.Filled.Error, contentDescription = null)
                        Text(if (success) "Connection successful" else (result.errorMessage ?: "Connection failed"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            uiState.error?.let {
                Spacer(Modifier.size(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.size(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::testConnection,
                    enabled = !uiState.isTesting && !uiState.isSaving && uiState.instanceUrl.isNotBlank() && uiState.instanceApiKey.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text(if (uiState.isTesting) "Testing…" else "Test") }
                NeumorphicButton(
                    text = if (uiState.isSaving) "Connecting…" else "Connect",
                    onClick = viewModel::saveInstance,
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTesting && !uiState.isSaving,
                    isPrimary = true
                )
            }
        }
    }
}
