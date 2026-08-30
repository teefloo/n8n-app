package com.n8n.mobilemanager.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.n8n.mobilemanager.ui.screens.credentials.CredentialDetailScreen
import com.n8n.mobilemanager.ui.screens.credentials.CredentialsScreen
import com.n8n.mobilemanager.ui.screens.dashboard.DashboardScreen
import com.n8n.mobilemanager.ui.screens.executions.ExecutionDetailScreen
import com.n8n.mobilemanager.ui.screens.executions.ExecutionsScreen
import com.n8n.mobilemanager.ui.screens.login.LoginScreen
import com.n8n.mobilemanager.ui.screens.settings.SettingsScreen
import com.n8n.mobilemanager.ui.screens.workflows.WorkflowDetailScreen
import com.n8n.mobilemanager.ui.screens.workflows.WorkflowsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val iconOutlined: androidx.compose.ui.graphics.vector.ImageVector,
    val iconFilled: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Login : Screen("login", "Login", Icons.AutoMirrored.Outlined.Login, Icons.AutoMirrored.Filled.Login)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    object Workflows : Screen("workflows", "Workflows", Icons.Outlined.AccountTree, Icons.Filled.AccountTree)
    object Executions : Screen("executions", "Executions", Icons.Outlined.History, Icons.Filled.History)
    object Credentials : Screen("credentials", "Credentials", Icons.Outlined.Key, Icons.Filled.Key)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)

    object WorkflowDetail : Screen("workflow/{workflowId}", "Workflow detail", Icons.Outlined.AccountTree, Icons.Filled.AccountTree) {
        fun createRoute(workflowId: String) = "workflow/$workflowId"
    }

    object ExecutionDetail : Screen("execution/{executionId}", "Execution detail", Icons.Outlined.History, Icons.Filled.History) {
        fun createRoute(executionId: String) = "execution/$executionId"
    }

    object CredentialDetail : Screen("credential/{credentialId}", "Credential detail", Icons.Outlined.Key, Icons.Filled.Key) {
        fun createRoute(credentialId: String) = "credential/$credentialId"
    }
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Workflows,
    Screen.Executions,
    Screen.Credentials
)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun N8nNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showPrimaryNavigation = bottomNavScreens.any { it.route == currentRoute }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = showPrimaryNavigation && maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                AppNavigationRail(navController = navController, currentRoute = currentRoute)
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showPrimaryNavigation && !useRail) {
                        AppNavigationBar(navController = navController, currentRoute = currentRoute)
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Login.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    enterTransition = { fadeIn(tween(180)) },
                    exitTransition = { fadeOut(tween(120)) },
                    popEnterTransition = { fadeIn(tween(180)) },
                    popExitTransition = { fadeOut(tween(120)) }
                ) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            onNavigateToWorkflows = { navController.navigate(Screen.Workflows.route) },
                            onNavigateToExecutions = { navController.navigate(Screen.Executions.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToExecution = { id ->
                                navController.navigate(Screen.ExecutionDetail.createRoute(id))
                            }
                        )
                    }

                    composable(Screen.Workflows.route) {
                        WorkflowsScreen(
                            onNavigateToWorkflow = { id ->
                                navController.navigate(Screen.WorkflowDetail.createRoute(id))
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.WorkflowDetail.route,
                        arguments = listOf(navArgument("workflowId") { type = NavType.StringType })
                    ) {
                        WorkflowDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExecution = { id ->
                                navController.navigate(Screen.ExecutionDetail.createRoute(id))
                            }
                        )
                    }

                    composable(Screen.Executions.route) {
                        ExecutionsScreen(
                            onNavigateToExecution = { id ->
                                navController.navigate(Screen.ExecutionDetail.createRoute(id))
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.ExecutionDetail.route,
                        arguments = listOf(navArgument("executionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        ExecutionDetailScreen(
                            executionId = backStackEntry.arguments?.getString("executionId").orEmpty(),
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToExecution = { id ->
                                navController.navigate(Screen.ExecutionDetail.createRoute(id))
                            }
                        )
                    }

                    composable(Screen.Credentials.route) {
                        CredentialsScreen(
                            onNavigateToCredential = { id ->
                                navController.navigate(Screen.CredentialDetail.createRoute(id))
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.CredentialDetail.route,
                        arguments = listOf(navArgument("credentialId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        CredentialDetailScreen(
                            credentialId = backStackEntry.arguments?.getString("credentialId").orEmpty(),
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNoActiveInstance = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavigationBar(
    navController: NavController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        bottomNavScreens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { navigateToRoot(navController, screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                        contentDescription = null
                    )
                },
                label = { Text(screen.title) }
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    navController: NavController,
    currentRoute: String?
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.AccountTree,
                contentDescription = "n8n Mobile Manager",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        bottomNavScreens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationRailItem(
                selected = selected,
                onClick = { navigateToRoot(navController, screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                        contentDescription = null
                    )
                },
                label = { Text(screen.title) }
            )
        }
    }
}

private fun navigateToRoot(navController: NavController, screen: Screen) {
    navController.navigate(screen.route) {
        popUpTo(Screen.Dashboard.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
