package com.example.tiktokvasp.screens

import android.util.Log
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.viewpager2.widget.ViewPager2
import com.example.tiktokvasp.adapters.VideoAdapter
import com.example.tiktokvasp.components.RandomStopOverlay
import com.example.tiktokvasp.components.SwipeAnalyticsOverlay
import com.example.tiktokvasp.components.TikTokBottomBar
import com.example.tiktokvasp.components.TikTokTopBar
import com.example.tiktokvasp.tracking.SwipeAnalyticsService
import com.example.tiktokvasp.tracking.SwipeDirection
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.util.TikTokSwipeDetector
import com.example.tiktokvasp.viewmodel.MainViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onOpenDebugScreen: () -> Unit = {},
    onVideoAdapterCreated: (VideoAdapter) -> Unit = {}
) {
    val videos by viewModel.videos.collectAsState()
    val currentVideoIndex by viewModel.currentVideoIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val sessionTimeRemaining by viewModel.sessionTimeRemaining.collectAsState()
    val exportStatus by viewModel.exportStatus.collectAsState()
    val context = LocalContext.current
    val analyticsService = remember { SwipeAnalyticsService() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Get state for random stops
    val isRandomStopActive by viewModel.isRandomStopActive.collectAsState()

    // State for ViewPager2 reference
    var viewPager by remember { mutableStateOf<ViewPager2?>(null) }
    var viewPagerContainer by remember { mutableStateOf<View?>(null) }

    // State for the last detected swipe and analytics
    var lastSwipeEvent by remember { mutableStateOf<SwipeEvent?>(null) }
    var showDebugInfo by remember { mutableStateOf(false) }

    // Create the adapter when videos are loaded and pass it back via the callback
    val videoAdapter = remember(videos) {
        VideoAdapter(context, videos, viewModel).also { adapter ->
            // Notify when adapter is created
            onVideoAdapterCreated(adapter)
        }
    }

    val isPlaying by viewModel.isPlaying.collectAsState()

    // Ensure player cleanup when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            // Cleanup all players when the screen is disposed
            videoAdapter.releaseAllPlayers()
        }
    }

    LaunchedEffect(isPlaying) {
        videoAdapter.updatePlaybackState(isPlaying)
    }

    // Create enhanced swipe detector
    val swipeDetector = remember {
        TikTokSwipeDetector(context).apply {
            setOnSwipeListener(object : TikTokSwipeDetector.OnSwipeListener {
                // ← swallow the simple up/down gestures so ViewPager2 handles them:
                override fun onSwipeUp()    { /* no-op */ }
                override fun onSwipeDown()  { /* no-op */ }
                override fun onSwipeLeft()  { /* no-op */ }
                override fun onSwipeRight() { /* no-op */ }

                override fun onSingleTap()     { viewModel.toggleVideoPlayback() }
                override fun onDoubleTap() {
                    viewModel.currentVideoIndex.value.let { index ->
                        if (videos.isNotEmpty() && index < videos.size) {
                            viewModel.likeVideo(videos[index].id)
                        }
                    }
                }
                override fun onLongPress()     { /* … */ }

                // still record detailed swipes for analytics
                override fun onDetailedSwipeDetected(swipeEvent: SwipeEvent) {
                    Log.d("MainScreen","🎉 detailed swipe detected! $swipeEvent")
                    viewModel.trackDetailedSwipe(swipeEvent)
                    lastSwipeEvent = swipeEvent

                    // Check for looping when swiping up at the end of videos
                    if (swipeEvent.direction == SwipeDirection.UP &&
                        currentVideoIndex >= videos.size - 1) {
                        viewModel.checkAndHandleLooping()
                    }
                }
            })
        }
    }

    LaunchedEffect(videos, currentVideoIndex) {
        if (videos.isNotEmpty()) {
            swipeDetector.setCurrentVideoId(videos[currentVideoIndex].id)
        }
    }

    // Show export status in snackbar
    LaunchedEffect(exportStatus) {
        if (exportStatus.isNotEmpty()) {
            snackbarHostState.showSnackbar(exportStatus)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .statusBarsPadding()
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
                        factory = { ctx ->
                            ViewPager2(ctx).apply {
                                viewPager = this
                                adapter = videoAdapter
                                orientation = ViewPager2.ORIENTATION_VERTICAL

                                registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(pos: Int) {
                                        super.onPageSelected(pos)
                                        Log.d("MainScreen", "Page selected: $pos")
                                        viewModel.onPageSelected(pos)
                                        swipeDetector.setCurrentVideoId(videos[pos].id)
                                        videoAdapter.updateCurrentPosition(pos)
                                    }
                                })

                                // ensure the internal RecyclerView exists, then hook the detector
                                post {
                                    // The first (and only) child of ViewPager2 is its RecyclerView
                                    (getChildAt(0) as? RecyclerView)?.let { rv ->
                                        Log.d("MainScreen","📌 attaching swipeDetector to RecyclerView in factory → $rv")
                                        rv.setOnTouchListener { v, ev ->
                                            swipeDetector.onTouch(v, ev)
                                            // return false so RecyclerView/ViewPager2 still handles scrolling
                                            false
                                        }
                                    }
                                }

                                setCurrentItem(currentVideoIndex, false)
                            }
                        },
                        update = { pager ->
                            viewPager = pager
                            (pager.adapter as? VideoAdapter)?.updateVideos(videos)
                            if (pager.currentItem != currentVideoIndex) {
                                pager.setCurrentItem(currentVideoIndex, true)
                            }
                        },
                        modifier = Modifier
                            .pointerInteropFilter { motionEvent ->
                                // send every event into your detector
                                swipeDetector.onTouch(viewPager, motionEvent)
                                // return false so ViewPager2 still scrolls normally
                                false
                            }
                            .fillMaxSize()
                    )

                    // Add the TikTok top bar
                    TikTokTopBar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                    )

