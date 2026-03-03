package com.n8n.mobilemanager.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.n8n.mobilemanager.data.model.Execution
import com.n8n.mobilemanager.data.model.ExecutionMode
import com.n8n.mobilemanager.data.model.InstanceStats
import com.n8n.mobilemanager.ui.components.EmptyState
import com.n8n.mobilemanager.ui.components.ExecutionStatusChip
import com.n8n.mobilemanager.ui.components.NeumorphicCard
import com.n8n.mobilemanager.ui.components.NeumorphicIconButton
import com.n8n.mobilemanager.ui.components.neumorphicColors
import com.n8n.mobilemanager.ui.components.neumorphicRaised
import com.n8n.mobilemanager.ui.theme.N8nPrimary
import com.n8n.mobilemanager.ui.theme.StatusError
import com.n8n.mobilemanager.ui.theme.StatusSuccess
import com.n8n.mobilemanager.utils.DateUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToWorkflows: () -> Unit,
    onNavigateToExecutions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                title = uiState.instance?.name ?: "Dashboard",
                isOnline = uiState.isOnline,
                onSettingsClick = onNavigateToSettings,
                isVisible = isVisible
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        initialOffsetY = { 50 }
                    )
                ) {
                    PeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = viewModel::setPeriod
                    )
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 100)) + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        initialOffsetY = { 75 }
                    )
                ) {
                    StatsGrid(stats = uiState.stats)
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(500, delayMillis = 200)) + slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        initialOffsetY = { 100 }
                    )
                ) {
                    RecentExecutionsSection(
                        executions = uiState.recentExecutions,
                        onSeeAllClick = onNavigateToExecutions,
                        onExecutionClick = onNavigateToExecution
                    )
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DashboardTopBar(
    title: String,
    isOnline: Boolean,
    onSettingsClick: () -> Unit,
    isVisible: Boolean
) {
    val topBarAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "TopBarAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(topBarAlpha)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatusIndicatorPulse(isOnline = isOnline)
            }
        }
        
        NeumorphicIconButton(
            icon = Icons.Outlined.Settings,
            onClick = onSettingsClick,
            size = 48.dp,
            iconSize = 24.dp,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatusIndicatorPulse(isOnline: Boolean) {
    var pulse by remember { mutableStateOf(false) }
    
    LaunchedEffect(isOnline) {
        if (isOnline) {
            while (true) {
                pulse = true
                delay(1000)
                pulse = false
                delay(1000)
            }
        } else {
            pulse = false
        }
    }
    
    val size by animateFloatAsState(
        targetValue = if (pulse) 12f else 8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "StatusSize"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (pulse) 0.6f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "StatusAlpha"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(if (isOnline) StatusSuccess else StatusError)
    )
}

@Composable
fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)
    ) {
        items(StatsPeriod.entries.toTypedArray()) { period ->
            val isSelected = period == selectedPeriod
            val colors = neumorphicColors()
            
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .neumorphicRaised(
                        lightShadowColor = if (isSelected) colors.lightShadow.copy(alpha = 0.4f) else colors.lightShadow,
                        darkShadowColor = if (isSelected) colors.darkShadow.copy(alpha = 0.4f) else colors.darkShadow,
                        backgroundColor = if (isSelected) colors.primary else colors.background,
                        cornerRadius = 21.dp,
                        shadowOffset = 4.dp,
                        shadowBlur = 8.dp
                    )
                    .clip(RoundedCornerShape(21.dp))
                    .clickable { 
                        onPeriodSelected(period) 
                    }
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else colors.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatsGrid(stats: InstanceStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Workflows",
                value = stats.totalWorkflows.toString(),
                icon = Icons.Outlined.AccountTree,
                color = N8nPrimary,
                delayMillis = 0
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Active",
                value = stats.activeWorkflows.toString(),
                icon = Icons.Filled.PlayCircle,
                color = StatusSuccess,
                delayMillis = 100
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Success",
                value = stats.successfulExecutions.toString(),
                icon = Icons.Outlined.CheckCircle,
                color = StatusSuccess,
                delayMillis = 200
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Failures",
                value = stats.failedExecutions.toString(),
                icon = Icons.Outlined.Error,
                color = StatusError,
                delayMillis = 300
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    delayMillis: Int
) {
    var animateIn by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        animateIn = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "StatCardScale"
    )

    NeumorphicCard(
        modifier = modifier.scale(scale),
        cornerRadius = 20.dp,
        shadowOffset = 6.dp,
        shadowBlur = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentExecutionsSection(
    executions: List<Execution>,
    onSeeAllClick: () -> Unit,
    onExecutionClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Executions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "See all", 
                    color = N8nPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (executions.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "No executions yet",
                message = "When your workflows run, their execution history will appear here.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 16.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                executions.take(5).forEachIndexed { index, execution ->
                    var itemVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        delay(index * 100L)
                        itemVisible = true
                    }
                    
                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            initialOffsetY = { 30 }
                        )
                    ) {
                        ExecutionItem(
                            execution = execution,
                            onClick = { onExecutionClick(execution.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutionItem(
    execution: Execution,
    onClick: () -> Unit
) {
    val colors = neumorphicColors()
    
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 16.dp,
        shadowOffset = 5.dp,
        shadowBlur = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(execution.mode) {
                        ExecutionMode.WEBHOOK, ExecutionMode.TRIGGER -> Icons.Outlined.Bolt
                        ExecutionMode.MANUAL -> Icons.Outlined.PlayArrow
                        else -> Icons.Outlined.Settings
                    },
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = execution.workflowName ?: "Workflow ${execution.workflowId.take(5)}...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "#${execution.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(colors.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    
                    Text(
                        text = DateUtils.formatTime(execution.startedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    
                    val duration = DateUtils.calculateDuration(execution.startedAt, execution.stoppedAt)
                    if (duration.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(colors.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                        
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            ExecutionStatusChip(status = execution.status)
        }
    }
}