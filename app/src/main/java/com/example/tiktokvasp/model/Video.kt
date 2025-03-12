package com.example.tiktokvasp.model

import android.net.Uri

/**
 * Model class representing a video in the TikTok-style application
 */
data class Video(
    val id: String,
    val uri: Uri,
    val title: String = "",
    val description: String = "",
    val duration: Long = 0L,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: Uri? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isLiked: Boolean = false,
    val musicTitle: String = "Original Sound",
    val musicAuthor: String = ""
)