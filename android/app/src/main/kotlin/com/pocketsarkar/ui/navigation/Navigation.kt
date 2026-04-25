package com.pocketsarkar.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketsarkar.ui.screens.*
import com.pocketsarkar.ui.theme.*
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String = "", val icon: ImageVector? = null) {
    data object Onboarding : Screen("onboarding")
    data object ModelSetup : Screen("model_setup")
    data object Home    : Screen("home", "Home", Icons.Default.Home)
    data object Decoder : Screen("decoder", "Scanner", Icons.Default.CameraAlt)
    data object Schemes : Screen("schemes", "Benefits", Icons.Default.Star)
    data object Rights  : Screen("rights", "Rights", Icons.Default.Shield)
    data object Profile : Screen("profile", "Account", Icons.Default.Person)
    
    // Side Panel Screens
    data object Tracker : Screen("tracker", "My Applications", Icons.Default.List)
    data object Vault   : Screen("vault", "Document Vault", Icons.Default.Lock)
    data object Offices : Screen("offices", "Local Offices", Icons.Default.LocationOn)
    data object Guide   : Screen("guide/{schemeName}", "How to Apply", Icons.Default.Info)
    data object Available : Screen("available", "Available Schemes", Icons.Default.Apps)
    data object CategoryDetail : Screen("category_detail/{name}", "Category Detail")
}

val navItems = listOf(
    Screen.Home,
    Screen.Decoder,
    Screen.Schemes,
    Screen.Rights,
    Screen.Profile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocketSarkarNavHost(
    userPrefs: com.pocketsarkar.data.UserPreferences,
    navController: NavHostController = rememberNavController()
) {
    val strings = Localization.getStrings(userPrefs.userLanguage)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val startDest = Screen.Onboarding.route 

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PSNavy,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxHeight().padding(24.dp)) {
                    Text("Pocket Sarkar", color = PSSaffron, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Digital Services", color = PSWhite.copy(alpha = 0.5f), style = MaterialTheme.typography.labelMedium)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    DrawerItem(Screen.Available.label, Screen.Available.icon!!) {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Available.route)
                    }
                    DrawerItem(Screen.Tracker.label, Screen.Tracker.icon!!) {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Tracker.route)
                    }
                    DrawerItem("Application Guide", Screen.Guide.icon!!) {
                        scope.launch { drawerState.close() }
                        navController.navigate("guide/General Scheme")
                    }
                    DrawerItem(Screen.Vault.label, Screen.Vault.icon!!) {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    }
                    DrawerItem(Screen.Offices.label, Screen.Offices.icon!!) {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Offices.route)
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val showBottomBar = currentDestination?.route?.startsWith("onboarding") == false && 
                                   currentDestination?.route?.startsWith("model_setup") == false

                if (showBottomBar && currentDestination?.route in navItems.map { it.route }) {
                    NavigationBar(containerColor = PSNavy, contentColor = PSWhite) {
                        navItems.forEach { screen ->
                            val label = when(screen.route) {
                                Screen.Home.route -> strings.navHome
                                Screen.Decoder.route -> strings.navDecoder
                                Screen.Schemes.route -> strings.navSchemes
                                Screen.Rights.route -> strings.navRights
                                Screen.Profile.route -> strings.navProfile
                                else -> screen.label
                            }
                            NavigationBarItem(
                                icon = { Icon(screen.icon!!, null) },
                                label = { Text(label) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PSNavy, selectedTextColor = PSSaffron,
                                    unselectedIconColor = PSWhite.copy(alpha = 0.6f), unselectedTextColor = PSWhite.copy(alpha = 0.6f),
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
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(500)) + fadeIn(tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(500)) + fadeOut(tween(500)) }
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(userPrefs = userPrefs, onComplete = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } } })
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        userPrefs = userPrefs,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Screen.Decoder.route) {
                    DecoderScreen(userPrefs = userPrefs, onBack = { navController.popBackStack() })
                }
                composable(Screen.Schemes.route) {
                    SchemeExplainerScreen(userPrefs = userPrefs, onBack = { navController.popBackStack() })
                }
                composable(Screen.Rights.route) {
                    RightsScreen(userPrefs = userPrefs, onBack = { navController.popBackStack() })
                }
                composable(Screen.Profile.route) {
                    Surface(modifier = Modifier.fillMaxSize(), color = PSCream) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Settings & Profile", style = MaterialTheme.typography.headlineMedium, color = PSNavy)
                            Text("Coming Soon in Phase 4", style = MaterialTheme.typography.bodyLarge, color = PSTextSecondary)
                        }
                    }
                }
                // SIDE PANEL SCREENS
                composable(Screen.Available.route) {
                    BenefitsScreen(userPrefs.userLanguage, onCategoryClick = { name -> navController.navigate("category_detail/$name") })
                }
                composable(Screen.CategoryDetail.route) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    CategoryDetailScreen(
                        categoryName = name,
                        userLanguage = userPrefs.userLanguage,
                        onBack = { navController.popBackStack() },
                        onNavigateToGuide = { schemeName -> navController.navigate("guide/$schemeName") }
                    )
                }
                composable(Screen.Tracker.route) {
                    TrackerScreen(userPrefs.userLanguage)
                }
                composable(
                    Screen.Guide.route,
                    arguments = listOf(navArgument("schemeName") { defaultValue = "General Scheme" })
                ) { backStackEntry ->
                    val schemeName = backStackEntry.arguments?.getString("schemeName") ?: "General Scheme"
                    GuideScreen(schemeName, userPrefs.userLanguage, onBack = { navController.popBackStack() })
                }
                composable(Screen.Offices.route) {
                    OfficesScreen(userPrefs.userLanguage)
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).padding(horizontal = 16.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PSSaffron, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = PSWhite, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}