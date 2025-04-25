package com.example.tiktokvasp.util

import kotlin.math.sqrt

class StableVelocityTracker {
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastTime: Long = 0L

    private val velocitySamples = mutableListOf<Float>()
    private val maxSamples = 5

    var velocityX: Float = 0f
        private set
    var velocityY: Float = 0f
        private set

    fun start(x: Float, y: Float, time: Long) {
        lastX = x
        lastY = y
        lastTime = time
        velocitySamples.clear()
        velocityX = 0f
        velocityY = 0f
    }

    fun update(x: Float, y: Float, time: Long) {
        val dx = x - lastX
        val dy = y - lastY
        val dt = (time - lastTime).coerceAtLeast(1L) // prevent division by zero

        if (dt < 5) return // Ignore very fast noise

        val instantVelocityX = dx / dt * 1000 // pixels per second
        val instantVelocityY = dy / dt * 1000

        velocitySamples.add(hypot(instantVelocityX, instantVelocityY))
        if (velocitySamples.size > maxSamples) {
            velocitySamples.removeAt(0)
        }

        velocityX = instantVelocityX.coerceIn(-3000f, 3000f)
        velocityY = instantVelocityY.coerceIn(-3000f, 3000f)

        lastX = x
        lastY = y
        lastTime = time
    }

    fun getSmoothedVelocity(): Float {
        if (velocitySamples.isEmpty()) return 0f
        return velocitySamples.average().toFloat()
    }

    private fun hypot(x: Float, y: Float): Float {
        return sqrt(x * x + y * y)
    }
}