//                    // Session timer overlay
//                    if (isSessionActive) {
//                        Box(
//                            modifier = Modifier
//                                .align(Alignment.TopEnd)
//                                .padding(top = 64.dp, end = 16.dp)  // Adjust this line
//                                .background(Color(0x80000000), RoundedCornerShape(8.dp))
//                                .border(1.dp, Color(0xFFFF0050), RoundedCornerShape(8.dp))
//                                .padding(8.dp)
//                        ) {
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                Icon(imageVector = Icons.Default.Timer, contentDescription = null)
//                                Spacer(Modifier.width(4.dp))
//                                // format the live-updating state:
//                                val mins = TimeUnit.MILLISECONDS.toMinutes(sessionTimeRemaining)
//                                val secs = TimeUnit.MILLISECONDS.toSeconds(sessionTimeRemaining) % 60
//                                Text(
//                                    text = String.format("%02d:%02d", mins, secs),
//                                    color = Color.White,
//                                    fontWeight = FontWeight.Bold
//                                )
//                            }
//                        }
//                    }
                }
            }

            // Bottom navigation bar
            TikTokBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            )

            // Action buttons row at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    // Session control button
//                    Button(
//                        onClick = {
//                            if (isSessionActive) {
//                                viewModel.endSession()
//                            } else {
//                                viewModel.exportSessionData()
//                            }
//                        },
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (isSessionActive) Color(0xFFFF4444) else Color(0xFF00AAFF)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Icon(
//                            imageVector = if (isSessionActive) Icons.Default.TimerOff else Icons.Default.Download,
//                            contentDescription = if (isSessionActive) "End Session" else "Export Data"
//                        )
//
//                        Spacer(modifier = Modifier.width(4.dp))
//
//                        Text(
//                            text = if (isSessionActive) "End Session" else "Export Data",
//                            color = Color.White
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.width(8.dp))
//
//                    // Generate PNG button (only for last swipe)
//                    Button(
//                        onClick = {
//                            lastSwipeEvent?.let { viewModel.generateSwipePatternPng(it.id) }
//                        },
//                        enabled = lastSwipeEvent != null,
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color(0xFF00CC66)
//                        ),
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Camera,
//                            contentDescription = "Generate Swipe PNG"
//                        )
//
//                        Spacer(modifier = Modifier.width(4.dp))
//
//                        Text(
//                            text = "Save Swipe",
//                            color = Color.White
//                        )
//                    }
                }

                // Status text
//                if (exportStatus.isNotEmpty()) {
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    Text(
//                        text = exportStatus,
//                        color = Color.White,
//                        fontSize = 12.sp,
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(Color(0x80000000), RoundedCornerShape(4.dp))
//                            .padding(4.dp)
//                    )
//                }
            }

//            // Debug button
//            FloatingActionButton(
//                onClick = onOpenDebugScreen,
//                modifier = Modifier
//                    .align(Alignment.BottomEnd)
//                    .padding(16.dp),
//                containerColor = Color(0x99000000),
//                contentColor = Color.White
//            ) {
//                Icon(
//                    imageVector = Icons.Default.BugReport,
//                    contentDescription = "Open Debug"
//                )
//            }

            // Show swipe analytics overlay if debug info is enabled
            if (showDebugInfo && lastSwipeEvent != null) {
                val swipeAnalytics = lastSwipeEvent?.let { analyticsService.analyzeSwipe(it) }
                SwipeAnalyticsOverlay(
                    swipeEvent = lastSwipeEvent,
                    swipeAnalytics = swipeAnalytics,
                    showDebugInfo = showDebugInfo
                )
            }

            // Show random stop overlay when active
            if (isRandomStopActive) {
                RandomStopOverlay()
            }
        }
    }
}