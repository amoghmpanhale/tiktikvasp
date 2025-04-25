package com.example.tiktokvasp.screens

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.viewpager2.widget.ViewPager2
import com.example.tiktokvasp.adapters.VideoAdapter
import com.example.tiktokvasp.components.SwipeAnalyticsOverlay
import com.example.tiktokvasp.components.TikTokBottomBar
import com.example.tiktokvasp.components.TikTokTopBar
import com.example.tiktokvasp.tracking.SwipeAnalyticsService
import com.example.tiktokvasp.tracking.SwipeDirection
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.util.TikTokSwipeDetector
import com.example.tiktokvasp.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onOpenDebugScreen: () -> Unit = {}
) {
    val videos by viewModel.videos.collectAsState()
    val currentVideoIndex by viewModel.currentVideoIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val analyticsService = remember { SwipeAnalyticsService() }

    // State for ViewPager2 reference
    var viewPager by remember { mutableStateOf<ViewPager2?>(null) }
    var viewPagerContainer by remember { mutableStateOf<View?>(null) }

    // State for the last detected swipe and analytics
    var lastSwipeEvent by remember { mutableStateOf<SwipeEvent?>(null) }
    var showDebugInfo by remember { mutableStateOf(false) }

    // Create the adapter when videos are loaded
    val videoAdapter = remember(videos) {
        VideoAdapter(context, videos, viewModel)
    }

    // Create enhanced swipe detector
    val swipeDetector = remember {
        TikTokSwipeDetector(context).apply {
            setOnSwipeListener(object : TikTokSwipeDetector.OnSwipeListener {
                override fun onSwipeUp() {
                    viewModel.onSwipeUp()
                }

                override fun onSwipeDown() {
                    viewModel.onSwipeDown()
                }

                override fun onSwipeLeft() {
                    // Optional - handle horizontal swipes if needed
                }

                override fun onSwipeRight() {
                    // Optional - handle horizontal swipes if needed
                }

                override fun onSingleTap() {
                    viewModel.toggleVideoPlayback()
                }

                override fun onDoubleTap() {
                    viewModel.likeCurrentVideo()
                }

                override fun onLongPress() {
                    // Toggle debug info display
                    showDebugInfo = !showDebugInfo
                }

                override fun onDetailedSwipeDetected(swipeEvent: SwipeEvent) {
                    // Record the swipe event in the view model
                    viewModel.trackDetailedSwipe(swipeEvent)

                    // Update the last swipe event for visualization
                    lastSwipeEvent = swipeEvent
                }
            })
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            videos.isEmpty() -> {
                Text(
                    text = "No videos found. Please add videos to your device.",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            else -> {
                // ViewPager2 implementation with vertical swiping
                AndroidView(
                    factory = { context ->
                        ViewPager2(context).apply {
                            adapter = videoAdapter
                            orientation = ViewPager2.ORIENTATION_VERTICAL

                            // Register callback for page changes
                            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                                override fun onPageSelected(position: Int) {
                                    super.onPageSelected(position)
                                    viewModel.onPageSelected(position)
                                }
                            })

                            // Set current item to match the view model state
                            setCurrentItem(currentVideoIndex, false)

                            // Save references
                            viewPager = this
                            viewPagerContainer = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { pager ->
                        // Update the adapter if videos change
                        (pager.adapter as? VideoAdapter)?.updateVideos(videos)

                        // Update current position if it changes in the viewModel
                        if (pager.currentItem != currentVideoIndex) {
                            pager.setCurrentItem(currentVideoIndex, true)
                        }
                    }
                )

                // Apply enhanced swipe detector to the ViewPager
                DisposableEffect(viewPagerContainer) {
                    val container = viewPagerContainer
                    if (container != null) {
                        container.setOnTouchListener(swipeDetector)
                    }

                    onDispose {
                        container?.setOnTouchListener(null)
                        swipeDetector.unregisterSensors()
                    }
                }

                // Add the TikTok top bar
                TikTokTopBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }
        }

        // Bottom navigation bar
        TikTokBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // Debug button
        FloatingActionButton(
            onClick = onOpenDebugScreen,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0x99000000),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Open Debug"
            )
        }

        // Show swipe analytics overlay if debug info is enabled
        if (showDebugInfo && lastSwipeEvent != null) {
            val swipeAnalytics = lastSwipeEvent?.let { analyticsService.analyzeSwipe(it) }
            SwipeAnalyticsOverlay(
                swipeEvent = lastSwipeEvent,
                swipeAnalytics = swipeAnalytics,
                showDebugInfo = showDebugInfo
            )
        }
    }
}