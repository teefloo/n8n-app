package com.n8n.mobilemanager.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.ui.components.*
import com.n8n.mobilemanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val neumorphColors = neumorphicColors()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var instanceToDelete by remember { mutableStateOf<N8nInstance?>(null) }
    
    Scaffold(
        topBar = {
            NeumorphicSettingsTopBar(onBackClick = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Instances Section
            item {
                NeumorphicSettingsSectionHeader(
                    title = "n8n Instances",
                    icon = Icons.Outlined.Cloud
                )
            }
            
            items(
                items = uiState.instances,
                key = { it.id }
            ) { instance ->
                NeumorphicInstanceCard(
                    instance = instance,
                    isActive = instance.id == uiState.activeInstanceId,
                    onSelect = { viewModel.setActiveInstance(instance.id) },
                    onEdit = { viewModel.startEditingInstance(instance) },
                    onDelete = { instanceToDelete = instance }
                )
            }
            
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    onClick = { viewModel.startAddingInstance() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = N8nPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add instance",
                            style = MaterialTheme.typography.labelLarge,
                            color = N8nPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Appearance Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NeumorphicSettingsSectionHeader(
                    title = "Appearance",
                    icon = Icons.Outlined.Palette
                )
            }
            
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    NeumorphicSettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Theme",
                        subtitle = when (uiState.themeMode) {
                            PreferencesManager.ThemeMode.LIGHT -> "Light"
                            PreferencesManager.ThemeMode.DARK -> "Dark"
                            PreferencesManager.ThemeMode.SYSTEM -> "System"
                        },
                        onClick = { showThemeDialog = true }
                    )
                }
            }
            
            // Security Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NeumorphicSettingsSectionHeader(
                    title = "Security",
                    icon = Icons.Outlined.Security
                )
            }
            
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    NeumorphicSettingsToggleRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Biometric authentication",
                        subtitle = "Protect access to the app",
                        checked = uiState.biometricEnabled,
                        onCheckedChange = viewModel::setBiometricEnabled
                    )
                }
            }
            
            // Notifications Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NeumorphicSettingsSectionHeader(
                    title = "Notifications",
                    icon = Icons.Outlined.Notifications
                )
            }
            
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column {
                        NeumorphicSettingsToggleRow(
                            icon = Icons.Outlined.ErrorOutline,
                            title = "Error alerts",
                            subtitle = "Receive a notification in case of failure",
                            checked = uiState.notifyErrors,
                            onCheckedChange = viewModel::setNotifyErrors
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )
                        
                        NeumorphicSettingsToggleRow(
                            icon = Icons.Outlined.CheckCircleOutline,
                            title = "Success notifications",
                            subtitle = "Receive a notification in case of success",
                            checked = uiState.notifySuccess,
                            onCheckedChange = viewModel::setNotifySuccess
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.testNotification() }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(N8nPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BugReport,
                                    contentDescription = null,
                                    tint = N8nPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Test notification",
                                style = MaterialTheme.typography.bodyLarge,
                                color = N8nPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // About Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                NeumorphicSettingsSectionHeader(
                    title = "About",
                    icon = Icons.Outlined.Info
                )
            }
            
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    NeumorphicSettingsRow(
                        icon = Icons.Outlined.Info,
                        title = "Version",
                        subtitle = "1.0.0"
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
    
    // Theme selection dialog with neumorphic style
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            title = { 
                Text(
                    text = "Choose theme",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreferencesManager.ThemeMode.entries.forEach { mode ->
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            isPressed = uiState.themeMode == mode,
                            onClick = {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (uiState.themeMode == mode) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        N8nPrimary.copy(alpha = 0.08f),
                                                        N8nPrimary.copy(alpha = 0.12f)
                                                    )
                                                )
                                            )
                                        } else Modifier
                                    )
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            PreferencesManager.ThemeMode.LIGHT -> Icons.Outlined.LightMode
                                            PreferencesManager.ThemeMode.DARK -> Icons.Outlined.DarkMode
                                            PreferencesManager.ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                                        },
                                        contentDescription = null,
                                        tint = if (uiState.themeMode == mode) N8nPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = when (mode) {
                                            PreferencesManager.ThemeMode.LIGHT -> "Light"
                                            PreferencesManager.ThemeMode.DARK -> "Dark"
                                            PreferencesManager.ThemeMode.SYSTEM -> "System"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (uiState.themeMode == mode) N8nPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                if (uiState.themeMode == mode) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(N8nPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = N8nPrimary)
                }
            }
        )
    }
    
    // Delete instance confirmation
    instanceToDelete?.let { instance ->
        AlertDialog(
            onDismissRequest = { instanceToDelete = null },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            title = { 
                Text(
                    text = "Delete instance?",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text("Are you sure you want to delete the instance \"${instance.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInstance(instance)
                        instanceToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = StatusError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { instanceToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
    
    // Add/Edit instance bottom sheet
    if (uiState.isAddingInstance) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelInstanceForm() },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            NeumorphicInstanceFormContent(
                isEditing = uiState.editingInstance != null,
                name = uiState.instanceName,
                url = uiState.instanceUrl,
                apiKey = uiState.instanceApiKey,
                isTesting = uiState.isTestingConnection,
                testResult = uiState.connectionTestResult,
                isSaving = uiState.isSavingInstance,
                error = uiState.error,
                onNameChange = viewModel::updateInstanceName,
                onUrlChange = viewModel::updateInstanceUrl,
                onApiKeyChange = viewModel::updateInstanceApiKey,
                onTestConnection = viewModel::testConnection,
                onSave = viewModel::saveInstance,
                onCancel = viewModel::cancelInstanceForm
            )
        }
    }
}

@Composable
private fun NeumorphicSettingsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
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
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NeumorphicSettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(N8nPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = N8nPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = N8nPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NeumorphicSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun NeumorphicSettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        NeumorphicToggle(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NeumorphicInstanceCard(
    instance: N8nInstance,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val neumorphColors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isActive) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    N8nPrimary.copy(alpha = 0.06f),
                                    N8nPrimary.copy(alpha = 0.1f)
                                )
                            )
                        )
                    } else Modifier
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .neumorphicRaised(
                        lightShadowColor = neumorphColors.lightShadow,
                        darkShadowColor = neumorphColors.darkShadow,
                        backgroundColor = if (isActive) N8nPrimary.copy(alpha = 0.2f) else neumorphColors.background,
                        shadowOffset = 3.dp,
                        shadowBlur = 6.dp,
                        cornerRadius = 14.dp
                    )
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = if (isActive) N8nPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = instance.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(N8nPrimary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = instance.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NeumorphicIconButton(
                    icon = Icons.Outlined.Edit,
                    onClick = onEdit,
                    size = 38.dp,
                    iconSize = 18.dp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                NeumorphicIconButton(
                    icon = Icons.Outlined.Delete,
                    onClick = onDelete,
                    size = 38.dp,
                    iconSize = 18.dp,
                    tint = StatusError
                )
            }
        }
    }
}

