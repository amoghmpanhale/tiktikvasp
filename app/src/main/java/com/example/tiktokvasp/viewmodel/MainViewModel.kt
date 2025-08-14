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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log
import com.example.tiktokvasp.tracking.PlayByPlayEvent
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
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

    // Videos should always be playing - remove toggle functionality
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

    // Random Stops Properties
    private val _randomStopsEnabled = MutableStateFlow(false)
    val randomStopsEnabled: StateFlow<Boolean> = _randomStopsEnabled.asStateFlow()

    private val _randomStopFrequency = MutableStateFlow(30) // Default 30 seconds
    val randomStopFrequency: StateFlow<Int> = _randomStopFrequency.asStateFlow()

    private val _randomStopDuration = MutableStateFlow(15000) // Default 15000ms
    val randomStopDuration: StateFlow<Int> = _randomStopDuration.asStateFlow()

    // #region Minimum pause duration
    private val _minPauseDuration = MutableStateFlow(10) // Default 10 seconds
    val minPauseDuration: StateFlow<Int> = _minPauseDuration.asStateFlow()
    // #endregion

    private val _isRandomStopActive = MutableStateFlow(false)
    val isRandomStopActive: StateFlow<Boolean> = _isRandomStopActive.asStateFlow()

    private var randomStopJob: Job? = null
    private val random = Random.Default

    // Track the last pause timestamp
    private var lastPauseTimestamp = 0L

    // Track commented videos
    private val _commentedVideos = MutableStateFlow<Set<String>>(emptySet())
    val commentedVideos: StateFlow<Set<String>> = _commentedVideos.asStateFlow()

    // Track comment counts per video
    private val _commentCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val commentCounts: StateFlow<Map<String, Int>> = _commentCounts.asStateFlow()

    // Track bookmarked videos
    private val _bookmarkedVideos = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedVideos: StateFlow<Set<String>> = _bookmarkedVideos.asStateFlow()

    // Play-by-play tracking
    private val playByPlayEvents = mutableListOf<PlayByPlayEvent>()
    private var currentVideoStartTime = 0L
    private var viewingInstanceCounter = 0
    private var currentInterruption: InterruptionData? = null

    // Add a map to track video IDs to their numbers in the original list
    private val videoIdToNumberMap = mutableMapOf<String, Int>()

    // Track interruption timing
    private var lastInterruptionEndTime: Long = 0L
    private var sessionStartTime: Long = 0L

    // Track watch counts per video (cumulative for UI purposes)
    private val _watchCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val watchCounts: StateFlow<Map<String, Int>> = _watchCounts.asStateFlow()

    // Track current session watch count (resets each viewing session)
    private var currentSessionWatchCount = 0

    // Data class to track interruption details for current video
    private data class InterruptionData(
        val startTime: Long,        // When interruption started (system time)
        val videoTimeStamp: Long,   // Time in video when interruption occurred
        val durationMs: Long,       // Duration of the interruption
        val timeSinceLastInterruptionMs: Long // Time from last interruption end to this interruption start
    )

    /**
     * Bookmark a video
     */
    fun bookmarkVideo(videoId: String) {
        // Track the bookmark in the behavior tracker (you may need to add this method)
        // behaviorTracker.trackVideoBookmark(videoId)

        // Update the UI state
        val currentBookmarks = _bookmarkedVideos.value.toMutableSet()
        currentBookmarks.add(videoId)
        _bookmarkedVideos.value = currentBookmarks

        Log.d("MainViewModel", "Bookmarked video: $videoId")
    }

    /**
     * Remove bookmark from a video
     */
    fun unbookmarkVideo(videoId: String) {
        // Update the UI state by removing from the set
        val currentBookmarks = _bookmarkedVideos.value.toMutableSet()
        currentBookmarks.remove(videoId)
        _bookmarkedVideos.value = currentBookmarks

        Log.d("MainViewModel", "Unbookmarked video: $videoId")
    }

    /**
     * Check if a video is bookmarked
     */
    fun isVideoBookmarked(videoId: String): Boolean {
        return _bookmarkedVideos.value.contains(videoId)
    }

    /**
     * Remove comment from a video (toggle off)
     */
    fun uncommentVideo(videoId: String) {
        // Remove this video from commented videos
        val currentComments = _commentedVideos.value.toMutableSet()
        currentComments.remove(videoId)
        _commentedVideos.value = currentComments

        // Reset comment count for this video
        val currentCounts = _commentCounts.value.toMutableMap()
        currentCounts.remove(videoId)
        _commentCounts.value = currentCounts

        Log.d("MainViewModel", "Removed comment from video: $videoId")
    }

    /**
     * Remove share from a video (toggle off)
     */
    fun unshareVideo(videoId: String) {
        // Update the UI state by removing from the set
        val currentShares = _sharedVideos.value.toMutableSet()
        currentShares.remove(videoId)
        _sharedVideos.value = currentShares

        Log.d("MainViewModel", "Unshared video: $videoId")
    }

    /**
     * Record a comment on a video
     */
    fun commentOnVideo(videoId: String) {
        // Mark this video as commented on
        val currentComments = _commentedVideos.value.toMutableSet()
        currentComments.add(videoId)
        _commentedVideos.value = currentComments

        // Increment comment count for this video
        val currentCounts = _commentCounts.value.toMutableMap()
        val currentCount = currentCounts[videoId] ?: 0
        currentCounts[videoId] = currentCount + 1
        _commentCounts.value = currentCounts

        // Also open the comments UI
        openComments(videoId)

        behaviorTracker.trackVideoComment(videoId)

        Log.d("MainViewModel", "Comment added to video: $videoId, total comments: ${currentCount + 1}")
    }

    /**
     * Check if the user has commented on a video
     */
    fun hasCommentedOnVideo(videoId: String): Boolean {
        return _commentedVideos.value.contains(videoId)
    }

    /**
     * Get comment count for a video
     */
    fun getCommentCount(videoId: String): Int {
        return _commentCounts.value[videoId] ?: 0
    }

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val localVideos = repository.getLocalVideos()

            _originalVideos.clear()
            _originalVideos.addAll(localVideos)

            initializeVideoNumbering(localVideos)

            val randomizedVideos = localVideos.shuffled()
            _videos.value = randomizedVideos

            _isLoading.value = false

            if (randomizedVideos.isNotEmpty()) {
                startVideoViewTracking(randomizedVideos.first().id)  // Don't increment watch count here
            }
        }
    }


    fun setParticipantId(id: String) {
        _participantId.value = id
    }

    /**
     * Reset interruption timing when starting a new session
     */
    fun startSession(
        durationMinutes: Int,
        autoGeneratePngs: Boolean,
        randomStopsEnabled: Boolean = false,
        randomStopFrequency: Int = 45,
        randomStopDuration: Int = 22500,
        minPauseDuration: Int = 30
    ) {
        if (_participantId.value.isBlank() || categoryFolder.isBlank()) {
            _exportStatus.value = "Please set participant ID and select a video folder first"
            return
        }

        // Clear previous session data
        playByPlayEvents.clear()
        viewingInstanceCounter = 0
        currentInterruption = null
        lastInterruptionEndTime = 0L // Reset interruption timing
        sessionStartTime = System.currentTimeMillis() // Track session start time

        configureRandomStops(randomStopsEnabled)

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
                stopRandomStopTimer()
                _exportStatus.value = "Session completed. Exporting data..."
                exportSessionData()
            }

            startSession(durationMinutes, autoGeneratePngs)
        }

        _isSessionActive.value = true

        if (randomStopsEnabled) {
            startRandomStopTimer()
        }
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
        // Stop the random stops timer if it's running
        stopRandomStopTimer()
        _isRandomStopActive.value = false

        // End video view tracking and the session
        endVideoViewTracking()
        sessionManager?.endSession()
        _isSessionActive.value = false
    }

    /**
     * Modified export method to include play-by-play data
     */
    fun exportSessionData() {
        viewModelScope.launch {
            endCurrentVideoTracking() // Make sure we record the last video view
            _exportStatus.value = "Exporting data..."

            val swipeEvents = behaviorTracker.getSwipeEvents()
            val viewEvents = behaviorTracker.getViewEvents()
            val interruptionEvents = behaviorTracker.getInterruptionEvents()

            // Export the original session data
            val exportedFilePath = dataExporter.exportSessionData(
                participantId = _participantId.value,
                category = categoryFolder,
                videos = _videos.value,
                viewEvents = viewEvents,
                swipeEvents = swipeEvents,
                swipeAnalytics = swipeAnalyticsMap,
                swipePatternPaths = swipePatternPaths,
                interruptionEvents = interruptionEvents,
                viewModel = this@MainViewModel // Add this line
            )

            // Export the new play-by-play data
            val playByPlayFilePath = dataExporter.exportPlayByPlayData(
                participantId = _participantId.value,
                category = categoryFolder,
                playByPlayEvents = playByPlayEvents
            )

            if (exportedFilePath.isNotEmpty() && playByPlayFilePath.isNotEmpty()) {
                _exportStatus.value = "Data exported to: $exportedFilePath and $playByPlayFilePath"
            } else {
                _exportStatus.value = "Failed to export some data files"
            }

            // Also export the legacy formats for compatibility
            dataExporter.exportSwipeEvents(swipeEvents)
            dataExporter.exportSwipeEventsAsCSV(swipeEvents)
            dataExporter.exportViewEvents(viewEvents)

            Log.d(
                "MainViewModel",
                "Exported ${swipeEvents.size} swipe events, ${viewEvents.size} view events, " +
                        "${interruptionEvents.size} interruption events, and ${playByPlayEvents.size} play-by-play events"
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
     * Track when a video is rewatched (looped) without swiping
     */
    fun trackVideoRewatch(videoId: String) {
        // Only track if this is the current video (prevent stale callbacks)
        if (videoId == currentVideoId && _isSessionActive.value) {
            // Increment the session watch count
            currentSessionWatchCount++

            Log.d("MainViewModel", "Video rewatched in same session - session watch count now: ${currentSessionWatchCount + 1} for video: $videoId")
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
//            when (swipeEvent.direction) {
//                SwipeDirection.UP, SwipeDirection.DOWN -> {
//                    endVideoViewTracking()
//                }
//
//                else -> { /* No action for horizontal swipes */
//                }
//            }
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

    /**
     * Start tracking a new video viewing session
     */
    private fun startVideoViewTracking(videoId: String) {
        // End previous video tracking if it exists
        endCurrentVideoTracking()

        videoViewStartTime = System.currentTimeMillis()
        currentVideoStartTime = System.currentTimeMillis()
        currentVideoId = videoId
        viewingInstanceCounter++
        currentInterruption = null
        currentSessionWatchCount = 0  // Reset session watch count for new video
        _isPlaying.value = true

        Log.d("MainViewModel", "Started viewing instance #$viewingInstanceCounter: $videoId (session watch count reset)")
    }


    /**
     * End the current video tracking and create a play-by-play event
     */
    private fun endCurrentVideoTracking() {
        currentVideoId?.let { videoId ->
            val currentVideo = _videos.value.find { it.id == videoId }
            currentVideo?.let { video ->
                val watchDuration = System.currentTimeMillis() - currentVideoStartTime

                // Calculate watch count using ratio approach for this session only
                val watchRatio = if (video.duration > 0) {
                    watchDuration.toFloat() / video.duration.toFloat()
                } else {
                    0f
                }
                val baseWatchCount = kotlin.math.ceil(watchRatio).toInt().coerceAtLeast(1)

                // Add any rewatch count from this session
                val sessionWatchCount = baseWatchCount + currentSessionWatchCount

                // Update cumulative watch count for UI (optional - for display purposes)
                val currentCounts = _watchCounts.value.toMutableMap()
                val previousCount = currentCounts[videoId] ?: 0
                currentCounts[videoId] = previousCount + sessionWatchCount
                _watchCounts.value = currentCounts

                // Get the video number from the original video list
                val videoNumber = getVideoNumber(videoId)

                // Use the time since last interruption that was calculated when the interruption started
                val timeSinceLastInterruption = currentInterruption?.timeSinceLastInterruptionMs ?: 0L

                // Create play-by-play event with THIS SESSION'S watch count only
                val playByPlayEvent = PlayByPlayEvent(
                    videoNumber = videoNumber,
                    videoName = video.title,
                    videoDurationMs = video.duration,
                    watchDurationMs = watchDuration,
                    watchCount = sessionWatchCount, // This is the key change - use session count, not cumulative
                    wasLiked = isVideoLiked(videoId),
                    wasShared = isVideoShared(videoId),
                    wasCommented = hasCommentedOnVideo(videoId),
                    interruptionOccurred = currentInterruption != null,
                    interruptionDurationMs = currentInterruption?.durationMs ?: 0L,
                    interruptionPointMs = currentInterruption?.videoTimeStamp ?: 0L,
                    timeSinceLastInterruptionMs = timeSinceLastInterruption
                )

                playByPlayEvents.add(playByPlayEvent)

                Log.d(
                    "MainViewModel", "Ended viewing session #$viewingInstanceCounter: " +
                            "Video #$videoNumber (${video.title}), watched ${watchDuration}ms, " +
                            "session watch count: $sessionWatchCount, " +
                            "cumulative total: ${currentCounts[videoId]}, " +
                            "interruption: ${currentInterruption != null}"
                )

                // Also create the regular view event for backward compatibility
                val watchPercentage = if (video.duration > 0) {
                    watchDuration / video.duration.toFloat()
                } else {
                    0f
                }
                behaviorTracker.trackVideoView(videoId, watchDuration, watchPercentage)
            }
        }
    }


    /**
     * Get the consistent video number for a video ID
     * This ensures both CSVs use the same numbering system
     */
    private fun getVideoNumber(videoId: String): Int {
        return videoIdToNumberMap[videoId] ?: run {
            // If not found, try to find it in current videos and assign a number
            val currentVideo = _videos.value.find { it.id == videoId }
            if (currentVideo != null) {
                val index = _videos.value.indexOf(currentVideo)
                val number = index + 1
                videoIdToNumberMap[videoId] = number
                number
            } else {
                // Fallback: check original videos
                val originalVideo = _originalVideos.find { it.id == videoId }
                if (originalVideo != null) {
                    val index = _originalVideos.indexOf(originalVideo)
                    val number = index + 1
                    videoIdToNumberMap[videoId] = number
                    number
                } else {
                    0 // Should not happen
                }
            }
        }
    }

    private fun endVideoViewTracking() {
        currentVideoId?.let { videoId ->
            val watchDuration = System.currentTimeMillis() - videoViewStartTime
            val currentVideo = _videos.value.find { it.id == videoId }

            currentVideo?.let {
                val watchPercentage = if (it.duration > 0) {
                    watchDuration / it.duration.toFloat()  // Allow unlimited ratio
                } else {
                    0f
                }

                behaviorTracker.trackVideoView(videoId, watchDuration, watchPercentage)
            }
        }
    }

    /**
     * Initialize video numbering when videos are loaded
     */
    private fun initializeVideoNumbering(videos: List<Video>) {
        videoIdToNumberMap.clear()
        videos.forEachIndexed { index, video ->
            videoIdToNumberMap[video.id] = index + 1
        }
        Log.d("MainViewModel", "Initialized video numbering: ${videoIdToNumberMap.size} videos")
    }

    private fun trackSwipeEvent(direction: SwipeDirection) {
        currentVideoId?.let { videoId ->
            val swipeDuration = System.currentTimeMillis() - videoViewStartTime
            behaviorTracker.trackSwipe(direction, videoId, swipeDuration)
        }
    }

    /**
     * Configure random stops with new randomized system
     */
    fun configureRandomStops(enabled: Boolean) {
        _randomStopsEnabled.value = enabled

        if (enabled && _isSessionActive.value) {
            startRandomStopTimer()
        } else {
            stopRandomStopTimer()
        }

        Log.d(
            "MainViewModel",
            "Random stops configured: enabled=$enabled, interval=30-60s, duration=15-30s"
        )
    }

    private fun startRandomStopTimer() {
        // Cancel any existing job
//        randomStopJob?.cancel()

        // Start a new coroutine for random stops
        randomStopJob = viewModelScope.launch {
            Log.d("MainViewModel", "Starting random stop timer with randomized intervals and durations")

            var iterationCount = 0
            val timerStartTime = System.currentTimeMillis()
            var lastInterruptionStartTime = 0L

            while (isActive && _randomStopsEnabled.value && _isSessionActive.value) {
                iterationCount++

                // Random delay between 30-60 seconds (30000-60000 ms)
                val baseDelayMs = (40000 + random.nextInt(30001)).toLong() // 30000 + 0-30000 = 30000-60000

                // Adjust the delay based on whether this is the first interruption or not
                val adjustedDelayMs = if (lastInterruptionStartTime == 0L) {
                    // First interruption - use full delay
                    baseDelayMs
                } else {
                    // Subsequent interruptions - calculate delay from last interruption START
                    val targetNextInterruptionTime = lastInterruptionStartTime + baseDelayMs
                    val currentTime = System.currentTimeMillis()
                    val remainingDelay = targetNextInterruptionTime - currentTime

                    Log.d("MainViewModel", "Iteration $iterationCount: Target interval from last START: ${baseDelayMs}ms")
                    Log.d("MainViewModel", "Time since last interruption started: ${currentTime - lastInterruptionStartTime}ms")
                    Log.d("MainViewModel", "Remaining delay needed: ${remainingDelay}ms")

                    // If we're already past the target time, trigger immediately
                    if (remainingDelay <= 0) {
                        Log.d("MainViewModel", "Already past target time, triggering immediately")
                        0L
                    } else {
                        remainingDelay
                    }
                }

                val beforeDelayTime = System.currentTimeMillis()
                Log.d("MainViewModel", "Iteration $iterationCount: Calculated delay = ${adjustedDelayMs}ms (${adjustedDelayMs/1000}s)")
                Log.d("MainViewModel", "Time since timer start: ${(beforeDelayTime - timerStartTime)/1000}s")

                if (adjustedDelayMs > 0) {
                    delay(adjustedDelayMs)
                }

                val afterDelayTime = System.currentTimeMillis()
                val actualDelayMs = afterDelayTime - beforeDelayTime
                Log.d("MainViewModel", "Iteration $iterationCount: Actual delay was ${actualDelayMs}ms (${actualDelayMs/1000}s)")

                // Record when this interruption starts
                lastInterruptionStartTime = afterDelayTime

                // Trigger a random stop if still enabled and session active
                if (_randomStopsEnabled.value && _isSessionActive.value) {
                    Log.d("MainViewModel", "Triggering random stop at ${(afterDelayTime - timerStartTime)/1000}s since timer start")
                    Log.d("MainViewModel", "Time since last interruption START: ${if (iterationCount > 1) (afterDelayTime - (lastInterruptionStartTime - actualDelayMs))/1000 else 0}s")
                    triggerRandomStop()
                }
            }
        }
    }

    // Stop the random stop timer
    private fun stopRandomStopTimer() {
        Log.d("MainViewModel", "Stopping random stop timer")
        randomStopJob?.cancel()
        randomStopJob = null
    }

    /**
     * Enhanced method to trigger random stop with timing tracking
     */
    private fun triggerRandomStop() {
        viewModelScope.launch {
            // Random duration between 15-30 seconds (15000-30000 ms)
            val stopDuration = (15000 + random.nextInt(15001)).toLong()

            // Calculate when in the current video the interruption occurred
            val videoTimeStamp = if (currentVideoStartTime > 0) {
                System.currentTimeMillis() - currentVideoStartTime
            } else {
                0L
            }

            val interruptionStartTime = System.currentTimeMillis()

            // Calculate time since last interruption END to current interruption START
            val timeSinceLastInterruption = if (lastInterruptionEndTime > 0) {
                interruptionStartTime - lastInterruptionEndTime
            } else {
                // First interruption - time since session start
                interruptionStartTime - sessionStartTime
            }

            // Store interruption data for current video (including the calculated time)
            currentInterruption = InterruptionData(
                startTime = interruptionStartTime,
                videoTimeStamp = videoTimeStamp,
                durationMs = stopDuration,
                timeSinceLastInterruptionMs = timeSinceLastInterruption
            )

            // Get the current video ID to associate the interruption with
            val currentVideo = currentVideoId

            // Log the stop event
            Log.d("MainViewModel", "Triggering random stop for ${stopDuration}ms on video: $currentVideo at timestamp: ${videoTimeStamp}ms")
            Log.d("MainViewModel", "Time since last interruption ended: ${timeSinceLastInterruption}ms")

            // Track the interruption in the behavior tracker (for backward compatibility)
            behaviorTracker.trackInterruption(stopDuration, currentVideo)

            // Save current playback state
            val wasPlaying = _isPlaying.value

            // Pause video playback
            _isPlaying.value = false

            // Show the overlay
            _isRandomStopActive.value = true

            // Wait for the random duration
            delay(stopDuration)

            // Hide the overlay
            _isRandomStopActive.value = false

            // Resume playback - videos should always be playing
            _isPlaying.value = true

            // Update the last interruption end time
            lastInterruptionEndTime = System.currentTimeMillis()

            Log.d("MainViewModel", "Random stop completed after ${stopDuration}ms, ended at: $lastInterruptionEndTime")
        }
    }

    fun likeCurrentVideo() {
        currentVideoId?.let { videoId ->
            likeVideo(videoId)
        }
    }

//    fun incrementVideoWatchCount(videoId: String) {
//        viewModelScope.launch {
//            repository.incrementVideoWatchCount(videoId)
//        }
//    }

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

            // Initialize consistent video numbering based on original order
            initializeVideoNumbering(folderVideos)

            // Randomize and set the videos for playback
            val randomizedVideos = folderVideos.shuffled()
            _videos.value = randomizedVideos

            _isLoading.value = false

            if (randomizedVideos.isNotEmpty()) {
                startVideoViewTracking(randomizedVideos.first().id)  // Don't increment watch count here
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
        endCurrentVideoTracking()
        endSession()

        // Force export if session was active (in case app was closed early)
        if (_isSessionActive.value || playByPlayEvents.isNotEmpty()) {
            Log.d("MainViewModel", "App closing - force exporting data")
            // Run export synchronously since we're in onCleared
            runBlocking {
                exportSessionData()
            }
        }

        super.onCleared()
    }

    fun unlikeVideo(videoId: String) {
        // Update the UI state by removing from the set
        val currentLikes = _likedVideos.value.toMutableSet()
        currentLikes.remove(videoId)
        _likedVideos.value = currentLikes

        // You might want to track this in behavior tracker too
        Log.d("MainViewModel", "Unliked video: $videoId")
    }

    /**
     * Get watch count for a video (cumulative across all sessions)
     */
    fun getWatchCount(videoId: String): Int {
        return _watchCounts.value[videoId] ?: 0
    }
}