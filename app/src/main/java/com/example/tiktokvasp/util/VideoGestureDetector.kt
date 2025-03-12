package com.example.tiktokvasp.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import kotlin.math.abs

/**
 * Advanced gesture detector for TikTok-style video interactions
 * Handles vertical swipes, taps, double taps, and provides visual feedback
 */
class VideoGestureDetector(
    context: Context,
    private val viewPagerContainer: View,
    private val playIndicator: ImageView,
    private val heartAnimation: ImageView
) {
    interface VideoGestureListener {
        fun onSingleTap()
        fun onDoubleTap(x: Float, y: Float)
        fun onSwipeUp()
        fun onSwipeDown()
        fun onSwipeLeft()
        fun onSwipeRight()
        fun onLongPress()
    }

    private var listener: VideoGestureListener? = null

    // Constants for gesture detection
    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100
    private val DOUBLE_TAP_TIMEOUT = 300L // milliseconds

    // State for double tap detection
    private var lastTapTime = 0L
    private var tapCount = 0

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            listener?.onSingleTap()
            showPlayIndicator()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            listener?.onDoubleTap(e.x, e.y)
            showHeartAnimation(e.x, e.y)
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
            if (e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            if (abs(diffY) > abs(diffX)) {
                // Vertical swipe
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        listener?.onSwipeDown()
                    } else {
                        listener?.onSwipeUp()
                    }
                    return true
                }
            } else {
                // Horizontal swipe
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        listener?.onSwipeRight()
                    } else {
                        listener?.onSwipeLeft()
                    }
                    return true
                }
            }

            return false
        }
    })

    fun setListener(listener: VideoGestureListener) {
        this.listener = listener
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private fun showPlayIndicator() {
        playIndicator.alpha = 1f

        ObjectAnimator.ofFloat(playIndicator, View.ALPHA, 1f, 0f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun showHeartAnimation(x: Float, y: Float) {
        // Position heart at the tap location
        heartAnimation.x = x - heartAnimation.width / 2
        heartAnimation.y = y - heartAnimation.height / 2

        // Animate the heart
        heartAnimation.scaleX = 0f
        heartAnimation.scaleY = 0f
        heartAnimation.alpha = 1f

        val scaleUpX = ObjectAnimator.ofFloat(heartAnimation, View.SCALE_X, 0f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(heartAnimation, View.SCALE_Y, 0f, 1.2f)

        scaleUpX.duration = 300
        scaleUpY.duration = 300
        scaleUpX.interpolator = AccelerateDecelerateInterpolator()
        scaleUpY.interpolator = AccelerateDecelerateInterpolator()

        scaleUpX.start()
        scaleUpY.start()

        scaleUpX.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val scaleDownX = ObjectAnimator.ofFloat(heartAnimation, View.SCALE_X, 1.2f, 1f)
                val scaleDownY = ObjectAnimator.ofFloat(heartAnimation, View.SCALE_Y, 1.2f, 1f)
                val fadeOut = ObjectAnimator.ofFloat(heartAnimation, View.ALPHA, 1f, 0f)

                scaleDownX.duration = 300
                scaleDownY.duration = 300
                fadeOut.duration = 300

                scaleDownX.start()
                scaleDownY.start()
                fadeOut.start()
            }
        })
    }
}