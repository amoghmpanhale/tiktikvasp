package com.example.tiktokvasp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.tiktokvasp.adapters.VideoAdapter
import com.example.tiktokvasp.screens.MainScreen
import com.example.tiktokvasp.ui.theme.TiktokvaspTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Store reference to any active VideoAdapter for lifecycle management
    private var activeVideoAdapter: VideoAdapter? = null

    // Lifecycle observer to handle ExoPlayer lifecycle properly
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                Log.d("MainActivity", "Lifecycle: ON_PAUSE - pausing all video playback")
                // Stop all playback when the app is paused
                activeVideoAdapter?.let { adapter ->
                    // Release all players when app goes to background
                    adapter.releaseAllPlayers()
                }
            }
            Lifecycle.Event.ON_STOP -> {
                Log.d("MainActivity", "Lifecycle: ON_STOP - releasing all video resources")
                // Make sure all players are released when app is stopped
                activeVideoAdapter?.let { adapter ->
                    adapter.releaseAllPlayers()
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                Log.d("MainActivity", "Lifecycle: ON_DESTROY - final cleanup")
                // Final cleanup
                activeVideoAdapter?.releaseAllPlayers()
                activeVideoAdapter = null
            }
            else -> {
                // No action needed for other lifecycle events
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Log.d("MainActivity", "Manage External Storage permission granted!")
                // Proceed with accessing external storage
            } else {
                Log.e("MainActivity", "Permission denied")
            }
        }

        // Ensure no stale players are running from previous sessions
        activeVideoAdapter?.let { adapter ->
            Log.d("MainActivity", "onResume: refreshing adapter state")
            // Give a small delay to let the UI settle before handling players
            lifecycleScope.launch {
                delay(500)
                // The current player will be reinitialized when ViewPager rebuilds
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            // Permissions granted, we can now load videos
            showMainScreen()
        } else {
            // Handle permission denied
            // In a real app, you would show a dialog explaining why permission is needed
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register lifecycle observer
        lifecycle.addObserver(lifecycleObserver)

        // Check for storage permission
        if (hasStoragePermission()) {
            showApp()
        } else {
            requestStoragePermission()
        }
    }

    override fun onDestroy() {
        // Important: Remove lifecycle observer to avoid leaks
        lifecycle.removeObserver(lifecycleObserver)

        // Final cleanup
        activeVideoAdapter?.releaseAllPlayers()
        activeVideoAdapter = null

        super.onDestroy()
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissions)
    }

    private fun showApp() {
        setContent {
            TiktokvaspTheme {
                // Set up transparent system bars with proper insets handling
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()

                DisposableEffect(systemUiController, useDarkIcons) {
                    // Make status bar fully transparent and ensure content goes behind it
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = false
                    )

                    // Set navigation bar to be black
                    systemUiController.setNavigationBarColor(
                        color = Color.Black,
                        darkIcons = false
                    )

                    // Hide the system bars for true edge-to-edge
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent
                    )

                    onDispose {}
                }

                // Use AppNavigation composable
                AppNavigation(
                    onVideoAdapterCreated = { adapter ->
                        activeVideoAdapter = adapter
                        Log.d("MainActivity", "VideoAdapter reference captured")
                    }
                )
            }
        }
    }

    private fun showMainScreen() {
        setContent {
            TiktokvaspTheme {
                // Set up transparent status bar
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = !isSystemInDarkTheme()

                DisposableEffect(systemUiController, useDarkIcons) {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = false
                    )

                    systemUiController.setNavigationBarColor(
                        color = Color.Black,
                        darkIcons = false
                    )

                    onDispose {}
                }

                // Use AppNavigation instead of directly showing MainScreen
                AppNavigation(
                    onVideoAdapterCreated = { adapter ->
                        // Store reference to the active adapter for lifecycle management
                        activeVideoAdapter = adapter
                        Log.d("MainActivity", "VideoAdapter reference captured")
                    }
                )
            }
        }
    }
}