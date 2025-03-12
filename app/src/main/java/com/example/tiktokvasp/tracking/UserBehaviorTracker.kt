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

    fun trackSwipe(direction: SwipeDirection, videoId: String, durationMs: Long) {
        val event = SwipeEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            videoId = videoId,
            direction = direction,
            swipeDurationMs = durationMs
        )

        swipeEvents.add(event)
        logEvent("Swipe: $direction, Video: $videoId, Duration: $durationMs ms")
    }

    fun trackVideoView(videoId: String, watchDurationMs: Long, watchPercentage: Float) {
        val event = ViewEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            videoId = videoId,
            watchDurationMs = watchDurationMs,
            watchPercentage = watchPercentage
        )

        viewEvents.add(event)
        logEvent("View: Video: $videoId, Watched: $watchDurationMs ms (${watchPercentage * 100}%)")
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
    UP, DOWN
}

data class SwipeEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val videoId: String,
    val direction: SwipeDirection,
    val swipeDurationMs: Long
)

data class ViewEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val videoId: String,
    val watchDurationMs: Long,
    val watchPercentage: Float
)