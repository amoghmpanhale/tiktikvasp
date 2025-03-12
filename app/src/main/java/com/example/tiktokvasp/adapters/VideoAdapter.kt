package com.example.tiktokvasp.adapters

import android.content.Context
import android.net.Uri
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
        }
    }

    override fun getItemCount(): Int = videos.size

    fun updateVideos(newVideos: List<Video>) {
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

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.video_player)
        private val titleTextView: TextView = itemView.findViewById(R.id.video_title)
        private val likeIcon: ImageView = itemView.findViewById(R.id.like_icon)
        private val commentIcon: ImageView = itemView.findViewById(R.id.comment_icon)
        private val shareIcon: ImageView = itemView.findViewById(R.id.share_icon)
        private val avatar: ImageView = itemView.findViewById(R.id.avatar)

        private var player: ExoPlayer? = null
        private var videoUri: Uri? = null
        private var videoId: String = ""

        init {
            setupClickListeners()
        }

        private fun setupClickListeners() {
            likeIcon.setOnClickListener {
                viewModel.likeVideo(videoId)
            }

            commentIcon.setOnClickListener {
                viewModel.openComments(videoId)
            }

            shareIcon.setOnClickListener {
                viewModel.shareVideo(videoId)
            }

            avatar.setOnClickListener {
                viewModel.openUserProfile(videoId)
            }

            playerView.setOnClickListener {
                // Toggle play/pause on tap
                togglePlayback()
            }
        }

        fun bind(video: Video) {
            videoId = video.id
            videoUri = Uri.parse(video.uri.toString())
            titleTextView.text = "@${video.title}"

            // Initialize player if needed
            if (player == null) {
                initializePlayer()
            }

            // Prepare the video
            player?.let { exoPlayer ->
                val mediaItem = MediaItem.fromUri(videoUri!!)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = false
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            }
        }

        private fun initializePlayer() {
            player = ExoPlayer.Builder(context).build().apply {
                playWhenReady = false
                repeatMode = Player.REPEAT_MODE_ONE

                // Add listener to update UI when player state changes
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        super.onPlaybackStateChanged(state)
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                // Show loading indicator
                            }
                            Player.STATE_READY -> {
                                // Hide loading indicator
                            }
                            Player.STATE_ENDED -> {
                                // Video ended, maybe restart it or show replay button
                            }
                            Player.STATE_IDLE -> {
                                // Uninitialized state
                            }
                        }
                    }
                })
            }

            playerView.player = player
        }

        fun play() {
            player?.playWhenReady = true
        }

        fun pause() {
            player?.playWhenReady = false
        }

        fun togglePlayback() {
            player?.let {
                it.playWhenReady = !it.playWhenReady
            }
        }

        fun release() {
            player?.release()
            player = null
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
        }
    }

    override fun onViewDetachedFromWindow(holder: VideoViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.pause()
    }
}