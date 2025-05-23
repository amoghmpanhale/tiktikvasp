package com.example.tiktokvasp.adapters

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.example.tiktokvasp.R
import com.example.tiktokvasp.model.Video
import com.example.tiktokvasp.viewmodel.MainViewModel

/**
 * Adapter for vertical swiping through videos in a ViewPager2
 */
class VideoAdapter(
    private val context: Context,
    private var videos: List<Video>,
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val viewHolders = mutableMapOf<Int, VideoViewHolder>()
    private var currentPosition = 0

    // Track all created ExoPlayers to ensure proper cleanup
    private val allPlayers = mutableListOf<ExoPlayer>()

    // #region Haptic Feedback
    private fun performHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
    // #endregion

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.video_container, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videos[position])
        viewHolders[position] = holder

        // Start playing automatically if this is the current position
        if (position == currentPosition) {
            holder.play()
        } else {
            // Ensure other videos are paused
            holder.pause()
        }
    }

    override fun getItemCount(): Int = videos.size

    fun updateVideos(newVideos: List<Video>) {
        // Release all players before updating the video list
        releaseAllPlayers()
        videos = newVideos
        notifyDataSetChanged()
    }

    fun playVideo(position: Int) {
        viewHolders[position]?.play()
    }

    fun pauseVideo(position: Int) {
        viewHolders[position]?.pause()
    }

    fun updateCurrentPosition(position: Int) {
        val oldPosition = currentPosition
        currentPosition = position

        // Pause old video and play new one
        if (oldPosition != position) {
            pauseVideo(oldPosition)
            playVideo(position)
        }
    }

    fun updateWatchCount(position: Int) {
        if (position >= 0 && position < videos.size) {
            viewModel.incrementVideoWatchCount(videos[position].id)
        }
    }

    // Release all ExoPlayer instances to free resources and stop playback
    fun releaseAllPlayers() {
        Log.d("VideoAdapter", "Releasing ${allPlayers.size} ExoPlayer instances")
        allPlayers.forEach { player ->
            player.stop()
            player.clearMediaItems()
            player.release()
        }
        allPlayers.clear()

        // Also release players in all ViewHolders
        viewHolders.values.forEach { holder ->
            holder.release()
        }
        viewHolders.clear()
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        if (isPlaying) {
            playVideo(currentPosition)
        } else {
            pauseVideo(currentPosition)
        }
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.video_player)
        private val titleTextView: TextView = itemView.findViewById(R.id.video_title)
        private val videoDescriptionTextView: TextView = itemView.findViewById(R.id.video_description)
        private val likeIcon: ImageView = itemView.findViewById(R.id.like_icon)
        private val commentIcon: ImageView = itemView.findViewById(R.id.comment_icon)
        private val bookmarkIcon: ImageView = itemView.findViewById(R.id.bookmark_icon)
        private val shareIcon: ImageView = itemView.findViewById(R.id.share_icon)
        private val likeCountTextView: TextView = itemView.findViewById(R.id.like_count)
        private val commentCountTextView: TextView = itemView.findViewById(R.id.comment_count)
        private val bookmarkCountTextView: TextView = itemView.findViewById(R.id.bookmark_count)
        private val shareCountTextView: TextView = itemView.findViewById(R.id.share_count)
        private val avatar: ImageView = itemView.findViewById(R.id.avatar)

        private var player: ExoPlayer? = null
        private var videoUri: Uri? = null
        private var videoId: String = ""
        private var mediaItem: MediaItem? = null
        // Track double tap timing for like functionality
        private var lastTapTime: Long = 0
        private val doubleTapDelay: Long = 300 // ms

        init {
            setupClickListeners()
        }

        private fun setupClickListeners() {
            likeIcon.setOnClickListener {
                // Add haptic feedback
                performHapticFeedback()

                // Toggle like status
                val currentLiked = viewModel.isVideoLiked(videoId)
                if (currentLiked) {
                    viewModel.unlikeVideo(videoId)
                } else {
                    viewModel.likeVideo(videoId)
                }
                // Update UI
                updateLikeStatus(!currentLiked)
            }

            commentIcon.setOnClickListener {
                // Add haptic feedback
                performHapticFeedback()

                // Toggle comment status
                val currentCommented = viewModel.hasCommentedOnVideo(videoId)
                if (currentCommented) {
                    viewModel.uncommentVideo(videoId)
                } else {
                    viewModel.commentOnVideo(videoId)
                }
                updateCommentStatus(!currentCommented)
            }

            bookmarkIcon.setOnClickListener {
                // Add haptic feedback
                performHapticFeedback()

                // Toggle bookmark status
                val currentBookmarked = viewModel.isVideoBookmarked(videoId)
                if (currentBookmarked) {
                    viewModel.unbookmarkVideo(videoId)
                } else {
                    viewModel.bookmarkVideo(videoId)
                }
                updateBookmarkStatus(!currentBookmarked)
            }

            shareIcon.setOnClickListener {
                // Add haptic feedback
                performHapticFeedback()

                // Toggle share status
                val currentShared = viewModel.isVideoShared(videoId)
                if (currentShared) {
                    viewModel.unshareVideo(videoId)
                } else {
                    viewModel.shareVideo(videoId)
                }
                updateShareStatus(!currentShared)
            }

            avatar.setOnClickListener {
                // Add haptic feedback
                performHapticFeedback()

                viewModel.openUserProfile(videoId)
            }

            // Keep double-tap to like/unlike but remove single-tap to pause
            playerView.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < doubleTapDelay) {
                    // Double tap detected - add haptic feedback and toggle like status
                    performHapticFeedback()

                    val currentLiked = viewModel.isVideoLiked(videoId)
                    if (currentLiked) {
                        viewModel.unlikeVideo(videoId)
                        updateLikeStatus(false)
                    } else {
                        viewModel.likeVideo(videoId)
                        updateLikeStatus(true)
                    }
                    lastTapTime = 0 // Reset to prevent triple tap
                } else {
                    // Single tap - do nothing (remove pause functionality)
                    lastTapTime = now
                    // No longer toggle playback - videos should stay playing
                }
            }
        }

        fun bind(video: Video) {
            videoId = video.id
            videoUri = Uri.parse(video.uri.toString())

            // Extract username from the video title using the specified pattern
            // Look for text between ")" and "_"
            val title = video.title
            val username = extractUsername(title)
            titleTextView.text = "@$username"

            // Update UI based on like/share/comment/bookmark status
            updateLikeStatus(viewModel.isVideoLiked(videoId))
            updateShareStatus(viewModel.isVideoShared(videoId))
            updateCommentStatus(viewModel.hasCommentedOnVideo(videoId))
            updateBookmarkStatus(viewModel.isVideoBookmarked(videoId))

            // Release old player if it exists to avoid memory leaks
            release()

            // Initialize a new player
            initializePlayer()

            // Prepare the video but don't automatically start
            player?.let { exoPlayer ->
                mediaItem = MediaItem.fromUri(videoUri!!)
                exoPlayer.setMediaItem(mediaItem!!)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = false  // Don't play automatically
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }

        private fun extractUsername(title: String): String {
            // Extract username as everything up to the first underscore followed by a number
            val pattern = """_\d""".toRegex()
            val match = pattern.find(title)

            return if (match != null) {
                title.substring(0, match.range.first).trim()
            } else {
                // Fallback: if no underscore+number pattern found, remove file extension
                title.substringBeforeLast('.').trim()
            }
        }

        private fun updateLikeStatus(isLiked: Boolean) {
            if (isLiked) {
                likeIcon.setColorFilter(Color.RED)
            } else {
                likeIcon.setColorFilter(Color.WHITE)
            }
        }

        private fun updateCommentStatus(hasCommented: Boolean) {
            if (hasCommented) {
                commentIcon.setColorFilter(Color.YELLOW)
            } else {
                commentIcon.setColorFilter(Color.WHITE)
            }
        }

        private fun updateShareStatus(isShared: Boolean) {
            if (isShared) {
                shareIcon.setColorFilter(Color.CYAN)
            } else {
                shareIcon.setColorFilter(Color.WHITE)
            }
        }

        private fun updateBookmarkStatus(isBookmarked: Boolean) {
            if (isBookmarked) {
                bookmarkIcon.setColorFilter(Color.BLUE)
            } else {
                bookmarkIcon.setColorFilter(Color.WHITE)
            }
        }

        private fun initializePlayer() {
            // Create a new ExoPlayer instance
            player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = false // Don't auto-play
                repeatMode = Player.REPEAT_MODE_ONE

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        super.onPlaybackStateChanged(state)
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                Log.d("VideoAdapter", "Player STATE_BUFFERING")
                            }
                            Player.STATE_READY -> {
                                Log.d("VideoAdapter", "Player STATE_READY")
                            }
                            Player.STATE_ENDED -> {
                                Log.d("VideoAdapter", "Player STATE_ENDED")
                            }
                            Player.STATE_IDLE -> {
                                Log.d("VideoAdapter", "Player STATE_IDLE")
                            }
                        }
                    }
                })
            }

            // Track this player for cleanup
            player?.let {
                allPlayers.add(it)
                Log.d("VideoAdapter", "New ExoPlayer created, total: ${allPlayers.size}")
            }

            playerView.player = player
        }

        fun play() {
            Log.d("VideoAdapter", "Playing video at position $adapterPosition, id: $videoId")
            player?.playWhenReady = true
        }

        fun pause() {
            Log.d("VideoAdapter", "Pausing video at position $adapterPosition, id: $videoId")
            player?.playWhenReady = false
        }

        fun release() {
            player?.let {
                Log.d("VideoAdapter", "Releasing player for position $adapterPosition, id: $videoId")
                it.stop()
                it.clearMediaItems()
                it.release()
                allPlayers.remove(it)
            }
            player = null
            playerView.player = null
        }
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.pause()
    }

    override fun onViewAttachedToWindow(holder: VideoViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder.adapterPosition == currentPosition) {
            holder.play()
        } else {
            holder.pause()
        }
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.pause()
    }
}