package com.n8n.mobilemanager.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.n8n.mobilemanager.ui.components.neumorphicShadow
import com.n8n.mobilemanager.data.repository.N8nRepository
import com.n8n.mobilemanager.ui.screens.credentials.CredentialsScreen
import com.n8n.mobilemanager.ui.screens.dashboard.DashboardScreen
import com.n8n.mobilemanager.ui.screens.executions.ExecutionsScreen
import com.n8n.mobilemanager.ui.screens.login.LoginScreen
import com.n8n.mobilemanager.ui.screens.settings.SettingsScreen
import com.n8n.mobilemanager.ui.screens.workflows.WorkflowsScreen
import com.n8n.mobilemanager.ui.theme.*

/**
 * Routes de navigation
 */
sealed class Screen(
    val route: String,
    val title: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector
) {
    object Login : Screen(
        route = "login",
        title = "Connexion",
        iconOutlined = Icons.Outlined.Login,
        iconFilled = Icons.Filled.Login
    )
    
    object Dashboard : Screen(
        route = "dashboard",
        title = "Tableau de bord",
        iconOutlined = Icons.Outlined.Dashboard,
        iconFilled = Icons.Filled.Dashboard
    )
    
    object Workflows : Screen(
        route = "workflows",
        title = "Workflows",
        iconOutlined = Icons.Outlined.AccountTree,
        iconFilled = Icons.Filled.AccountTree
    )
    
    object Executions : Screen(
        route = "executions",
        title = "Exécutions",
        iconOutlined = Icons.Outlined.History,
        iconFilled = Icons.Filled.History
    )
    
    object Credentials : Screen(
        route = "credentials",
        title = "Credentials",
        iconOutlined = Icons.Outlined.Key,
        iconFilled = Icons.Filled.Key
    )
    
    object Settings : Screen(
        route = "settings",
        title = "Paramètres",
        iconOutlined = Icons.Outlined.Settings,
        iconFilled = Icons.Filled.Settings
    )
    
    // Detail screens
    object WorkflowDetail : Screen(
        route = "workflow/{workflowId}",
        title = "Détail workflow",
        iconOutlined = Icons.Outlined.AccountTree,
        iconFilled = Icons.Filled.AccountTree
    ) {
        fun createRoute(workflowId: String) = "workflow/$workflowId"
    }
    
    object ExecutionDetail : Screen(
        route = "execution/{executionId}",
        title = "Détail exécution",
        iconOutlined = Icons.Outlined.History,
        iconFilled = Icons.Filled.History
    ) {
        fun createRoute(executionId: String) = "execution/$executionId"
    }
    
    object CredentialDetail : Screen(
        route = "credential/{credentialId}",
        title = "Détail credential",
        iconOutlined = Icons.Outlined.Key,
        iconFilled = Icons.Filled.Key
    ) {
        fun createRoute(credentialId: String) = "credential/$credentialId"
    }
}

/**
 * Écrans affichés dans la barre de navigation
 */
val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Workflows,
    Screen.Executions,
    Screen.Credentials
)

/**
 * Composable principal de navigation
 */
@Composable
fun N8nNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if bottom nav should be visible
    val showBottomNav = bottomNavScreens.any { it.route == currentRoute }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomNav) {
                NeumorphicBottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + 
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + 
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + 
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + 
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            }
        ) {
            // Login
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            
            // Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToWorkflows = {
                        navController.navigate(Screen.Workflows.route)
                    },
                    onNavigateToExecutions = {
                        navController.navigate(Screen.Executions.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToExecution = { executionId ->
                        navController.navigate(Screen.ExecutionDetail.createRoute(executionId))
                    }
                )
            }
            
            // Workflows
            composable(Screen.Workflows.route) {
                WorkflowsScreen(
                    onNavigateToWorkflow = { workflowId ->
                        navController.navigate(Screen.WorkflowDetail.createRoute(workflowId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Workflow Detail
            composable(Screen.WorkflowDetail.route) { backStackEntry ->
                val workflowId = backStackEntry.arguments?.getString("workflowId") ?: ""
                // TODO: WorkflowDetailScreen
                WorkflowsScreen(
                    onNavigateToWorkflow = {},
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Executions
            composable(Screen.Executions.route) {
                ExecutionsScreen(
                    onNavigateToExecution = { executionId ->
                        navController.navigate(Screen.ExecutionDetail.createRoute(executionId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Execution Detail
            composable(Screen.ExecutionDetail.route) { backStackEntry ->
                val executionId = backStackEntry.arguments?.getString("executionId") ?: ""
                // TODO: ExecutionDetailScreen
                ExecutionsScreen(
                    onNavigateToExecution = {},
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Credentials
            composable(Screen.Credentials.route) {
                CredentialsScreen(
                    onNavigateToCredential = { credentialId ->
                        navController.navigate(Screen.CredentialDetail.createRoute(credentialId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Credential Detail
            composable(Screen.CredentialDetail.route) { backStackEntry ->
                val credentialId = backStackEntry.arguments?.getString("credentialId") ?: ""
                // TODO: CredentialDetailScreen
                CredentialsScreen(
                    onNavigateToCredential = {},
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            // Settings
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun NeumorphicBottomNavBar(
    navController: NavController,
    currentRoute: String?
) {
    val neumorphColors = neumorphicColors()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicShadow(
                    lightShadowColor = neumorphColors.lightShadow,
                    darkShadowColor = neumorphColors.darkShadow,
                    shadowOffset = 6.dp,
                    shadowRadius = 12.dp,
                    cornerRadius = 28.dp
                )
                .clip(RoundedCornerShape(28.dp))
                .background(neumorphColors.surface)
                .padding(vertical = 8.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavScreens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    
                    NeumorphicNavItem(
                        screen = screen,
                        isSelected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeumorphicNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(
                                N8nPrimary.copy(alpha = 0.15f),
                                N8nPrimary.copy(alpha = 0.25f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) screen.iconFilled else screen.iconOutlined,
                contentDescription = screen.title,
                tint = if (isSelected) N8nPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = screen.title.split(" ").first(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = N8nPrimary
            )
        }
    }
}
