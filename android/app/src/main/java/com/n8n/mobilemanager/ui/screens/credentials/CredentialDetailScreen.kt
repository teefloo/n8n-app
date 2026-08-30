package com.n8n.mobilemanager.ui.screens.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Credential
import com.n8n.mobilemanager.data.model.NodeAccess
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.LoadingState
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nSectionHeader
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicCard
import com.n8n.mobilemanager.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun CredentialDetailScreen(
    credentialId: String = "",
    viewModel: CredentialsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val credential = uiState.currentCredential?.takeIf { it.id == credentialId }
        ?: uiState.credentials.firstOrNull { it.id == credentialId }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(credentialId) {
        if (credentialId.isNotBlank()) viewModel.loadCredential(credentialId)
    }

    Scaffold(
        topBar = {
            N8nTopAppBar(
                title = credential?.name ?: "Credential details",
                onBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            credentialId.isBlank() -> EmptyState(
                icon = Icons.Outlined.ErrorOutline,
                title = "Invalid credential",
                message = "The credential ID is missing.",
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
            uiState.isLoading && credential == null -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                message = "Loading credential…"
            )
            credential == null -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    uiState.error?.let { message ->
                        N8nErrorBanner(message = message, modifier = Modifier.padding(20.dp))
                    }
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Credential not found",
                        message = "It may have been removed or you may not have access.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        action = {
                            TextButton(onClick = { viewModel.loadCredential(credentialId) }) {
                                Text("Try again")
                            }
                        }
                    )
                }
            }
            else -> {
                credential?.let { loadedCredential ->
                    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        uiState.error?.let { message ->
                            N8nErrorBanner(message = message, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                        }
                        CredentialContent(
                            credential = loadedCredential,
                            onCopyId = {
                                clipboardManager.setText(AnnotatedString(loadedCredential.id))
                                scope.launch { snackbarHostState.showSnackbar("Credential ID copied") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialContent(credential: Credential, onCopyId: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CredentialMainInfo(credential) }
        item { CredentialDates(credential) }
        if (credential.nodesAccess.isNotEmpty()) {
            item { N8nSectionHeader(title = "Used by") }
            items(credential.nodesAccess) { nodeAccess -> NodeAccessCard(nodeAccess) }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onCopyId) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy ID · ${credential.id.take(8)}…")
                }
            }
        }
    }
}

@Composable
private fun CredentialMainInfo(credential: Credential) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp).size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatCredentialType(credential.type),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(credential.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CredentialDates(credential: Credential) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailSmallCard(
            modifier = Modifier.weight(1f),
            label = "Created",
            value = DateUtils.formatFullDate(credential.createdAt),
            icon = Icons.Outlined.CalendarToday
        )
        DetailSmallCard(
            modifier = Modifier.weight(1f),
            label = "Updated",
            value = DateUtils.formatFullDate(credential.updatedAt),
            icon = Icons.Outlined.Update
        )
    }
}

@Composable
private fun DetailSmallCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    NeumorphicCard(modifier = modifier, cornerRadius = 14.dp) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun NodeAccessCard(nodeAccess: NodeAccess) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.AccountTree, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(formatCredentialType(nodeAccess.nodeType), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                nodeAccess.date?.let {
                    Text(DateUtils.formatFullDate(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatCredentialType(type: String): String = type
    .split(Regex("(?=[A-Z])"))
    .joinToString(" ")
    .trim()
    .replace("Api", "API")
    .replace("Oauth2", "OAuth 2.0")
    .replace(" My Sql", " MySQL")
    .replace(" Ai", " AI")
