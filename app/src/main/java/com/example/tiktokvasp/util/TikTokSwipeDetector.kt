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
    private val velocityTracker = StableVelocityTracker()
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
    private val SWIPE_THRESHOLD = 0f
    private val SWIPE_VELOCITY_THRESHOLD = 0f
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

        val action = event.actionMasked
        Log.d("SwipeDetector", "onTouch action=$action raw=(${event.rawX},${event.rawY})")

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                isTracking = true
                startX = event.rawX
                startY = event.rawY
                startTime = System.currentTimeMillis()
                swipePoints.clear()
                velocityTracker.start(event.rawX, event.rawY, System.currentTimeMillis())
                Log.d("SwipeDetector", "→ DOWN at ($startX,$startY)")
                addSwipePoint(event)
            }

            MotionEvent.ACTION_MOVE -> if (isTracking) {
                val lastT = swipePoints.lastOrNull()?.timestamp ?: 0L
                if (System.currentTimeMillis() - lastT >= TRACKING_INTERVAL_MS) {
                    Log.d("SwipeDetector", "→ MOVE point")
                    addSwipePoint(event)
                }
                velocityTracker.update(event.rawX, event.rawY, System.currentTimeMillis())
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (isTracking) {
                Log.d("SwipeDetector", "→ UP/CANCEL, finishing")
                addSwipePoint(event)

                val dx = event.rawX - startX
                val dy = event.rawY - startY
                val dist = sqrt(dx * dx + dy * dy)
                Log.d("SwipeDetector", "   dist=$dist threshold=$SWIPE_THRESHOLD")

                val smoothedVelocity = velocityTracker.getSmoothedVelocity()

                val dir = calculateSwipeDirection(dx, dy)

                createDetailedSwipeEvent(
                    event.rawX,
                    event.rawY,
                    velocityX = velocityTracker.velocityX,
                    velocityY = velocityTracker.velocityY,
                    direction = dir
                )

                if (dist > SWIPE_THRESHOLD) {
                    if (dir == SwipeDirection.UP) listener?.onSwipeUp()
                    if (dir == SwipeDirection.DOWN) listener?.onSwipeDown()
                }

                isTracking = false
            }
        }

        gestureDetector.onTouchEvent(event)
        return false
    }

    private fun addSwipePoint(event: MotionEvent) {
        val px = event.rawX
        val py = event.rawY
        val p = SwipePoint(x=px, y=py, timestamp=System.currentTimeMillis(), pressure=event.pressure)
        swipePoints.add(p)
        Log.d("SwipeDetector","pt: x=$px y=$py pressure=${event.pressure}")
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
        // if we don't know what video we're on, drop it
        if (currentVideoId == null) return

        // DEBUG: always log how many points we collected
        Log.d("SwipeDetector", "createDetailedSwipeEvent: pts=${swipePoints.size} dir=$direction")

        // Calculate swipe distance & duration exactly as before
        val deltaX = endX - startX
        val deltaY = endY - startY
        val distance = sqrt(deltaX*deltaX + deltaY*deltaY)
        val duration = System.currentTimeMillis() - startTime

        // build the event (path may be just 2 points, but that's fine)
        val swipeEvent = SwipeEvent(
            id            = java.util.UUID.randomUUID().toString(),
            sessionId     = "",              // tracker will stamp this
            timestamp     = startTime,
            videoId       = currentVideoId!!,
            direction     = direction,
            swipeDurationMs = duration,
            startX        = startX,
            startY        = startY,
            endX          = endX,
            endY          = endY,
            velocityX     = velocityX,
            velocityY     = velocityY,
            distance      = distance,
            path          = swipePoints.toList(),
            pressure      = swipePoints.mapNotNull { it.pressure }.average().toFloat(),
            screenWidth   = screenWidth,
            screenHeight  = screenHeight
        )

        // fire it exactly once, unconditionally
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

        // Remove single tap functionality - videos should not be pausable
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Do nothing - remove tap to pause functionality
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