package com.example.tiktokvasp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.tiktokvasp.components.TikTokBottomBar
import com.example.tiktokvasp.viewmodel.MainViewModel
import com.example.tiktokvasp.util.TikTokSwipeDetector

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val currentVideoIndex by viewModel.currentVideoIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // State for ViewPager2 reference
    var viewPager by remember { mutableStateOf<ViewPager2?>(null) }

    // Create the adapter when videos are loaded
    val videoAdapter = remember(videos) {
        VideoAdapter(context, videos, viewModel)
    }

    Scaffold(
        bottomBar = {
            TikTokBottomBar()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

                                // Save reference to viewPager
                                viewPager = this
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

                    // Apply custom swipe detector
                    DisposableEffect(viewPager) {
                        val swipeDetector = TikTokSwipeDetector(context)
                        swipeDetector.setOnSwipeListener(object : TikTokSwipeDetector.OnSwipeListener {
                            override fun onSwipeUp() {
                                viewModel.onSwipeUp()
                            }

                            override fun onSwipeDown() {
                                viewModel.onSwipeDown()
                            }

                            override fun onSingleTap() {
                                viewModel.toggleVideoPlayback()
                            }

                            override fun onDoubleTap() {
                                viewModel.likeCurrentVideo()
                            }
                        })

                        viewPager?.setOnTouchListener(swipeDetector)

                        onDispose {
                            viewPager?.setOnTouchListener(null)
                            viewPager = null
                        }
                    }
                }
            }
        }
    }
}