package com.example.tiktokvasp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktokvasp.model.Video
import com.example.tiktokvasp.repository.VideoRepository
import com.example.tiktokvasp.tracking.DataExporter
import com.example.tiktokvasp.tracking.SwipeDirection
import com.example.tiktokvasp.tracking.UserBehaviorTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val behaviorTracker = UserBehaviorTracker()
    private val dataExporter = DataExporter(application)

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var videoViewStartTime = 0L
    private var currentVideoId: String? = null

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val localVideos = repository.getLocalVideos()
            _videos.value = localVideos
            _isLoading.value = false

            if (localVideos.isNotEmpty()) {
                startVideoViewTracking(localVideos.first().id)
            }
        }
    }

    fun onSwipeUp() {
        val videos = _videos.value
        if (videos.isEmpty()) return

        val currentIndex = _currentVideoIndex.value
        if (currentIndex < videos.size - 1) {
            trackSwipeEvent(SwipeDirection.UP)
            endVideoViewTracking()

            _currentVideoIndex.value = currentIndex + 1

            startVideoViewTracking(videos[currentIndex + 1].id)
        }
    }

    fun onSwipeDown() {
        val currentIndex = _currentVideoIndex.value
        if (currentIndex > 0) {
            trackSwipeEvent(SwipeDirection.DOWN)
            endVideoViewTracking()

            _currentVideoIndex.value = currentIndex - 1

            startVideoViewTracking(_videos.value[currentIndex - 1].id)
        }
    }

    fun onPageSelected(position: Int) {
        if (position != _currentVideoIndex.value) {
            // If the page was changed programmatically (e.g. by ViewPager2),
            // track it as a swipe event
            val direction = if (position > _currentVideoIndex.value)
                SwipeDirection.UP else SwipeDirection.DOWN

            trackSwipeEvent(direction)
            endVideoViewTracking()
            _currentVideoIndex.value = position
            startVideoViewTracking(_videos.value[position].id)
        }
    }

    private fun startVideoViewTracking(videoId: String) {
        videoViewStartTime = System.currentTimeMillis()
        currentVideoId = videoId
        _isPlaying.value = true
    }

    private fun endVideoViewTracking() {
        currentVideoId?.let { videoId ->
            val watchDuration = System.currentTimeMillis() - videoViewStartTime
            val currentVideo = _videos.value.find { it.id == videoId }

            currentVideo?.let {
                val watchPercentage = if (it.duration > 0) {
                    (watchDuration / it.duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

                behaviorTracker.trackVideoView(videoId, watchDuration, watchPercentage)
            }
        }
    }

    private fun trackSwipeEvent(direction: SwipeDirection) {
        currentVideoId?.let { videoId ->
            val swipeDuration = System.currentTimeMillis() - videoViewStartTime
            behaviorTracker.trackSwipe(direction, videoId, swipeDuration)
        }
    }

    fun exportData() {
        viewModelScope.launch {
            endVideoViewTracking() // Make sure we record the last video view

            val swipeEvents = behaviorTracker.getSwipeEvents()
            val viewEvents = behaviorTracker.getViewEvents()

            dataExporter.exportSwipeEvents(swipeEvents)
            dataExporter.exportViewEvents(viewEvents)
        }
    }

    fun toggleVideoPlayback() {
        _isPlaying.value = !_isPlaying.value
    }

    fun likeCurrentVideo() {
        // Implementation for liking the current video
        currentVideoId?.let { videoId ->
            // Add like functionality here
        }
    }

    fun incrementVideoWatchCount(videoId: String) {
        viewModelScope.launch {
            repository.incrementVideoWatchCount(videoId)
        }
    }

    fun likeVideo(videoId: String) {
        // Implementation for liking a video
    }

    fun openComments(videoId: String) {
        // Implementation for opening comments
    }

    fun shareVideo(videoId: String) {
        // Implementation for sharing a video
    }

    fun openUserProfile(videoId: String) {
        // Implementation for opening user profile
    }

    override fun onCleared() {
        endVideoViewTracking()
        exportData() // Export data when ViewModel is cleared
        super.onCleared()
    }
}