@Composable
private fun NeumorphicInstanceFormContent(
    isEditing: Boolean,
    name: String,
    url: String,
    apiKey: String,
    isTesting: Boolean,
    testResult: ConnectionTestResult?,
    isSaving: Boolean,
    error: String?,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }
    val neumorphColors = neumorphicColors()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = if (isEditing) "Edit instance" else "New instance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        // Name field with neumorphic style
        NeumorphicTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "My n8n instance",
                            leadingIcon = Icons.AutoMirrored.Outlined.Label,            modifier = Modifier.fillMaxWidth()
        )
        
        // URL field
        NeumorphicTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = "https://n8n.example.com",
            leadingIcon = Icons.Outlined.Link,
            modifier = Modifier.fillMaxWidth()
        )
        
        // API Key field
        NeumorphicTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            placeholder = "n8n_api_...",
            leadingIcon = Icons.Outlined.Key,
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (showApiKey) "Hide" else "Show",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Connection test result
        testResult?.let { result ->
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (result) {
                                is ConnectionTestResult.Success -> StatusSuccess.copy(alpha = 0.1f)
                                is ConnectionTestResult.Error -> StatusError.copy(alpha = 0.1f)
                            }
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (result) {
                            is ConnectionTestResult.Success -> Icons.Filled.CheckCircle
                            is ConnectionTestResult.Error -> Icons.Filled.Error
                        },
                        contentDescription = null,
                        tint = when (result) {
                            is ConnectionTestResult.Success -> StatusSuccess
                            is ConnectionTestResult.Error -> StatusError
                        }
                    )
                    Text(
                        text = when (result) {
                            is ConnectionTestResult.Success -> "Connection successful!"
                            is ConnectionTestResult.Error -> result.message
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (result) {
                            is ConnectionTestResult.Success -> StatusSuccess
                            is ConnectionTestResult.Error -> StatusError
                        }
                    )
                }
            }
        }
        
        // Error message
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = StatusError
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Test connection button
        NeumorphicButton(
            text = if (isTesting) "Testing..." else "Test connection",
            onClick = onTestConnection,
            icon = Icons.Outlined.NetworkCheck,
            isPrimary = false,
            enabled = !isTesting && url.isNotBlank() && apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeumorphicButton(
                text = "Cancel",
                onClick = onCancel,
                isPrimary = false,
                modifier = Modifier.weight(1f)
            )
            
            NeumorphicButton(
                text = if (isSaving) "Saving..." else "Save",
                onClick = onSave,
                enabled = !isSaving && name.isNotBlank() && url.isNotBlank() && apiKey.isNotBlank(),
                isPrimary = true,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
