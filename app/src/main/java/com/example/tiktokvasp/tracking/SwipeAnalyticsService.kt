package com.example.tiktokvasp.tracking

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced analytics service that processes and analyzes swipe data
 * to extract meaningful metrics and patterns
 */
class SwipeAnalyticsService {

    /**
     * Extract key metrics from a swipe event
     */
    fun analyzeSwipe(swipeEvent: SwipeEvent): SwipeAnalytics {
        // Extract path data and calculate metrics
        val path = swipeEvent.path

        // Skip if we don't have enough points
        if (path.size < 2) {
            return SwipeAnalytics(
                swipeId = swipeEvent.id,
                straightness = 1.0f,
                smoothness = 1.0f,
                acceleration = 0.0f,
                jerk = 0.0f,
                peakVelocity = abs(swipeEvent.velocityY),
                averageVelocity = abs(swipeEvent.velocityY),
                durationMs = swipeEvent.swipeDurationMs,
                speedConsistency = 1.0f
            )
        }

        // Calculate straightness (ratio of direct distance to actual path length)
        val directDistance = sqrt(
            (path.last().x - path.first().x).pow(2) +
                    (path.last().y - path.first().y).pow(2)
        )

        var pathLength = 0.0f
        for (i in 1 until path.size) {
            pathLength += sqrt(
                (path[i].x - path[i-1].x).pow(2) +
                        (path[i].y - path[i-1].y).pow(2)
            )
        }

        val straightness = if (pathLength > 0) directDistance / pathLength else 1.0f

        // Calculate smoothness (less angular changes = smoother path)
        var angleChanges = 0.0f
        if (path.size >= 3) {
            for (i in 2 until path.size) {
                val prevVector = Pair(
                    path[i-1].x - path[i-2].x,
                    path[i-1].y - path[i-2].y
                )

                val currentVector = Pair(
                    path[i].x - path[i-1].x,
                    path[i].y - path[i-1].y
                )

                // Calculate angle change using dot product
                val dotProduct = prevVector.first * currentVector.first +
                        prevVector.second * currentVector.second

                val prevMagnitude = sqrt(prevVector.first.pow(2) + prevVector.second.pow(2))
                val currentMagnitude = sqrt(currentVector.first.pow(2) + currentVector.second.pow(2))

                // Avoid division by zero
                if (prevMagnitude > 0 && currentMagnitude > 0) {
                    val cosAngle = dotProduct / (prevMagnitude * currentMagnitude)
                    // Clamp to valid range for acos
                    val clampedCosAngle = cosAngle.coerceIn(-1.0f, 1.0f)
                    val angle = Math.acos(clampedCosAngle.toDouble()).toFloat()
                    angleChanges += angle
                }
            }
        }

        // Normalize smoothness (lower angles = smoother)
        val maxPossibleAngleChanges = (path.size - 2) * Math.PI.toFloat()
        val smoothness = if (maxPossibleAngleChanges > 0)
            1.0f - (angleChanges / maxPossibleAngleChanges) else 1.0f

        // Calculate velocity at each point
        val velocities = mutableListOf<Float>()
        for (i in 1 until path.size) {
            val dx = path[i].x - path[i-1].x
            val dy = path[i].y - path[i-1].y
            val dt = (path[i].timestamp - path[i-1].timestamp).toFloat()

            if (dt > 0) {
                val distance = sqrt(dx.pow(2) + dy.pow(2))
                val velocity = distance / dt * 1000 // Convert to pixels per second
                velocities.add(velocity)
            }
        }

        // Calculate acceleration at each point
        val accelerations = mutableListOf<Float>()
        for (i in 1 until velocities.size) {
            val dv = velocities[i] - velocities[i-1]
            val dt = (path[i+1].timestamp - path[i].timestamp).toFloat()

            if (dt > 0) {
                val acceleration = dv / dt * 1000 // Convert to pixels/sec²
                accelerations.add(acceleration)
            }
        }

        // Calculate jerk (rate of change of acceleration)
        val jerks = mutableListOf<Float>()
        for (i in 1 until accelerations.size) {
            val da = accelerations[i] - accelerations[i-1]
            val dt = (path[i+2].timestamp - path[i+1].timestamp).toFloat()

            if (dt > 0) {
                val jerk = da / dt * 1000 // Convert to pixels/sec³
                jerks.add(jerk)
            }
        }

        // Calculate average metrics
        val avgVelocity = if (velocities.isNotEmpty()) velocities.average().toFloat() else 0.0f
        val avgAcceleration = if (accelerations.isNotEmpty()) accelerations.average().toFloat() else 0.0f
        val avgJerk = if (jerks.isNotEmpty()) jerks.average().toFloat() else 0.0f

        // Find peak velocity
        val peakVelocity = velocities.maxOrNull() ?: 0.0f

        // Calculate speed consistency (standard deviation of velocity)
        var speedVariance = 0.0f
        if (velocities.isNotEmpty()) {
            val mean = velocities.average().toFloat()
            speedVariance = velocities.map { (it - mean).pow(2) }.sum() / velocities.size
        }
        val speedStdDev = sqrt(speedVariance)

        // Normalize speed consistency (lower variation = more consistent)
        val speedConsistency = if (avgVelocity > 0)
            1.0f - (speedStdDev / avgVelocity).coerceIn(0.0f, 1.0f) else 1.0f

        return SwipeAnalytics(
            swipeId = swipeEvent.id,
            straightness = straightness,
            smoothness = smoothness,
            acceleration = avgAcceleration,
            jerk = avgJerk,
            peakVelocity = peakVelocity,
            averageVelocity = avgVelocity,
            durationMs = swipeEvent.swipeDurationMs,
            speedConsistency = speedConsistency
        )
    }

