package com.example.tiktokvasp.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.tiktokvasp.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Manages the data collection session, including timing, swipe pattern generation, and data export
 */
class SessionManager(
    private val context: Context,
    private val participantId: String,
    private val categoryFolder: String
) {
    private val handler = Handler(Looper.getMainLooper())
    private var sessionStartTime = 0L
    private var sessionEndTime = 0L
    private var sessionDurationMs = 0L
    private var isSessionActive = false
    private var autoGeneratePngs = false
    private var videoCounter = 0

    // Callbacks
    private var onSessionCompleteListener: (() -> Unit)? = null
    private var onTimerUpdateListener: ((Long) -> Unit)? = null

    // Runnable for timer updates
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isSessionActive) {
                val elapsedTime = System.currentTimeMillis() - sessionStartTime
                val remainingTime = max(0, sessionDurationMs - elapsedTime)

                onTimerUpdateListener?.invoke(remainingTime)

                if (remainingTime <= 0) {
                    endSession()
                } else {
                    // Update every second
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    /**
     * Start a new data collection session with specified duration
     */
    fun startSession(durationMinutes: Int, autoGenPngs: Boolean) {
        if (isSessionActive) {
            return
        }

        sessionStartTime = System.currentTimeMillis()
        sessionDurationMs = durationMinutes * 60 * 1000L
        isSessionActive = true
        autoGeneratePngs = autoGenPngs
        videoCounter = 0

        // Start the timer
        handler.post(timerRunnable)

        Log.d("SessionManager", "Started session for participant $participantId ($durationMinutes minutes)")
    }

    /**
     * End the current session and process the collected data
     */
    fun endSession() {
        if (!isSessionActive) {
            return
        }

        isSessionActive = false
        sessionEndTime = System.currentTimeMillis()
        handler.removeCallbacks(timerRunnable)

        // Notify listener
        onSessionCompleteListener?.invoke()

        Log.d("SessionManager", "Ended session for participant $participantId")
    }

    /**
     * Set callback for when session completes
     */
    fun setOnSessionCompleteListener(listener: () -> Unit) {
        this.onSessionCompleteListener = listener
    }

    /**
     * Set callback for timer updates
     */
    fun setOnTimerUpdateListener(listener: (Long) -> Unit) {
        this.onTimerUpdateListener = listener
    }

    /**
     * Track a video view, incrementing the video counter
     */
    fun trackVideoView(video: Video) {
        if (!isSessionActive) {
            return
        }

        videoCounter++
    }

    /**
     * Generate PNG image of a swipe pattern
     * Returns the file path of the generated image
     */
    suspend fun generateSwipePatternPng(swipeEvent: SwipeEvent): String = withContext(Dispatchers.IO) {
        try {
            // Create directory if it doesn't exist
            val directory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "$participantId/$categoryFolder/swipe_patterns"
            )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Create a timestamp for the filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val fileName = "swipe_${swipeEvent.direction}_${timestamp}.png"
            val file = File(directory, fileName)

            // Create a bitmap for the swipe pattern
            val padding = 50
            val width = swipeEvent.screenWidth
            val height = swipeEvent.screenHeight

            // Draw the swipe pattern
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Fill background
            canvas.drawColor(Color.BLACK)

            // Setup paint for the path
            val pathPaint = Paint().apply {
                color = when (swipeEvent.direction) {
                    SwipeDirection.UP -> Color.MAGENTA
                    SwipeDirection.DOWN -> Color.CYAN
                    SwipeDirection.LEFT -> Color.YELLOW
                    SwipeDirection.RIGHT -> Color.GREEN
                }
                style = Paint.Style.STROKE
                strokeWidth = 8f
                isAntiAlias = true
                alpha = 180 // Semi-transparent
            }

            // Draw the path
            val path = Path()
            if (swipeEvent.path.isNotEmpty()) {
                path.moveTo(swipeEvent.path.first().x, swipeEvent.path.first().y)

                swipeEvent.path.drop(1).forEach { point ->
                    path.lineTo(point.x, point.y)
                }

                canvas.drawPath(path, pathPaint)
            }

            // Draw start and end points
            val startPointPaint = Paint().apply {
                color = Color.GREEN
                style = Paint.Style.FILL
            }

            val endPointPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
            }

            canvas.drawCircle(swipeEvent.startX, swipeEvent.startY, 12f, startPointPaint)
            canvas.drawCircle(swipeEvent.endX, swipeEvent.endY, 12f, endPointPaint)

            // Save the bitmap to a file
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            return@withContext file.absolutePath

        } catch (e: Exception) {
            Log.e("SessionManager", "Error generating swipe pattern PNG", e)
            return@withContext ""
        }
    }

    /**
     * Checks if auto-generation of PNGs is enabled
     */
    fun isAutoGeneratePngsEnabled(): Boolean {
        return autoGeneratePngs && isSessionActive
    }

    /**
     * Get the current video counter value
     */
    fun getCurrentVideoNumber(): Int {
        return videoCounter
    }

    /**
     * Check if a session is currently active
     */
    fun isSessionActive(): Boolean {
        return isSessionActive
    }

    /**
     * Get the remaining time in the session (milliseconds)
     */
    fun getRemainingTimeMs(): Long {
        if (!isSessionActive) {
            return 0
        }

        val elapsedTime = System.currentTimeMillis() - sessionStartTime
        return max(0, sessionDurationMs - elapsedTime)
    }
}