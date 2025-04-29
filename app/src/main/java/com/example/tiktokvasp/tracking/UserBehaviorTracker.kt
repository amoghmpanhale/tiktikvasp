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

    // Add maps to track likes and shares by video ID
    private val videoLikes = mutableMapOf<String, Boolean>()
    private val videoShares = mutableMapOf<String, Boolean>()

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
            isShared = videoShares[videoId] ?: false  // Include share status
        )

        viewEvents.add(event)
        logEvent("View: Video: $videoId, Watched: $watchDurationMs ms (${watchPercentage * 100}%), Liked: ${event.isLiked}, Shared: ${event.isShared}")
    }

    // Add functions to track likes and shares
    fun trackVideoLike(videoId: String) {
        videoLikes[videoId] = true
        logEvent("Like: Video: $videoId")
    }

    fun trackVideoShare(videoId: String) {
        videoShares[videoId] = true
        logEvent("Share: Video: $videoId")
    }

    // Add functions to check if a video is liked or shared
    fun isVideoLiked(videoId: String): Boolean {
        return videoLikes[videoId] ?: false
    }

    fun isVideoShared(videoId: String): Boolean {
        return videoShares[videoId] ?: false
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
    val isLiked: Boolean = false,  // Add this property
    val isShared: Boolean = false  // Add this property
)