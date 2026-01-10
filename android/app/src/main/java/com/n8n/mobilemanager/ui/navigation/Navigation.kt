package com.n8n.mobilemanager.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
import com.n8n.mobilemanager.ui.screens.executions.ExecutionDetailScreen
import com.n8n.mobilemanager.ui.screens.login.LoginScreen
import com.n8n.mobilemanager.ui.screens.settings.SettingsScreen
import com.n8n.mobilemanager.ui.screens.workflows.WorkflowDetailScreen
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
                // Déterminer la direction en fonction de l'ordre des écrans
                val fromIndex = bottomNavScreens.indexOfFirst { it.route == initialState.destination.route }
                val toIndex = bottomNavScreens.indexOfFirst { it.route == targetState.destination.route }
                val direction = if (fromIndex != -1 && toIndex != -1 && toIndex < fromIndex) {
                    AnimatedContentTransitionScope.SlideDirection.End // Aller vers la gauche = slide depuis la droite
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start // Aller vers la droite = slide depuis la gauche
                }
                fadeIn(animationSpec = tween(300)) + 
                slideIntoContainer(direction, tween(300))
            },
            exitTransition = {
                val fromIndex = bottomNavScreens.indexOfFirst { it.route == initialState.destination.route }
                val toIndex = bottomNavScreens.indexOfFirst { it.route == targetState.destination.route }
                val direction = if (fromIndex != -1 && toIndex != -1 && toIndex < fromIndex) {
                    AnimatedContentTransitionScope.SlideDirection.End
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Start
                }
                fadeOut(animationSpec = tween(300)) + 
                slideOutOfContainer(direction, tween(300))
            },
            popEnterTransition = {
                val fromIndex = bottomNavScreens.indexOfFirst { it.route == initialState.destination.route }
                val toIndex = bottomNavScreens.indexOfFirst { it.route == targetState.destination.route }
                val direction = if (fromIndex != -1 && toIndex != -1 && toIndex > fromIndex) {
                    AnimatedContentTransitionScope.SlideDirection.Start
                } else {
                    AnimatedContentTransitionScope.SlideDirection.End
                }
                fadeIn(animationSpec = tween(300)) + 
                slideIntoContainer(direction, tween(300))
            },
            popExitTransition = {
                val fromIndex = bottomNavScreens.indexOfFirst { it.route == initialState.destination.route }
                val toIndex = bottomNavScreens.indexOfFirst { it.route == targetState.destination.route }
                val direction = if (fromIndex != -1 && toIndex != -1 && toIndex > fromIndex) {
                    AnimatedContentTransitionScope.SlideDirection.Start
                } else {
                    AnimatedContentTransitionScope.SlideDirection.End
                }
                fadeOut(animationSpec = tween(300)) + 
                slideOutOfContainer(direction, tween(300))
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
                WorkflowDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExecution = { executionId ->
                        navController.navigate(Screen.ExecutionDetail.createRoute(executionId))
                    }
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
            composable(Screen.ExecutionDetail.route) {
                ExecutionDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToExecution = { executionId ->
                        navController.navigate(Screen.ExecutionDetail.createRoute(executionId))
                    }
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
    val selectedIndex = bottomNavScreens.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    
    // Animation fluide de l'indicateur
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 150f
        ),
        label = "indicator_position"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Barre flottante avec ombre neumorphique
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .neumorphicShadow(
                    lightShadowColor = neumorphColors.lightShadow,
                    darkShadowColor = neumorphColors.darkShadow,
                    shadowOffset = 8.dp,
                    shadowRadius = 16.dp,
                    cornerRadius = 28.dp
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            neumorphColors.surface,
                            neumorphColors.surface.copy(alpha = 0.98f)
                        )
                    )
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                val itemWidth = maxWidth / bottomNavScreens.size
                
                // Indicateur glissant avec dégradé
                Box(
                    modifier = Modifier
                        .offset(x = itemWidth * animatedIndex)
                        .width(itemWidth)
                        .fillMaxHeight()
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        N8nPrimary.copy(alpha = 0.15f),
                                        N8nPrimaryLight.copy(alpha = 0.08f)
                                    )
                                )
                            )
                    )
                }

                // Items de navigation
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavScreens.forEachIndexed { index, screen ->
                        val isSelected = index == selectedIndex
                        
                        // Animations pour chaque item
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.1f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 300f
                            ),
                            label = "icon_scale_$index"
                        )
                        
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) N8nPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            animationSpec = tween(250),
                            label = "icon_color_$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (!isSelected) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSelected) screen.iconFilled else screen.iconOutlined,
                                    contentDescription = screen.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                // Label animé qui apparaît quand sélectionné
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                                ) {
                                    Text(
                                        text = getShortLabel(screen),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = N8nPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Labels courts pour la barre de navigation
private fun getShortLabel(screen: Screen): String {
    return when (screen) {
        Screen.Dashboard -> "Home"
        Screen.Workflows -> "Flows"
        Screen.Executions -> "Runs"
        Screen.Credentials -> "Keys"
        else -> screen.title.take(5)
    }
}
