package com.pocketsarkar.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.pocketsarkar.ui.screens.*
import com.pocketsarkar.ui.theme.*

sealed class Screen(val route: String, val label: String = "", val icon: ImageVector? = null) {
    data object ModelSetup : Screen("model_setup")
    data object Home    : Screen("home", "Home", Icons.Default.Home)
    data object Decoder : Screen("decoder", "Decoder", Icons.Default.CameraAlt)
    data object Schemes : Screen("schemes", "Schemes", Icons.Default.Star)
    data object Rights  : Screen("rights", "Rights", Icons.Default.Shield)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
    
    // Sub-screens
    data object TestAi  : Screen("test_ai")
}

val navItems = listOf(
    Screen.Home,
    Screen.Decoder,
    Screen.Schemes,
    Screen.Rights,
    Screen.Profile
)

@Composable
fun PocketSarkarNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBottomBar = currentDestination?.route in navItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = PSNavy,
                    contentColor = PSWhite,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PSNavy,
                                selectedTextColor = PSSaffron,
                                unselectedIconColor = PSWhite.copy(alpha = 0.6f),
                                unselectedTextColor = PSWhite.copy(alpha = 0.6f),
                                indicatorColor = PSSaffron
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ModelSetup.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.ModelSetup.route) {
                ModelSetupScreen(
                    onModelReady = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.ModelSetup.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(onNavigate = { screen -> navController.navigate(screen.route) })
            }
            composable(Screen.Decoder.route) {
                DecoderScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Schemes.route) {
                SchemesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Rights.route) {
                RightsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Profile.route) {
                PlaceholderProfileScreen()
            }
            composable(Screen.TestAi.route) {
                TestQueryScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun PlaceholderProfileScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = PSCream) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text("Settings & Profile", style = MaterialTheme.typography.headlineMedium, color = PSNavy)
            Text("Coming Soon in Phase 4", style = MaterialTheme.typography.bodyLarge, color = PSTextSecondary)
        }
    }
}