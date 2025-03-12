package com.example.tiktokvasp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktokvasp.screens.MainScreen
import com.example.tiktokvasp.screens.SwipeAnalyticsDebugScreen
import com.example.tiktokvasp.viewmodel.DebugViewModel
import com.example.tiktokvasp.viewmodel.MainViewModel

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Create and remember ViewModels
    val mainViewModel: MainViewModel = viewModel()
    val debugViewModel: DebugViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                onOpenDebugScreen = {
                    // Load the events from the main view model's tracker
                    debugViewModel.loadEventsFromTracker(mainViewModel.getBehaviorTracker())
                    navController.navigate("debug")
                }
            )
        }

        composable("debug") {
            SwipeAnalyticsDebugScreen(
                viewModel = debugViewModel,
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Routes used in the app's navigation
 */
object AppRoutes {
    const val MAIN = "main"
    const val DEBUG = "debug"
}