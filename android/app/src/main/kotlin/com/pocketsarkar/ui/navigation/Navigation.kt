package com.pocketsarkar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketsarkar.ui.screens.*

sealed class Screen(val route: String) {
    data object ModelSetup : Screen("model_setup") // Phase 5 — first-launch gate
    data object Home    : Screen("home")
    data object Decoder : Screen("decoder")
    data object Schemes : Screen("schemes")
    data object Radar   : Screen("radar")
    data object Rights  : Screen("rights")
    data object TestAi  : Screen("test_ai")
}

@Composable
fun PocketSarkarNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ModelSetup.route
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
        composable(Screen.Radar.route) {
            RadarScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Rights.route) {
            RightsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TestAi.route) {
            TestQueryScreen(onBack = { navController.popBackStack() })
        }
    }
}