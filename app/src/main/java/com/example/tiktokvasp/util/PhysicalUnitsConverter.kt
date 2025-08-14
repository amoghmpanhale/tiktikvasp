package com.example.tiktokvasp.util

import android.content.Context
import android.util.DisplayMetrics
import kotlin.math.sqrt

/**
 * Utility class to convert pixel measurements to physical units (mm, meters)
 */
class PhysicalUnitsConverter(context: Context) {

    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics

    // Pixels per inch for this device
    private val xdpi = displayMetrics.xdpi
    private val ydpi = displayMetrics.ydpi

    // Conversion factors
    private val pixelsPerMmX = xdpi / 25.4f  // 25.4 mm per inch
    private val pixelsPerMmY = ydpi / 25.4f

    /**
     * Convert pixels to millimeters for X coordinate
     */
    fun pixelsToMmX(pixels: Float): Float {
        return pixels / pixelsPerMmX
    }

    /**
     * Convert pixels to millimeters for Y coordinate
     */
    fun pixelsToMmY(pixels: Float): Float {
        return pixels / pixelsPerMmY
    }

    /**
     * Convert pixel distance to millimeters (using average DPI)
     */
    fun pixelsToMm(pixels: Float): Float {
        val avgPixelsPerMm = (pixelsPerMmX + pixelsPerMmY) / 2f
        return pixels / avgPixelsPerMm
    }

    /**
     * Convert pixel distance to meters
     */
    fun pixelsToMeters(pixels: Float): Float {
        return pixelsToMm(pixels) / 1000f
    }

    /**
     * Convert pixel velocity (px/s) to mm/s
     */
    fun velocityPxToMmPerSecond(velocityPx: Float): Float {
        return pixelsToMm(velocityPx)
    }

    /**
     * Convert pixel velocity (px/s) to m/s
     */
    fun velocityPxToMetersPerSecond(velocityPx: Float): Float {
        return velocityPxToMmPerSecond(velocityPx) / 1000f
    }

    /**
     * Convert pixel acceleration (px/s²) to mm/s²
     */
    fun accelerationPxToMmPerSecondSquared(accelerationPx: Float): Float {
        return pixelsToMm(accelerationPx)
    }

    /**
     * Convert pixel acceleration (px/s²) to m/s²
     */
    fun accelerationPxToMetersPerSecondSquared(accelerationPx: Float): Float {
        return accelerationPxToMmPerSecondSquared(accelerationPx) / 1000f
    }

    /**
     * Get device DPI information for debugging/logging
     */
    fun getDeviceInfo(): String {
        return "Device DPI: X=${xdpi}, Y=${ydpi}, Density=${displayMetrics.density}, " +
                "Screen: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels}px"
    }
}