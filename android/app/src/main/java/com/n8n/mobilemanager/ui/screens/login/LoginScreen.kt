package com.n8n.mobilemanager.ui.screens.login

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val neumorphColors = neumorphicColors()
    
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        N8nPrimary.copy(alpha = 0.03f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Logo / Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .neumorphicShadow(
                        lightShadowColor = neumorphColors.lightShadow,
                        darkShadowColor = neumorphColors.darkShadow,
                        shadowOffset = 8.dp,
                        cornerRadius = 28.dp
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                N8nPrimary,
                                N8nSecondary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Cloud,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "n8n Manager",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Connectez-vous à votre instance n8n",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Form Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Instance Name
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Nom de l'instance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        NeumorphicTextField(
                            value = uiState.instanceName,
                            onValueChange = viewModel::updateInstanceName,
                            placeholder = "Mon instance n8n",
                            leadingIcon = Icons.Outlined.Label,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // URL
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "URL de l'instance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        NeumorphicTextField(
                            value = uiState.instanceUrl,
                            onValueChange = viewModel::updateInstanceUrl,
                            placeholder = "https://n8n.example.com",
                            leadingIcon = Icons.Outlined.Link,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // API Key
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Clé API",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        NeumorphicTextField(
                            value = uiState.instanceApiKey,
                            onValueChange = viewModel::updateInstanceApiKey,
                            placeholder = "n8n_api_...",
                            leadingIcon = Icons.Outlined.Key,
                            trailingIcon = {
                                IconButton(onClick = viewModel::toggleApiKeyVisibility) {
                                    Icon(
                                        imageVector = if (uiState.showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (uiState.showApiKey) "Masquer" else "Afficher",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Connection test result
                    AnimatedVisibility(
                        visible = uiState.connectionTestResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        uiState.connectionTestResult?.let { result ->
                            NeumorphicCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (result.isSuccess) StatusSuccess.copy(alpha = 0.1f)
                                            else StatusError.copy(alpha = 0.1f)
                                        )
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (result.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = if (result.isSuccess) StatusSuccess else StatusError
                                    )
                                    Text(
                                        text = if (result.isSuccess) "Connexion réussie !" else result.errorMessage ?: "Erreur de connexion",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (result.isSuccess) StatusSuccess else StatusError
                                    )
                                }
                            }
                        }
                    }
                    
                    // Error message
                    AnimatedVisibility(
                        visible = uiState.error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        uiState.error?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Test Connection Button
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                onClick = { if (!uiState.isTesting) viewModel.testConnection() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.NetworkCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.isTesting) "Test en cours..." else "Tester la connexion",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Connect Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphicShadow(
                        lightShadowColor = neumorphColors.lightShadow,
                        darkShadowColor = neumorphColors.darkShadow,
                        shadowOffset = 6.dp,
                        cornerRadius = 18.dp
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(N8nPrimary, N8nSecondary)
                        )
                    )
            ) {
                Button(
                    onClick = { viewModel.saveInstance() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Login,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.isSaving) "Connexion..." else "Se connecter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Help text
            Text(
                text = "Vous pouvez créer une clé API dans les paramètres de votre instance n8n",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
