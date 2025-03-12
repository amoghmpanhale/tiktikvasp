package com.example.tiktokvasp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiktokvasp.tracking.BatchAnalytics
import com.example.tiktokvasp.tracking.DataExporter
import com.example.tiktokvasp.tracking.SwipeAnalytics
import com.example.tiktokvasp.tracking.SwipeAnalyticsService
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.tracking.UserBehaviorAnalytics
import com.example.tiktokvasp.tracking.UserBehaviorTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.LinkedList

/**
 * ViewModel for the debug analytics screen that processes and provides swipe data
 */
class DebugViewModel(application: Application) : AndroidViewModel(application) {

    private val dataExporter = DataExporter(application)
    private val analyticsService = SwipeAnalyticsService()

    // Keep track of the most recent swipe events (limit to avoid memory issues)
    private val recentSwipeEvents = LinkedList<SwipeEvent>()
    private val MAX_STORED_EVENTS = 50

    // Map of swipe events to their analytics
    private val swipeAnalyticsMap = mutableMapOf<String, SwipeAnalytics>()

    // Expose data through StateFlows
    private val _swipeEvents = MutableStateFlow<List<SwipeEvent>>(emptyList())
    val swipeEvents: StateFlow<List<SwipeEvent>> = _swipeEvents.asStateFlow()

    private val _batchAnalytics = MutableStateFlow(
        BatchAnalytics(
            swipeCount = 0,
            avgStraightness = 0.0f,
            avgSmoothness = 0.0f,
            avgVelocity = 0.0f,
            avgAcceleration = 0.0f,
            avgDurationMs = 0,
            directionDistribution = mapOf()
        )
    )
    val batchAnalytics: StateFlow<BatchAnalytics> = _batchAnalytics.asStateFlow()

    private val _userBehaviorAnalytics = MutableStateFlow(
        UserBehaviorAnalytics(
            swipeCount = 0,
            swipesPerMinute = 0.0f,
            dominantDirection = null,
            consistencyScore = 0.0f,
            userStyle = "Not enough data"
        )
    )
    val userBehaviorAnalytics: StateFlow<UserBehaviorAnalytics> = _userBehaviorAnalytics.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    /**
     * Add a new swipe event to the analytics system
     */
    fun trackSwipeEvent(event: SwipeEvent) {
        // Add to recent events queue
        recentSwipeEvents.addFirst(event)

        // Keep queue size limited
        if (recentSwipeEvents.size > MAX_STORED_EVENTS) {
            recentSwipeEvents.removeLast()
        }

        // Update the events StateFlow
        _swipeEvents.value = recentSwipeEvents.toList()

        // Analyze the swipe event
        val analytics = analyticsService.analyzeSwipe(event)
        swipeAnalyticsMap[event.id] = analytics

        // Update batch analytics
        updateBatchAnalytics()

        // Update user behavior analytics
        updateUserBehaviorAnalytics()
    }

    /**
     * Get the analytics for a specific swipe event
     */
    fun getSwipeAnalytics(event: SwipeEvent): SwipeAnalytics? {
        return swipeAnalyticsMap[event.id] ?: run {
            // If not cached, compute it now
            val analytics = analyticsService.analyzeSwipe(event)
            swipeAnalyticsMap[event.id] = analytics
            analytics
        }
    }

    /**
     * Update batch analytics using all recent events
     */
    private fun updateBatchAnalytics() {
        _batchAnalytics.value = analyticsService.analyzeSwipeBatch(recentSwipeEvents.toList())
    }

    /**
     * Update user behavior analytics
     */
    private fun updateUserBehaviorAnalytics() {
        _userBehaviorAnalytics.value = analyticsService.analyzeUserBehavior(recentSwipeEvents.toList())
    }

    /**
     * Export the swipe data to files
     */
    fun exportSwipeData() {
        viewModelScope.launch {
            _isExporting.value = true

            // Export both JSON and CSV formats
            dataExporter.exportSwipeEvents(recentSwipeEvents.toList())
            dataExporter.exportSwipeEventsAsCSV(recentSwipeEvents.toList())

            // Add a small delay to show the exporting state
            delay(1000)

            _isExporting.value = false
        }
    }

    /**
     * Load swipe events from the tracking system
     */
    fun loadEventsFromTracker(tracker: UserBehaviorTracker) {
        val events = tracker.getSwipeEvents()

        // Only take the last MAX_STORED_EVENTS
        val recentEvents = events.takeLast(MAX_STORED_EVENTS).reversed()

        recentSwipeEvents.clear()
        recentSwipeEvents.addAll(recentEvents)

        // Update the events StateFlow
        _swipeEvents.value = recentEvents

        // Analyze all events
        recentEvents.forEach { event ->
            val analytics = analyticsService.analyzeSwipe(event)
            swipeAnalyticsMap[event.id] = analytics
        }

        // Update analytics
        updateBatchAnalytics()
        updateUserBehaviorAnalytics()
    }

    /**
     * Clear all stored data
     */
    fun clearData() {
        recentSwipeEvents.clear()
        swipeAnalyticsMap.clear()
        _swipeEvents.value = emptyList()

        // Reset analytics
        _batchAnalytics.value = BatchAnalytics(
            swipeCount = 0,
            avgStraightness = 0.0f,
            avgSmoothness = 0.0f,
            avgVelocity = 0.0f,
            avgAcceleration = 0.0f,
            avgDurationMs = 0,
            directionDistribution = mapOf()
        )

        _userBehaviorAnalytics.value = UserBehaviorAnalytics(
            swipeCount = 0,
            swipesPerMinute = 0.0f,
            dominantDirection = null,
            consistencyScore = 0.0f,
            userStyle = "Not enough data"
        )
    }
}