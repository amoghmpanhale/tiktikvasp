package com.example.tiktokvasp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktokvasp.adapters.VideoAdapter
import com.example.tiktokvasp.screens.LandingScreen
import com.example.tiktokvasp.screens.MainScreen
import com.example.tiktokvasp.screens.SwipeAnalyticsDebugScreen
import com.example.tiktokvasp.viewmodel.DebugViewModel
import com.example.tiktokvasp.viewmodel.LandingViewModel
import com.example.tiktokvasp.viewmodel.MainViewModel

/**
 * Main navigation component for the app
 */
@Composable
fun AppNavigation(
    onVideoAdapterCreated: (VideoAdapter) -> Unit = {}
) {
    val navController = rememberNavController()

    // Create and remember ViewModels
    val landingViewModel: LandingViewModel = viewModel()
    val mainViewModel: MainViewModel = viewModel()
    val debugViewModel: DebugViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.LANDING
    ) {
        composable(AppRoutes.LANDING) {
            LandingScreen(
                onStartSession = { participantId, folderName, durationMinutes, autoGeneratePngs ->
                    // Set participant and folder
                    mainViewModel.setParticipantId(participantId)
                    mainViewModel.loadVideosFromFolder(folderName)

                    // Start the timed session using the two new parameters
                    mainViewModel.startSession(durationMinutes, autoGeneratePngs)

                    // Navigate to the main screen
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.LANDING) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.MAIN) {
            MainScreen(
                viewModel = mainViewModel,
                onOpenDebugScreen = {
                    // Load the events from the main view model's tracker
                    debugViewModel.loadEventsFromTracker(mainViewModel.getBehaviorTracker())
                    navController.navigate(AppRoutes.DEBUG)
                },
                onVideoAdapterCreated = onVideoAdapterCreated
            )
        }

        composable(AppRoutes.DEBUG) {
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
    const val LANDING = "landing"
    const val MAIN = "main"
    const val DEBUG = "debug"
}