package com.example.tiktokvasp.tracking

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UserBehaviorTracker {

    private val sessionId = UUID.randomUUID().toString()
    private var swipeEvents = mutableListOf<SwipeEvent>()
    private var viewEvents = mutableListOf<ViewEvent>()

    // Add maps to track likes, shares, and comments by video ID
    private val videoLikes = mutableMapOf<String, Boolean>()
    private val videoShares = mutableMapOf<String, Boolean>()
    private val videoComments = mutableMapOf<String, Boolean>()

    // Track interruption events
    private var interruptionEvents = mutableListOf<InterruptionEvent>()

    /**
    * Track a random interruption event
    */
    fun trackInterruption(durationMs: Long, videoId: String?) {
        val event = InterruptionEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            durationMs = durationMs,
            videoIdWhenOccurred = videoId
        )

        interruptionEvents.add(event)
        logEvent("Interruption: Duration: ${durationMs}ms, Video: $videoId")
    }

    /**
     * Get all interruption events
     */
    fun getInterruptionEvents(): List<InterruptionEvent> = interruptionEvents.toList()

    /**
     * Get interruption for a specific video (if any occurred during that video)
     */
    fun getInterruptionForVideo(videoId: String): InterruptionEvent? {
        return interruptionEvents.find { it.videoIdWhenOccurred == videoId }
    }

    /**
     * Clear interruption events
     */
    fun clearInterruptionEvents() {
        interruptionEvents.clear()
    }

    /**
     * Track detailed swipe information from the EnhancedSwipeTracker
     */
    fun trackDetailedSwipe(swipeEvent: SwipeEvent) {
        // Set the session ID for this event
        val eventWithSession = swipeEvent.copy(
            sessionId = sessionId
        )

        swipeEvents.add(eventWithSession)

        // Log basic information about the swipe
        logEvent(
            "Detailed Swipe: ${eventWithSession.direction}, " +
                    "Video: ${eventWithSession.videoId}, " +
                    "Duration: ${eventWithSession.swipeDurationMs} ms, " +
                    "Velocity: (${eventWithSession.velocityX.toInt()}, ${eventWithSession.velocityY.toInt()}) px/s, " +
                    "Distance: ${eventWithSession.distance.toInt()} px"
        )
    }

    /**
     * Legacy method to track basic swipe information
     * You can use this as a fallback or remove it if fully replaced by trackDetailedSwipe
     */
    fun trackSwipe(direction: SwipeDirection, videoId: String, durationMs: Long) {
        // Create a simple SwipeEvent with minimal data for backward compatibility
        val pathPoints = listOf(
            SwipePoint(0f, 0f, System.currentTimeMillis(), 0f),
            SwipePoint(0f, 0f, System.currentTimeMillis() + durationMs, 0f)
        )

        val event = SwipeEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            videoId = videoId,
            direction = direction,
            swipeDurationMs = durationMs,
            startX = 0f,
            startY = 0f,
            endX = 0f,
            endY = 0f,
            velocityX = 0f,
            velocityY = 0f,
            distance = 0f,
            path = pathPoints,
            pressure = 0f,
            screenWidth = 0,
            screenHeight = 0
        )

        swipeEvents.add(event)
        logEvent("Basic Swipe: $direction, Video: $videoId, Duration: $durationMs ms")
    }

    fun trackVideoView(videoId: String, watchDurationMs: Long, watchPercentage: Float) {
        val event = ViewEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            videoId = videoId,
            watchDurationMs = watchDurationMs,
            watchPercentage = watchPercentage,
            isLiked = videoLikes[videoId] ?: false,  // Include like status
            isShared = videoShares[videoId] ?: false,  // Include share status
            hasCommented = videoComments[videoId] ?: false  // Include comment status
        )

        viewEvents.add(event)
        logEvent("View: Video: $videoId, Watched: $watchDurationMs ms (${watchPercentage * 100}%), Liked: ${event.isLiked}, Shared: ${event.isShared}, Commented: ${event.hasCommented}")
    }

    // Add functions to track likes, shares, and comments
    fun trackVideoLike(videoId: String) {
        videoLikes[videoId] = true
        updateExistingViewEvents(videoId)
        logEvent("Like: Video: $videoId")
    }

    fun trackVideoUnlike(videoId: String) {
        videoLikes[videoId] = false
        updateExistingViewEvents(videoId)
        logEvent("Unlike: Video: $videoId")
    }

    fun trackVideoShare(videoId: String) {
        videoShares[videoId] = true
        updateExistingViewEvents(videoId)
        logEvent("Share: Video: $videoId")
    }

    fun trackVideoComment(videoId: String) {
        videoComments[videoId] = true
        updateExistingViewEvents(videoId)
        logEvent("Comment: Video: $videoId")
    }

    // Update existing view events when likes, shares, or comments change
    private fun updateExistingViewEvents(videoId: String) {
        // Find the most recent view event for this video and update it
        val latestViewEvent = viewEvents.lastOrNull { it.videoId == videoId }
        latestViewEvent?.let { event ->
            // Create an updated copy of the event
            val updatedEvent = event.copy(
                isLiked = videoLikes[videoId] ?: false,
                isShared = videoShares[videoId] ?: false,
                hasCommented = videoComments[videoId] ?: false
            )

            // Replace the old event with the updated one
            val index = viewEvents.indexOf(event)
            if (index >= 0) {
                viewEvents[index] = updatedEvent
            }
        }
    }

    // Add functions to check if a video is liked, shared, or commented on
    fun isVideoLiked(videoId: String): Boolean {
        return videoLikes[videoId] ?: false
    }

    fun isVideoShared(videoId: String): Boolean {
        return videoShares[videoId] ?: false
    }

    fun hasCommentedOnVideo(videoId: String): Boolean {
        return videoComments[videoId] ?: false
    }

    private fun logEvent(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        Log.d("BehaviorTracker", "[$timestamp] $message")
    }

    fun getSwipeEvents(): List<SwipeEvent> = swipeEvents.toList()

    fun getViewEvents(): List<ViewEvent> = viewEvents.toList()

    fun clearEvents() {
        swipeEvents.clear()
        viewEvents.clear()
    }
}

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

data class SwipeEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val videoId: String,
    val direction: SwipeDirection,
    val swipeDurationMs: Long,
    val startX: Float,             // Starting X coordinate
    val startY: Float,             // Starting Y coordinate
    val endX: Float,               // Ending X coordinate
    val endY: Float,               // Ending Y coordinate
    val velocityX: Float,          // Horizontal velocity in pixels per second
    val velocityY: Float,          // Vertical velocity in pixels per second
    val distance: Float,           // Total distance traveled
    val path: List<SwipePoint>,    // Full path of the swipe as a series of points
    val pressure: Float,           // Average touch pressure (if available)
    val screenWidth: Int,          // Screen width for normalization
    val screenHeight: Int,          // Screen height for normalization
    var velocities: List<Float> = emptyList(), // Velocity at each point
    var accelerations: List<Float> = emptyList(), // Acceleration at each point
    var timeElapsed: List<Long> = emptyList() // Time elapsed between points
)

// Point along the swipe path with timestamp for temporal analysis
data class SwipePoint(
    val x: Float,
    val y: Float,
    val timestamp: Long,
    val pressure: Float?  // Optional pressure value
)

data class ViewEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val videoId: String,
    val watchDurationMs: Long,
    val watchPercentage: Float,
    val isLiked: Boolean = false,
    val isShared: Boolean = false,
    val hasCommented: Boolean = false  // Added comment status
)

data class InterruptionEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val durationMs: Long,
    val videoIdWhenOccurred: String? // The video that was playing when interruption started
)