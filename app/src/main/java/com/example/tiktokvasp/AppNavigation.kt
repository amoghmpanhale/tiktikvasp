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
fun AppNavigation() {
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
            val videoFolders by landingViewModel.availableFolders.collectAsState()
            val participantId by landingViewModel.participantId.collectAsState()
            val selectedFolder by landingViewModel.selectedFolder.collectAsState()

            LandingScreen(
                onStartSession = { participantId, folderName ->
                    // Set the participant ID in the MainViewModel
                    mainViewModel.setParticipantId(participantId)

                    // Load videos from the selected folder
                    mainViewModel.loadVideosFromFolder(folderName)

                    // Navigate to the main screen
                    navController.navigate(AppRoutes.MAIN) {
                        // Clear the back stack so users can't go back to the landing page
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
                }
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