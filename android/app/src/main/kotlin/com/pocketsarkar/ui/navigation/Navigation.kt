package com.pocketsarkar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketsarkar.ui.screens.HomeScreen
import com.pocketsarkar.ui.screens.DecoderScreen
import com.pocketsarkar.ui.screens.SchemesScreen
import com.pocketsarkar.ui.screens.RadarScreen
import com.pocketsarkar.ui.screens.RightsScreen

/**
 * All navigation destinations in one place.
 * Add new routes here — never hardcode strings elsewhere.
 */
sealed class Screen(val route: String) {
    data object Home    : Screen("home")
    data object Decoder : Screen("decoder")      // Document Decoder
    data object Schemes : Screen("schemes")      // Scheme Explainer
    data object Radar   : Screen("radar")        // Opportunity Radar
    data object Rights  : Screen("rights")       // Rights Companion
    // FormPilot will be added in Phase 7
}

@Composable
fun PocketSarkarNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { screen -> navController.navigate(screen.route) }
            )
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
    }
}
