package com.example.tiktokvasp.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Custom touch listener that detects swipe gestures in a TikTok-like interface
 */
class TikTokSwipeDetector(context: Context) : View.OnTouchListener {

    interface OnSwipeListener {
        fun onSwipeUp()
        fun onSwipeDown()
        fun onSingleTap()
        fun onDoubleTap()
    }

    private var listener: OnSwipeListener? = null
    private val gestureDetector: GestureDetector

    private val SWIPE_THRESHOLD = 100
    private val SWIPE_VELOCITY_THRESHOLD = 100

    init {
        gestureDetector = GestureDetector(context, GestureListener())
    }

    fun setOnSwipeListener(listener: OnSwipeListener) {
        this.listener = listener
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        return event?.let { gestureDetector.onTouchEvent(it) } ?: false
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            listener?.onSingleTap()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            listener?.onDoubleTap()
            return true
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

            // Check if the swipe is primarily vertical
            if (abs(diffY) > abs(diffX)) {
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        listener?.onSwipeDown()
                    } else {
                        listener?.onSwipeUp()
                    }
                    return true
                }
            }

            return false
        }
    }
}