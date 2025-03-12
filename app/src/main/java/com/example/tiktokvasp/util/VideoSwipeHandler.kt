package com.example.tiktokvasp.util

import android.view.MotionEvent
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.example.tiktokvasp.adapters.VideoAdapter
import com.example.tiktokvasp.model.Video
import kotlin.math.abs

/**
 * Handles swipe gestures for a TikTok-like video feed.
 * This class detects and processes vertical swipe gestures to navigate between videos.
 */
class VideoSwipeHandler(private val viewPager: ViewPager2) {

    private var swipeThreshold = 100f
    private var startY = 0f
    private var videos = mutableListOf<Video>()
    private var videoAdapter: VideoAdapter? = null
    private var currentPosition = 0
    private var isSwipeEnabled = true

    /**
     * Set the adapter for the view pager
     */
    fun setVideoAdapter(adapter: VideoAdapter) {
        this.videoAdapter = adapter
    }

    /**
     * Set the list of videos to display
     */
    fun setVideos(videos: List<Video>) {
        this.videos.clear()
        this.videos.addAll(videos)
    }

    /**
     * Attach this handler to a view to detect touch events
     */
    fun attachToView(view: View) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.y
                    false
                }
                MotionEvent.ACTION_UP -> {
                    handleTouchEnd(event.y)
                }
                else -> false
            }
        }
    }

    /**
     * Process the completed touch event to determine if it was a swipe
     */
    private fun handleTouchEnd(endY: Float): Boolean {
        if (!isSwipeEnabled) return false

        val dragDistance = endY - startY

        // Check if the swipe distance exceeds the threshold
        if (abs(dragDistance) > swipeThreshold) {
            if (dragDistance < 0) {
                // Swipe up - move to the next video
                return swipeUp()
            } else {
                // Swipe down - move to the previous video
                return swipeDown()
            }
        }

        return false
    }

    /**
     * Handle swipe up gesture to show the next video
     */
    fun swipeUp(): Boolean {
        if (currentPosition < videos.size - 1) {
            // Pause the current video
            videoAdapter?.pauseVideo(currentPosition)

            // Update position
            currentPosition++

            // Update view pager
            viewPager.setCurrentItem(currentPosition, true)

            // Play the new video
            videoAdapter?.let {
                it.playVideo(currentPosition)
                it.updateWatchCount(currentPosition)
                it.updateCurrentPosition(currentPosition)
            }

            return true
        }
        return false
    }

    /**
     * Handle swipe down gesture to show the previous video
     */
    fun swipeDown(): Boolean {
        if (currentPosition > 0) {
            // Pause the current video
            videoAdapter?.pauseVideo(currentPosition)

            // Update position
            currentPosition--

            // Update view pager
            viewPager.setCurrentItem(currentPosition, true)

            // Play the new video
            videoAdapter?.let {
                it.playVideo(currentPosition)
                it.updateWatchCount(currentPosition)
                it.updateCurrentPosition(currentPosition)
            }

            return true
        }
        return false
    }

    /**
     * Get the current video position
     */
    fun getCurrentPosition(): Int {
        return currentPosition
    }

    /**
     * Update the current position
     */
    fun updateCurrentPosition(position: Int) {
        this.currentPosition = position
    }

    /**
     * Enable or disable swipe functionality
     */
    fun setSwipeEnabled(enabled: Boolean) {
        isSwipeEnabled = enabled
    }
}