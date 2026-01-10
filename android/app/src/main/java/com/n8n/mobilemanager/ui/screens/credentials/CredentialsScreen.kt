package com.n8n.mobilemanager.ui.screens.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import java.text.SimpleDateFormat
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
                placeholder = "Rechercher des credentials…",
                leadingIcon = Icons.Default.Search,
                trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Effacer",
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
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadCredentials() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    // État de chargement initial
                    uiState.isLoading && uiState.credentials.isEmpty() -> {
                        LoadingState(
                            message = "Chargement des credentials...",
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
                            title = if (isAccessDenied) "Accès non autorisé" else "Erreur de chargement",
                            message = uiState.error ?: "Erreur inconnue",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Liste vide sans erreur
                    filteredCredentials.isEmpty() && !uiState.isLoading -> {
                        EmptyState(
                            icon = Icons.Outlined.Key,
                            title = if (uiState.searchQuery.isNotEmpty()) "Aucun résultat" else "Aucun credential",
                            message = if (uiState.searchQuery.isNotEmpty()) 
                                "Essayez avec d'autres termes" 
                            else 
                                "Ajoutez vos credentials dans n8n",
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
                                    lastUpdated = formatDate(credential.updatedAt),
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
                icon = Icons.Default.ArrowBack,
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
        .replace("Api", "API")
        .replace("Oauth2", "OAuth 2.0")
        .split(Regex("(?=[A-Z])"))
        .joinToString(" ")
        .trim()
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
