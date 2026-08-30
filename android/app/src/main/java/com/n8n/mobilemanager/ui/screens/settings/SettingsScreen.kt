package com.n8n.mobilemanager.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.local.PreferencesManager
import com.n8n.mobilemanager.data.model.N8nInstance
import com.n8n.mobilemanager.ui.components.N8nErrorBanner
import com.n8n.mobilemanager.ui.components.N8nSectionHeader
import com.n8n.mobilemanager.ui.components.N8nTopAppBar
import com.n8n.mobilemanager.ui.components.NeumorphicCard
import com.n8n.mobilemanager.ui.components.NeumorphicTextField
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.ui.theme.StatusSuccess

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNoActiveInstance: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var instanceToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedInstance = uiState.instances.firstOrNull { it.id == instanceToDelete }

    LaunchedEffect(uiState.hasLoadedInstances, uiState.instances.isEmpty(), uiState.isAddingInstance) {
        if (uiState.hasLoadedInstances && uiState.instances.isEmpty() && !uiState.isAddingInstance) {
            onNoActiveInstance()
        }
    }

    Scaffold(
        topBar = { N8nTopAppBar(title = "Settings", onBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SettingsSectionHeader("n8n instances", Icons.Filled.Cloud) }
            items(uiState.instances, key = { it.id }) { instance ->
                InstanceCard(
                    instance = instance,
                    isActive = instance.id == uiState.activeInstanceId,
                    onSelect = { viewModel.setActiveInstance(instance.id) },
                    onEdit = { viewModel.startEditingInstance(instance) },
                    onDelete = { instanceToDelete = instance.id }
                )
            }
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::startAddingInstance,
                    onClickLabel = "Add n8n instance"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Add instance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item { SettingsSectionHeader("Appearance", Icons.Filled.Palette) }
            item {
                SettingsCard {
                    SettingsValueRow(
                        icon = Icons.Filled.Palette,
                        title = "Theme",
                        subtitle = uiState.themeMode.displayName(),
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            item { SettingsSectionHeader("Security", Icons.Filled.Security) }
            item {
                SettingsCard {
                    SettingsToggleRow(
                        icon = Icons.Filled.Fingerprint,
                        title = "Biometric authentication",
                        subtitle = "Protect access to the app",
                        checked = uiState.biometricEnabled,
                        onCheckedChange = viewModel::setBiometricEnabled
                    )
                }
            }

            item { SettingsSectionHeader("Notifications", Icons.Filled.Notifications) }
            item {
                SettingsCard {
                    Column {
                        SettingsToggleRow(
                            icon = Icons.Outlined.ErrorOutline,
                            title = "Error alerts",
                            subtitle = "Notify me when a workflow fails",
                            checked = uiState.notifyErrors,
                            onCheckedChange = viewModel::setNotifyErrors
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsToggleRow(
                            icon = Icons.Filled.CheckCircle,
                            title = "Success notifications",
                            subtitle = "Notify me when a workflow succeeds",
                            checked = uiState.notifySuccess,
                            onCheckedChange = viewModel::setNotifySuccess
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Test notification") },
                            supportingContent = { Text("Send a sample notification to this device") },
                            leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = viewModel::testNotification)
                        )
                    }
                }
            }

            item { SettingsSectionHeader("About", Icons.Filled.Info) }
            item {
                SettingsCard {
                    SettingsValueRow(icon = Icons.Filled.Info, title = "Version", subtitle = "1.0.0")
                }
            }

            uiState.error?.let { message ->
                item { N8nErrorBanner(message = message, actionLabel = "Dismiss", onAction = viewModel::clearError) }
            }
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            selected = uiState.themeMode,
            onSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    selectedInstance?.let { instance ->
        AlertDialog(
            onDismissRequest = { instanceToDelete = null },
            title = { Text("Delete instance?") },
            text = { Text("Remove \"${instance.name}\" from this device?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteInstance(instance)
                        instanceToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { instanceToDelete = null }) { Text("Cancel") } }
        )
    }

    if (uiState.isAddingInstance) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelInstanceForm,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            InstanceForm(
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
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    NeumorphicCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, content = { content() })
}

@Composable
private fun SettingsValueRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = if (onClick != null) {
            { Icon(Icons.Filled.ChevronRight, contentDescription = "Open") }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InstanceCard(
    instance: N8nInstance,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        onClickLabel = "Select ${instance.name}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Filled.Cloud,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(instance.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (isActive) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                            Text("Active", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Text(instance.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit ${instance.name}") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${instance.name}", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ThemeDialog(
    selected: PreferencesManager.ThemeMode,
    onSelected: (PreferencesManager.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose theme") },
        text = {
            Column {
                PreferencesManager.ThemeMode.entries.forEach { mode ->
                    ListItem(
                        headlineContent = { Text(mode.displayName()) },
                        leadingContent = { RadioButton(selected = selected == mode, onClick = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(mode) }
                            .semantics { role = Role.RadioButton }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun InstanceForm(
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
    var showApiKey by rememberSaveable(isEditing) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(if (isEditing) "Edit instance" else "Add instance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("Keep the API key on this device only. It is encrypted before local storage.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        NeumorphicTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Instance name",
            leadingIcon = Icons.AutoMirrored.Outlined.Label,
            modifier = Modifier.fillMaxWidth()
        )
        NeumorphicTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = "https://n8n.example.com",
            leadingIcon = Icons.Outlined.Link,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
        NeumorphicTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            placeholder = "API key",
            leadingIcon = Icons.Outlined.Key,
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                    )
                }
            },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        testResult?.let { result ->
            val isSuccess = result is ConnectionTestResult.Success
            Surface(
                color = if (isSuccess) StatusSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (isSuccess) StatusSuccess else MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error, contentDescription = null)
                    Text(if (isSuccess) "Connection successful" else (result as ConnectionTestResult.Error).message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        error?.let { N8nErrorBanner(message = it) }

        Button(
            onClick = onTestConnection,
            enabled = !isTesting && !isSaving && url.isNotBlank() && apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.NetworkCheck, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isTesting) "Testing…" else "Test connection")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = onSave,
                enabled = !isSaving && name.isNotBlank() && url.isNotBlank() && apiKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text(if (isSaving) "Saving…" else "Save") }
        }
        Spacer(Modifier.size(12.dp))
    }
}

private fun PreferencesManager.ThemeMode.displayName(): String = when (this) {
    PreferencesManager.ThemeMode.LIGHT -> "Light"
    PreferencesManager.ThemeMode.DARK -> "Dark"
    PreferencesManager.ThemeMode.SYSTEM -> "System default"
}
