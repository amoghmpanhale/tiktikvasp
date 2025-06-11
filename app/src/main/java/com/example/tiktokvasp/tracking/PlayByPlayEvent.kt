package com.example.tiktokvasp.tracking

/**
 * Data class representing a single video viewing instance in the play-by-play log
 * Each instance represents one complete viewing of a video (which may be repeated)
 */
data class PlayByPlayEvent(
    val videoNumber: Int,           // Sequential number for this video instance (1, 2, 3...)
    val videoName: String,          // Name of the video file
    val videoDurationMs: Long,      // Total duration of the video
    val watchDurationMs: Long,      // How long the user watched this instance
    val wasLiked: Boolean,          // Whether user liked this video during this viewing
    val wasShared: Boolean,         // Whether user shared this video during this viewing
    val wasCommented: Boolean,      // Whether user commented on this video during this viewing
    val interruptionOccurred: Boolean,  // Whether an interruption happened during this viewing
    val interruptionDurationMs: Long,   // Duration of interruption (0 if none)
    val interruptionPointMs: Long,      // Timestamp in video when interruption occurred (0 if none)
    val timeSinceLastInterruptionMs: Long // Time elapsed since the last interruption ended (0 if first/none)
)