    /**
     * Analyze a batch of swipe events and aggregate the results
     */
    fun analyzeSwipeBatch(events: List<SwipeEvent>): BatchAnalytics {
        val analytics = events.map { analyzeSwipe(it) }

        // Skip if no events
        if (analytics.isEmpty()) {
            return BatchAnalytics(
                swipeCount = 0,
                avgStraightness = 0.0f,
                avgSmoothness = 0.0f,
                avgVelocity = 0.0f,
                avgAcceleration = 0.0f,
                avgDurationMs = 0,
                directionDistribution = mapOf()
            )
        }

        // Calculate averages
        val avgStraightness = analytics.map { it.straightness }.average().toFloat()
        val avgSmoothness = analytics.map { it.smoothness }.average().toFloat()
        val avgVelocity = analytics.map { it.averageVelocity }.average().toFloat()
        val avgAcceleration = analytics.map { it.acceleration }.average().toFloat()
        val avgDurationMs = analytics.map { it.durationMs }.average().toLong()

        // Calculate direction distribution
        val directions = events.groupBy { it.direction }
            .mapValues { (_, events) -> events.size.toFloat() / events.size }

        return BatchAnalytics(
            swipeCount = events.size,
            avgStraightness = avgStraightness,
            avgSmoothness = avgSmoothness,
            avgVelocity = avgVelocity,
            avgAcceleration = avgAcceleration,
            avgDurationMs = avgDurationMs,
            directionDistribution = directions
        )
    }

    /**
     * Analyze user behavior over time by looking at patterns in swipe data
     */
    fun analyzeUserBehavior(events: List<SwipeEvent>): UserBehaviorAnalytics {
        if (events.isEmpty()) {
            return UserBehaviorAnalytics(
                swipeCount = 0,
                swipesPerMinute = 0.0f,
                dominantDirection = null,
                consistencyScore = 0.0f,
                userStyle = "Unknown"
            )
        }

        // Group events by time to calculate swipes per minute
        val timeRange = events.maxByOrNull { it.timestamp }!!.timestamp -
                events.minByOrNull { it.timestamp }!!.timestamp

        val swipesPerMinute = if (timeRange > 0)
            events.size.toFloat() / (timeRange / 60000.0f) else 0.0f

        // Find dominant direction
        val directionCounts = events.groupBy { it.direction }
            .mapValues { (_, events) -> events.size }

        val dominantDirection = directionCounts.maxByOrNull { it.value }?.key

        // Calculate consistency score based on various metrics
        val analytics = events.map { analyzeSwipe(it) }

        val straightnessConsistency = calculateConsistency(analytics.map { it.straightness })
        val smoothnessConsistency = calculateConsistency(analytics.map { it.smoothness })
        val velocityConsistency = calculateConsistency(analytics.map { it.averageVelocity })

        val consistencyScore = (straightnessConsistency + smoothnessConsistency + velocityConsistency) / 3.0f

        // Determine user style based on metrics
        val avgVelocity = analytics.map { it.averageVelocity }.average().toFloat()
        val avgSmoothness = analytics.map { it.smoothness }.average().toFloat()

        val userStyle = when {
            avgVelocity > 2000 && avgSmoothness > 0.8 -> "Fast and precise"
            avgVelocity > 2000 && avgSmoothness <= 0.8 -> "Fast but erratic"
            avgVelocity <= 2000 && avgSmoothness > 0.8 -> "Careful and precise"
            else -> "Casual browser"
        }

        return UserBehaviorAnalytics(
            swipeCount = events.size,
            swipesPerMinute = swipesPerMinute,
            dominantDirection = dominantDirection,
            consistencyScore = consistencyScore,
            userStyle = userStyle
        )
    }

    /**
     * Calculate consistency of a metric (lower coefficient of variation = more consistent)
     */
    private fun calculateConsistency(values: List<Float>): Float {
        if (values.isEmpty()) return 0.0f

        val mean = values.average().toFloat()
        if (mean == 0.0f) return 1.0f // Avoid division by zero

        val variance = values.map { (it - mean).pow(2) }.sum() / values.size
        val stdDev = sqrt(variance)

        // Lower coefficient of variation = higher consistency
        return 1.0f - (stdDev / mean).coerceIn(0.0f, 1.0f)
    }
}

/**
 * Analytics for a single swipe event
 */
data class SwipeAnalytics(
    val swipeId: String,
    val straightness: Float,     // 0-1 (0 = very curved, 1 = perfectly straight)
    val smoothness: Float,       // 0-1 (0 = very jerky, 1 = perfectly smooth)
    val acceleration: Float,     // Average acceleration in pixels/second²
    val jerk: Float,             // Rate of change of acceleration
    val peakVelocity: Float,     // Maximum velocity during swipe
    val averageVelocity: Float,  // Average velocity during swipe
    val durationMs: Long,        // Duration of swipe in milliseconds
    val speedConsistency: Float  // 0-1 (0 = very inconsistent speed, 1 = perfectly consistent)
)

/**
 * Analytics for a batch of swipe events
 */
data class BatchAnalytics(
    val swipeCount: Int,
    val avgStraightness: Float,
    val avgSmoothness: Float,
    val avgVelocity: Float,
    val avgAcceleration: Float,
    val avgDurationMs: Long,
    val directionDistribution: Map<SwipeDirection, Float>
)

/**
 * Analytics for user behavior based on swipe patterns
 */
data class UserBehaviorAnalytics(
    val swipeCount: Int,
    val swipesPerMinute: Float,
    val dominantDirection: SwipeDirection?,
    val consistencyScore: Float,  // 0-1 (0 = very inconsistent, 1 = perfectly consistent)
    val userStyle: String         // Description of user's swiping style
)