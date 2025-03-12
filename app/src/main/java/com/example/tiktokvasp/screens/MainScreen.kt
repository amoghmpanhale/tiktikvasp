package com.example.tiktokvasp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktokvasp.components.TikTokBottomBar
import com.example.tiktokvasp.components.TikTokOverlay
import com.example.tiktokvasp.components.VideoPlayer
import com.example.tiktokvasp.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val currentVideoIndex by viewModel.currentVideoIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
                    val currentVideo = videos[currentVideoIndex]

                    // Video Player (handles swipes)
                    VideoPlayer(
                        video = currentVideo,
                        onSwipeUp = viewModel::onSwipeUp,
                        onSwipeDown = viewModel::onSwipeDown,
                        modifier = Modifier.fillMaxSize()
                    )

                    // UI Overlay (buttons, etc.)
                    TikTokOverlay(
                        video = currentVideo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}