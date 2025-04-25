package com.example.tiktokvasp.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.example.tiktokvasp.tracking.SwipeDirection
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.tracking.SwipePoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Enhanced swipe detector specifically designed for TikTok-style applications
 * that captures extremely detailed information about swipe gestures.
 */
class TikTokSwipeDetector(
    private val context: Context
) : View.OnTouchListener, SensorEventListener {

    interface OnSwipeListener {
        fun onSwipeUp()
        fun onSwipeDown()
        fun onSwipeLeft()
        fun onSwipeRight()
        fun onSingleTap()
        fun onDoubleTap()
        fun onLongPress()
        fun onDetailedSwipeDetected(swipeEvent: SwipeEvent)
    }

    private var listener: OnSwipeListener? = null
    private val gestureDetector: GestureDetector
    private var velocityTracker: VelocityTracker? = null
    private var currentVideoId: String? = null

    // Screen dimensions
    private val screenWidth = context.resources.displayMetrics.widthPixels
    private val screenHeight = context.resources.displayMetrics.heightPixels

    // Tracking state
    private var isTracking = false
    private var startX = 0f
    private var startY = 0f
    private var startTime = 0L
    private val swipePoints = mutableListOf<SwipePoint>()

    // Sensor data for additional context
    private var deviceRotationAngle = 0f
    private var deviceAccelerationMagnitude = 0f
    private val sensorManager by lazy { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val accelerometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    private val rotationSensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    // Configuration constants
    private val SWIPE_THRESHOLD = 80f
    private val SWIPE_VELOCITY_THRESHOLD = 100f
    private val TRACKING_INTERVAL_MS = 5L  // Track points every 5ms for high-resolution data

    init {
        gestureDetector = GestureDetector(context, GestureListener())
        registerSensors()
    }

    fun setOnSwipeListener(listener: OnSwipeListener) {
        this.listener = listener
    }

    fun setCurrentVideoId(videoId: String) {
        this.currentVideoId = videoId
    }

    private fun registerSensors() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (event == null) return false

        // 1) Debug every call, showing the masked action
        Log.d("SwipeDetector", "onTouch() view=$view actionMasked=${event.actionMasked} raw=(${event.rawX},${event.rawY})")

        // velocity tracking (unchanged)
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        // 2) Use actionMasked so we match DOWN, MOVE, UP correctly
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTracking = true
                startX = event.rawX
                startY = event.rawY
                startTime = System.currentTimeMillis()
                swipePoints.clear()
                Log.d("SwipeDetector", "→ ACTION_DOWN, starting swipe at ($startX,$startY)")
                addSwipePoint(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTracking) {
                    val lastTime = swipePoints.lastOrNull()?.timestamp ?: 0L
                    if (System.currentTimeMillis() - lastTime >= TRACKING_INTERVAL_MS) {
                        Log.d("SwipeDetector", "→ ACTION_MOVE, adding point")
                        addSwipePoint(event)
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (isTracking) {
                    Log.d("SwipeDetector", "→ ACTION_UP/CANCEL, finishing swipe")
                    addSwipePoint(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f

                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    val dist = sqrt(dx*dx + dy*dy)
                    if (dist > SWIPE_THRESHOLD) {
                        val dir = calculateSwipeDirection(dx, dy)
                        createDetailedSwipeEvent(
                            endX = event.rawX,
                            endY = event.rawY,
                            velocityX = vx,
                            velocityY = vy,
                            direction = dir
                        )
                        when (dir) {
                            SwipeDirection.UP    -> listener?.onSwipeUp()
                            SwipeDirection.DOWN  -> listener?.onSwipeDown()
                            SwipeDirection.LEFT  -> listener?.onSwipeLeft()
                            SwipeDirection.RIGHT -> listener?.onSwipeRight()
                        }
                    }

                    isTracking = false
                    velocityTracker?.recycle()
                    velocityTracker = null
                }
            }
        }

        // Let GestureDetector handle taps, but always consume so we keep getting MOVE/UP
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun addSwipePoint(event: MotionEvent) {
        val px = event.rawX
        val py = event.rawY
        val p = SwipePoint(
            x = px,
            y = py,
            timestamp = System.currentTimeMillis(),
            pressure = event.pressure
        )
        swipePoints.add(p)
        // debug log to prove points are non-zero now
        Log.d("SwipeDetector", "pt: x=$px y=$py pressure=${event.pressure}")
    }


    private fun calculateSwipeDirection(deltaX: Float, deltaY: Float): SwipeDirection {
        val angle = atan2(deltaY, deltaX) * 180 / Math.PI

        return when {
            angle < -45 && angle > -135 -> SwipeDirection.UP
            angle > 45 && angle < 135 -> SwipeDirection.DOWN
            abs(angle) <= 45 -> SwipeDirection.RIGHT
            else -> SwipeDirection.LEFT
        }
    }

    private fun createDetailedSwipeEvent(
        endX: Float,
        endY: Float,
        velocityX: Float,
        velocityY: Float,
        direction: SwipeDirection
    ) {
        if (currentVideoId == null || swipePoints.size < 2) return

        // Calculate swipe distance
        val deltaX = endX - startX
        val deltaY = endY - startY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

        // Calculate duration
        val duration = System.currentTimeMillis() - startTime

        // Create the detailed swipe event
        val swipeEvent = SwipeEvent(
            id = java.util.UUID.randomUUID().toString(),
            sessionId = "",  // Will be populated by the tracker
            timestamp = startTime,
            videoId = currentVideoId!!,
            direction = direction,
            swipeDurationMs = duration,
            startX = startX,
            startY = startY,
            endX = endX,
            endY = endY,
            velocityX = velocityX,
            velocityY = velocityY,
            distance = distance,
            path = swipePoints.toList(),  // Make a copy of the path points
            pressure = swipePoints.mapNotNull { it.pressure }.average().toFloat(),
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )

        // Notify the listener
        listener?.onDetailedSwipeDetected(swipeEvent)
    }

    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Calculate acceleration magnitude (excluding gravity)
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                deviceAccelerationMagnitude = sqrt(x*x + y*y + z*z)
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
                // Extract device rotation angle
                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)

                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                // First value is azimuth (angle around the z-axis)
                deviceRotationAngle = orientationAngles[0] * 180f / Math.PI.toFloat()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            listener?.onSingleTap()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            listener?.onDoubleTap()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            listener?.onLongPress()
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Let the main onTouch handler deal with flings/swipes
            return false
        }
    }
}