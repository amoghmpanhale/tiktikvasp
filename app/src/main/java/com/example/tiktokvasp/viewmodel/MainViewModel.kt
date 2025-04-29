// Add this to app/src/main/java/com/example/tiktokvasp/viewmodel/MainViewModel.kt

package com.example.tiktokvasp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktokvasp.model.Video
import com.example.tiktokvasp.repository.VideoRepository
import com.example.tiktokvasp.tracking.DataExporter
import com.example.tiktokvasp.tracking.SessionManager
import com.example.tiktokvasp.tracking.SwipeAnalyticsService
import com.example.tiktokvasp.tracking.SwipeDirection
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.tracking.SwipeAnalytics
import com.example.tiktokvasp.tracking.UserBehaviorTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val behaviorTracker = UserBehaviorTracker()
    private val dataExporter = DataExporter(application)
    private val analyticsService = SwipeAnalyticsService()
    private var sessionManager: SessionManager? = null

    private val _participantId = MutableStateFlow("")
    val participantId: StateFlow<String> = _participantId.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    // Keep original list of videos for looping
    private val _originalVideos = mutableListOf<Video>()

    private val _currentVideoIndex = MutableStateFlow(0)
    val currentVideoIndex: StateFlow<Int> = _currentVideoIndex.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Session timer state
    private val _sessionTimeRemaining = MutableStateFlow<Long>(0)
    val sessionTimeRemaining: StateFlow<Long> = _sessionTimeRemaining.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    // Export status
    private val _exportStatus = MutableStateFlow("")
    val exportStatus: StateFlow<String> = _exportStatus.asStateFlow()

    // Detailed swipe tracking stats
    private val _totalTrackedSwipes = MutableStateFlow(0)
    val totalTrackedSwipes: StateFlow<Int> = _totalTrackedSwipes.asStateFlow()

    private val _avgSwipeVelocity = MutableStateFlow(0f)
    val avgSwipeVelocity: StateFlow<Float> = _avgSwipeVelocity.asStateFlow()

    // Track liked videos
    private val _likedVideos = MutableStateFlow<Set<String>>(emptySet())
    val likedVideos: StateFlow<Set<String>> = _likedVideos.asStateFlow()

    // Track shared videos
    private val _sharedVideos = MutableStateFlow<Set<String>>(emptySet())
    val sharedVideos: StateFlow<Set<String>> = _sharedVideos.asStateFlow()

    // Map to store all swipe analytics
    private val swipeAnalyticsMap = mutableMapOf<String, SwipeAnalytics>()

    // Map to store paths to generated swipe pattern PNGs
    private val swipePatternPaths = mutableMapOf<String, String>()

    private var videoViewStartTime = 0L
    private var currentVideoId: String? = null
    private var categoryFolder: String = ""

    private val _swipeEvents = mutableListOf<SwipeEvent>()
    private val _swipeAnalytics = mutableListOf<SwipeAnalytics>()
    private val _lastSwipeEvent = MutableStateFlow<SwipeEvent?>(null)
    val lastSwipeEvent: StateFlow<SwipeEvent?> = _lastSwipeEvent.asStateFlow()

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val localVideos = repository.getLocalVideos()

            // Store original videos
            _originalVideos.clear()
            _originalVideos.addAll(localVideos)

            // Randomize and set the videos
            val randomizedVideos = localVideos.shuffled()
            _videos.value = randomizedVideos

            _isLoading.value = false

            if (randomizedVideos.isNotEmpty()) {
                startVideoViewTracking(randomizedVideos.first().id)
            }
        }
    }

    fun setParticipantId(id: String) {
        _participantId.value = id
    }

    /**
     * Start a timed data collection session
     */
    fun startSession(durationMinutes: Int, autoGeneratePngs: Boolean) {
        if (_participantId.value.isBlank() || categoryFolder.isBlank()) {
            _exportStatus.value = "Please set participant ID and select a video folder first"
            return
        }

        // Create session manager
        sessionManager = SessionManager(
            getApplication(),
            _participantId.value,
            categoryFolder
        ).apply {
            setOnTimerUpdateListener { remainingTime ->
                _sessionTimeRemaining.value = remainingTime
            }

            setOnSessionCompleteListener {
                _isSessionActive.value = false
                _exportStatus.value = "Session completed. Exporting data..."

                // Export data when session ends
                exportSessionData()
            }

            // Start the session
            startSession(durationMinutes, autoGeneratePngs)
        }

        _isSessionActive.value = true
        _exportStatus.value = "Session started. Duration: $durationMinutes minutes"
    }

    /**
     * Check if we need to loop and handle videos
     */
    fun checkAndHandleLooping() {
        val videos = _videos.value
        val currentIndex = _currentVideoIndex.value

        // If we've reached the end of the list and we have videos
        if (currentIndex >= videos.size - 1 && videos.isNotEmpty()) {
            // Create a new random order but don't repeat the last video first
            val videosExceptLast = _originalVideos.filter { it.id != videos.last().id }
            val newRandomVideos = if (videosExceptLast.isNotEmpty()) {
                listOf(videosExceptLast.random()) + videosExceptLast.shuffled().filter { it.id != videosExceptLast.random().id }
            } else {
                _originalVideos.shuffled()
            }

            // Update the videos list with the new random order
            _videos.value = newRandomVideos

            // Reset index to 0
            _currentVideoIndex.value = 0

            // Start tracking the new first video
            if (newRandomVideos.isNotEmpty()) {
                startVideoViewTracking(newRandomVideos.first().id)
            }
        }
    }

    /**
     * End the current session
     */
    fun endSession() {
        sessionManager?.endSession()
        _isSessionActive.value = false
    }

    /**
     * Export session data to CSV with all metrics
     */
    fun exportSessionData() {
        viewModelScope.launch {
            endVideoViewTracking() // Make sure we record the last video view
            _exportStatus.value = "Exporting data..."

            val swipeEvents = behaviorTracker.getSwipeEvents()
            val viewEvents = behaviorTracker.getViewEvents()

            // Export using the enhanced format
            val exportedFilePath = dataExporter.exportSessionData(
                participantId = _participantId.value,
                category = categoryFolder,
                videos = _videos.value,
                viewEvents = viewEvents,
                swipeEvents = swipeEvents,
                swipeAnalytics = swipeAnalyticsMap,
                swipePatternPaths = swipePatternPaths
            )

            if (exportedFilePath.isNotEmpty()) {
                _exportStatus.value = "Data exported to: $exportedFilePath"
            } else {
                _exportStatus.value = "Failed to export data"
            }

            // Also export the legacy formats for compatibility
            dataExporter.exportSwipeEvents(swipeEvents)
            dataExporter.exportSwipeEventsAsCSV(swipeEvents)
            dataExporter.exportViewEvents(viewEvents)

            Log.d(
                "MainViewModel",
                "Exported ${swipeEvents.size} swipe events and ${viewEvents.size} view events"
            )
        }
    }

    /**
     * Generate a swipe pattern PNG on demand
     */
    fun generateSwipePatternPng(swipeId: String) {
        viewModelScope.launch {
            val event = behaviorTracker.getSwipeEvents().find { it.id == swipeId }
            event?.let {
                sessionManager?.let { manager ->
                    val path = manager.generateSwipePatternPng(it)
                    if (path.isNotEmpty()) {
                        swipePatternPaths[swipeId] = path
                        _exportStatus.value = "Generated swipe pattern: $path"
                    }
                }
            }
        }
    }

    /**
     * Expose the behavior tracker for debugging purposes
     */
    fun getBehaviorTracker(): UserBehaviorTracker {
        return behaviorTracker
    }

    /**
     * Track a detailed swipe event from the EnhancedSwipeTracker
     */
    fun trackDetailedSwipe(swipeEvent: SwipeEvent) {
        viewModelScope.launch {
            // Send the event to the behavior tracker
            behaviorTracker.trackDetailedSwipe(swipeEvent)

            // Add this log statement:
            Log.d("MainViewModel", "trackDetailedSwipe: swipeEvent = $swipeEvent")

            // Analyze the swipe event
            val swipeAnalytics = analyticsService.analyzeSwipe(swipeEvent)

            // Add this log statement:
            Log.d("MainViewModel", "trackDetailedSwipe: swipeAnalytics = $swipeAnalytics")

            // Store the swipe event and analytics
            _swipeEvents.add(swipeEvent)
            _swipeAnalytics.add(swipeAnalytics)

            // Update the last swipe event
            _lastSwipeEvent.value = swipeEvent
            swipeAnalyticsMap[swipeEvent.id] = swipeAnalytics

            // Generate PNG if auto-generation is enabled
            if (sessionManager?.isAutoGeneratePngsEnabled() == true) {
                val path = sessionManager?.generateSwipePatternPng(swipeEvent)
                if (!path.isNullOrEmpty()) {
                    swipePatternPaths[swipeEvent.id] = path
                }
            }

            // Update tracking stats
            _totalTrackedSwipes.value = _totalTrackedSwipes.value + 1

            // Calculate average velocity (absolute value of y-velocity since we're mainly concerned with vertical swipes)
            val currentTotal = _avgSwipeVelocity.value * (_totalTrackedSwipes.value - 1)
            val newAvg = (currentTotal + Math.abs(swipeEvent.velocityY)) / _totalTrackedSwipes.value
            _avgSwipeVelocity.value = newAvg

            // Log detailed information about the swipe
            Log.d(
                "MainViewModel", "Tracked detailed swipe: " +
                        "direction=${swipeEvent.direction}, " +
                        "velocity=(${swipeEvent.velocityX.toInt()}, ${swipeEvent.velocityY.toInt()}), " +
                        "distance=${swipeEvent.distance.toInt()}, " +
                        "duration=${swipeEvent.swipeDurationMs}ms, " +
                        "points=${swipeEvent.path.size}"
            )

            // For significant swipes, we'll end the current video view tracking
            // and update the current video as needed based on the direction
            when (swipeEvent.direction) {
                SwipeDirection.UP, SwipeDirection.DOWN -> {
                    endVideoViewTracking()
                }

                else -> { /* No action for horizontal swipes */
                }
            }
        }
    }

    fun onSwipeUp() {
        val videos = _videos.value
        if (videos.isEmpty()) return

        val currentIndex = _currentVideoIndex.value

        // Check if we're at the last video
        if (currentIndex >= videos.size - 1) {
            // Only perform basic tracking if not already handled by enhanced tracking
            if (_totalTrackedSwipes.value == 0) {
                trackSwipeEvent(SwipeDirection.UP)
                endVideoViewTracking()
            }

            // Create a new randomized list and reset to beginning
            checkAndHandleLooping()

            // Update the session manager with the new video view
            if (_videos.value.isNotEmpty()) {
                sessionManager?.trackVideoView(_videos.value.first())
            }
        } else {
            // Normal behavior for non-last videos
            // Only perform basic tracking if not already handled by enhanced tracking
            if (_totalTrackedSwipes.value == 0) {
                trackSwipeEvent(SwipeDirection.UP)
                endVideoViewTracking()
            }

            _currentVideoIndex.value = currentIndex + 1
            startVideoViewTracking(videos[currentIndex + 1].id)

            // Update the session manager with the new video view
            sessionManager?.trackVideoView(videos[currentIndex + 1])
        }
    }

    fun onSwipeDown() {
        val currentIndex = _currentVideoIndex.value
        if (currentIndex > 0) {
            // Only perform basic tracking if not already handled by enhanced tracking
            if (_totalTrackedSwipes.value == 0) {
                trackSwipeEvent(SwipeDirection.DOWN)
                endVideoViewTracking()
            }

            _currentVideoIndex.value = currentIndex - 1
            startVideoViewTracking(_videos.value[currentIndex - 1].id)

            // Update the session manager with the new video view
            sessionManager?.trackVideoView(_videos.value[currentIndex - 1])
        }
    }

    fun onPageSelected(position: Int) {
        if (position != _currentVideoIndex.value) {
            // If the page was changed programmatically (e.g. by ViewPager2),
            // track it as a swipe event if it wasn't already tracked
            if (_totalTrackedSwipes.value == 0) {
                val direction = if (position > _currentVideoIndex.value)
                    SwipeDirection.UP else SwipeDirection.DOWN

                trackSwipeEvent(direction)
                endVideoViewTracking()
            }

            _currentVideoIndex.value = position
            startVideoViewTracking(_videos.value[position].id)

            // Update the session manager with the new video view
            sessionManager?.trackVideoView(_videos.value[position])
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

    fun toggleVideoPlayback() {
        _isPlaying.value = !_isPlaying.value
    }

    fun likeCurrentVideo() {
        currentVideoId?.let { videoId ->
            likeVideo(videoId)
        }
    }

    fun incrementVideoWatchCount(videoId: String) {
        viewModelScope.launch {
            repository.incrementVideoWatchCount(videoId)
        }
    }

    fun likeVideo(videoId: String) {
        // Track the like in the behavior tracker
        behaviorTracker.trackVideoLike(videoId)

        // Update the UI state
        val currentLikes = _likedVideos.value.toMutableSet()
        currentLikes.add(videoId)
        _likedVideos.value = currentLikes

        Log.d("MainViewModel", "Liked video: $videoId")
    }

    fun openComments(videoId: String) {
        // Implementation for opening comments
    }

    fun shareVideo(videoId: String) {
        // Track the share in the behavior tracker
        behaviorTracker.trackVideoShare(videoId)

        // Update the UI state
        val currentShares = _sharedVideos.value.toMutableSet()
        currentShares.add(videoId)
        _sharedVideos.value = currentShares

        Log.d("MainViewModel", "Shared video: $videoId")
    }

    fun openUserProfile(videoId: String) {
        // Implementation for opening user profile
    }

    fun loadVideosFromFolder(folderName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            categoryFolder = folderName
            val folderVideos = repository.getVideosFromFolder(folderName)

            // Store original videos
            _originalVideos.clear()
            _originalVideos.addAll(folderVideos)

            // Randomize and set the videos
            val randomizedVideos = folderVideos.shuffled()
            _videos.value = randomizedVideos

            _isLoading.value = false

            if (randomizedVideos.isNotEmpty()) {
                startVideoViewTracking(randomizedVideos.first().id)
            }
        }
    }

    // Functions to check if a video is liked or shared
    fun isVideoLiked(videoId: String): Boolean {
        return _likedVideos.value.contains(videoId)
    }

    fun isVideoShared(videoId: String): Boolean {
        return _sharedVideos.value.contains(videoId)
    }

    /**
     * Get formatted time remaining string (mm:ss)
     */
    fun getFormattedTimeRemaining(): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(_sessionTimeRemaining.value)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(_sessionTimeRemaining.value) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onCleared() {
        endVideoViewTracking()
        endSession()
        exportSessionData() // Export data when ViewModel is cleared
        super.onCleared()
    }
}