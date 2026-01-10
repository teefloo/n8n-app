package com.n8n.mobilemanager.ui.screens.executions

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionStatus
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExecutionDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExecution: (String) -> Unit, /* Pour le retry qui créerait une nouvelle exécution éventuellement */
    viewModel: ExecutionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            NeumorphicDetailTopBar(
                title = "Détail exécution",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = N8nPrimary
                )
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = uiState.error ?: "Une erreur est survenue",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    NeumorphicButton(
                        text = "Réessayer",
                        onClick = { viewModel.loadExecution() },
                        isPrimary = true
                    )
                }
            } else {
                uiState.execution?.let { execution ->
                    ExecutionContent(
                        execution = execution,
                        onRetry = { viewModel.retryExecution() },
                        onStop = { viewModel.stopExecution() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NeumorphicDetailTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ExecutionContent(
    execution: Execution,
    onRetry: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Card with Status and ID
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExecutionStatusChip(status = execution.status)
                    
                    if (execution.status == ExecutionStatus.RUNNING) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Stop,
                            onClick = onStop,
                            size = 40.dp,
                            iconSize = 20.dp,
                            tint = StatusError
                        )
                    } else if (execution.status == ExecutionStatus.ERROR) {
                        NeumorphicIconButton(
                            icon = Icons.Filled.Refresh,
                            onClick = onRetry,
                            size = 40.dp,
                            iconSize = 20.dp,
                            tint = N8nPrimary
                        )
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Workflow",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = execution.workflowName ?: "Workflow ${execution.workflowId}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "ID",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "#${execution.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Mode",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = execution.mode.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // Timing Info
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Chronologie",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimingItem(
                        icon = Icons.Outlined.PlayArrow,
                        label = "Début",
                        value = formatDateTime(execution.startedAt)
                    )
                    
                    execution.stoppedAt?.let {
                        TimingItem(
                            icon = Icons.Outlined.Stop,
                            label = "Fin",
                            value = formatDateTime(it)
                        )
                    }
                    
                    if (execution.stoppedAt != null) {
                        TimingItem(
                            icon = Icons.Outlined.Timer,
                            label = "Durée",
                            value = calculateDuration(execution.startedAt, execution.stoppedAt)
                        )
                    }
                }
            }
        }
        
        // Error details if any
        if (execution.status == ExecutionStatus.ERROR && execution.data?.resultData?.error != null) {
             val error = execution.data.resultData.error
             NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp,
                backgroundColor = StatusError.copy(alpha = 0.05f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Erreur",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusError
                        )
                    }
                    
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (!error.node.isNullOrBlank()) {
                        Text(
                            text = "Nœud: ${error.node}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    if (!error.stack.isNullOrBlank()) {
                        NeumorphicCard(
                             modifier = Modifier.fillMaxWidth(),
                             cornerRadius = 12.dp,
                             backgroundColor = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = error.stack,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = N8nPrimary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDateTime(dateString: String): String {
    if (dateString.isBlank()) return "—"
    
    return try {
        val outputFormat = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        
        // Essayer plusieurs formats de date
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to null,
            "yyyy-MM-dd'T'HH:mm:ss.SSS" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss'Z'" to TimeZone.getTimeZone("UTC"),
            "yyyy-MM-dd'T'HH:mm:ss" to TimeZone.getTimeZone("UTC")
        )
        
        for ((pattern, tz) in formats) {
            try {
                val inputFormat = SimpleDateFormat(pattern, Locale.getDefault())
                if (tz != null) inputFormat.timeZone = tz
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                // Essayer le format suivant
            }
        }
        
        dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun calculateDuration(startedAt: String, stoppedAt: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val start = format.parse(startedAt)
        val stop = format.parse(stoppedAt)
        if (start != null && stop != null) {
            val durationMs = stop.time - start.time
             when {
                durationMs < 0 -> "—" /* Should not happen */
                durationMs < 1000 -> "${durationMs}ms"
                durationMs < 60000 -> "${durationMs / 1000}s"
                durationMs < 3600000 -> "${durationMs / 60000}m ${(durationMs % 60000) / 1000}s"
                else -> "${durationMs / 3600000}h ${(durationMs % 3600000) / 60000}m"
            }
        } else "—"
    } catch (e: Exception) {
        "—"
    }
}
