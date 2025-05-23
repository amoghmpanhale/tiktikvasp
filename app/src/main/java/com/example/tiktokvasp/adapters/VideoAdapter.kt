package com.example.tiktokvasp.adapters

import android.content.Context
import android.graphics.Color
import android.net.Uri
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
                viewModel.openComments(videoId)
                updateCommentStatus(true)
            }

            shareIcon.setOnClickListener {
                viewModel.shareVideo(videoId)
                updateShareStatus(true)
            }

            avatar.setOnClickListener {
                viewModel.openUserProfile(videoId)
            }

            // Keep double-tap to like/unlike but remove single-tap to pause
            playerView.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastTapTime < doubleTapDelay) {
                    // Double tap detected - toggle like status
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

            // Update UI based on like/share/comment status
            updateLikeStatus(viewModel.isVideoLiked(videoId))
            updateShareStatus(viewModel.isVideoShared(videoId))
            updateCommentStatus(viewModel.hasCommentedOnVideo(videoId))

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
            // Try to find text between ")" and "_"
            val pattern = """\)(.+?)_""".toRegex()
            val matchResult = pattern.find(title)

            return if (matchResult != null && matchResult.groupValues.size > 1) {
                matchResult.groupValues[1].trim()
            } else {
                // Fallback to just the title if pattern doesn't match
                title
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