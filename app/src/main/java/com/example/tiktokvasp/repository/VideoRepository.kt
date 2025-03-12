package com.example.tiktokvasp.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.tiktokvasp.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class VideoRepository(private val context: Context) {

    suspend fun getLocalVideos(): List<Video> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<Video>()

        try {
            val contentResolver: ContentResolver = context.contentResolver
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )

            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    videos.add(
                        Video(
                            id = UUID.randomUUID().toString(),
                            uri = contentUri,
                            title = name,
                            duration = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error getting videos", e)
        }

        return@withContext videos
    }

    /**
     * Increment the watch count for a video
     */
    suspend fun incrementVideoWatchCount(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Here you would normally update a database or backend
            // For local videos, we could maintain a local database of watch counts
            // For this implementation, we'll just log the increment
            Log.d("VideoRepository", "Incrementing watch count for video: $videoId")
            return@withContext true
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error incrementing watch count", e)
            return@withContext false
        }
    }

    /**
     * Get a specific video by ID
     */
    suspend fun getVideoById(videoId: String): Video? = withContext(Dispatchers.IO) {
        try {
            // In a real app, you would query your database or backend for the specific video
            // For this implementation, we'll search through all local videos
            val allVideos = getLocalVideos()
            return@withContext allVideos.find { it.id == videoId }
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error getting video by ID", e)
            return@withContext null
        }
    }
}