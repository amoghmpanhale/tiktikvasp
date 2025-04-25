package com.example.tiktokvasp.tracking

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.tiktokvasp.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Enhanced data exporter that supports all the required metrics
 */
class DataExporter(private val context: Context) {

    /**
     * Export detailed session data as CSV with all metrics
     */
    suspend fun exportSessionData(
        participantId: String,
        category: String,
        videos: List<Video>,
        viewEvents: List<ViewEvent>,
        swipeEvents: List<SwipeEvent>,
        swipeAnalytics: Map<String, SwipeAnalytics>,
        swipePatternPaths: Map<String, String>
    ): String = withContext(Dispatchers.IO) {
        try {
            val timestamp = getTimestamp()
            val directory = getSessionDirectory(participantId, category)
            val fileName = "session_data_${timestamp}.csv"
            val file = File(directory, fileName)

            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("Participant ID,Category,Video Number,Video Name,Video Duration(ms),")
                writer.append("Watch Duration(ms),Watch Percentage,Liked?,Shared?,")
                writer.append("Swipe Pattern Image,Swipe Direction,Swipe Velocity(px/s),")
                writer.append("Swipe Acceleration(px/s²),Swipe Regularity(%)\n")

                // Group view events by video
                val viewsByVideo = viewEvents.groupBy { it.videoId }

                // Create a map of video ID to video number (sequential numbering)
                val videoNumberMap = mutableMapOf<String, Int>()
                videos.forEachIndexed { index, video ->
                    videoNumberMap[video.id] = index + 1
                }

                // Process each video
                videos.forEach { video ->
                    val videoNumber = videoNumberMap[video.id] ?: 0
                    val videoName = video.title
                    val videoDuration = video.duration

                    // Find all view events for this video
                    val views = viewsByVideo[video.id] ?: emptyList()

                    // Find all swipe events after viewing this video
                    val relevantSwipes = swipeEvents.filter { it.videoId == video.id }

                    // If there are no views or swipes, still record the video with empty data
                    if (views.isEmpty() && relevantSwipes.isEmpty()) {
                        writer.append("$participantId,$category,$videoNumber,\"$videoName\",$videoDuration,")
                        writer.append("0,0,No,No,,,0,0,0\n")
                    } else {
                        // Process each view event
                        views.forEach { view ->
                            // Find the swipe that ended this view
                            val exitSwipe = swipeEvents.find {
                                it.videoId == view.videoId &&
                                        abs(it.timestamp - (view.timestamp + view.watchDurationMs)) < 1000
                            }

                            // Get swipe analytics if available
                            val analytics = exitSwipe?.let { swipeAnalytics[it.id] }

                            // Get swipe pattern path if available
                            val patternPath = exitSwipe?.let { swipePatternPaths[it.id] } ?: ""

                            // Determine if video was liked or shared (placeholder logic)
                            // In a real app, this would be tracked in the ViewEvent
                            val isLiked = "No" // Replace with actual data if tracked
                            val isShared = "No" // Replace with actual data if tracked

                            // Write the row
                            writer.append("$participantId,$category,$videoNumber,\"$videoName\",$videoDuration,")
                            writer.append("${view.watchDurationMs},${view.watchPercentage},$isLiked,$isShared,")

                            if (exitSwipe != null && analytics != null) {
                                writer.append("\"$patternPath\",${exitSwipe.direction},")
                                writer.append("${abs(exitSwipe.velocityY).toInt()},")
                                writer.append("${analytics.acceleration.toInt()},")
                                writer.append("${(analytics.speedConsistency * 100).toInt()}\n")
                            } else {
                                writer.append(",,0,0,0\n")
                            }
                        }
                    }
                }
            }

            Log.d("DataExporter", "Exported session data to ${file.absolutePath}")
            return@withContext file.absolutePath

        } catch (e: Exception) {
            Log.e("DataExporter", "Failed to export session data", e)
            return@withContext ""
        }
    }

    /**
     * Export swipe events as before (keeping for backward compatibility)
     */
    suspend fun exportSwipeEvents(events: List<SwipeEvent>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            events.forEach { event ->
                val eventJson = JSONObject().apply {
                    // Basic information
                    put("id", event.id)
                    put("sessionId", event.sessionId)
                    put("timestamp", event.timestamp)
                    put("videoId", event.videoId)
                    put("direction", event.direction.toString())
                    put("swipeDurationMs", event.swipeDurationMs)

                    // Enhanced tracking data
                    put("startX", event.startX)
                    put("startY", event.startY)
                    put("endX", event.endX)
                    put("endY", event.endY)
                    put("velocityX", event.velocityX)
                    put("velocityY", event.velocityY)
                    put("distance", event.distance)
                    put("pressure", event.pressure)

                    // Screen dimensions for normalization
                    put("screenWidth", event.screenWidth)
                    put("screenHeight", event.screenHeight)

                    // Full path data as a nested JSON array
                    val pathArray = JSONArray()
                    event.path.forEach { point ->
                        val pointJson = JSONObject().apply {
                            put("x", point.x)
                            put("y", point.y)
                            put("timestamp", point.timestamp)
                            put("pressure", point.pressure ?: 0f)
                        }
                        pathArray.put(pointJson)
                    }
                    put("path", pathArray)

                    // safe normalized distances
                    val normX = if (event.screenWidth > 0)
                        (event.endX - event.startX) / event.screenWidth.toFloat()
                    else 0f
                    put("normalizedDistanceX", if (normX.isFinite()) normX else 0f)

                    val normY = if (event.screenHeight > 0)
                        (event.endY - event.startY) / event.screenHeight.toFloat()
                    else 0f
                    put("normalizedDistanceY", if (normY.isFinite()) normY else 0f)

                    // safe duration in seconds
                    val durSec = event.swipeDurationMs / 1000.0
                    put("durationSeconds", if (durSec.isFinite()) durSec else 0.0)
                }
                jsonArray.put(eventJson)
            }

            val fileName = "detailed_swipe_events_${getTimestamp()}.json"
            val file = getOutputFile(fileName)
            file.writeText(jsonArray.toString(4))

            Log.d("DataExporter", "Exported detailed swipe events to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("DataExporter", "Failed to export detailed swipe events", e)
            false
        }
    }

    /**
     * Export detailed swipe data as CSV for easier import into analysis tools
     */
    suspend fun exportSwipeEventsAsCSV(events: List<SwipeEvent>): Boolean = withContext(Dispatchers.IO) {
        try {
            val csvBuilder = StringBuilder()

            // Write CSV header
            csvBuilder.append("id,sessionId,timestamp,videoId,direction,swipeDurationMs,startX,startY,endX,endY,")
            csvBuilder.append("velocityX,velocityY,distance,pressure,screenWidth,screenHeight\n")

            // Write data rows
            events.forEach { event ->
                csvBuilder.append("${event.id},")
                csvBuilder.append("${event.sessionId},")
                csvBuilder.append("${event.timestamp},")
                csvBuilder.append("${event.videoId},")
                csvBuilder.append("${event.direction},")
                csvBuilder.append("${event.swipeDurationMs},")
                csvBuilder.append("${event.startX},")
                csvBuilder.append("${event.startY},")
                csvBuilder.append("${event.endX},")
                csvBuilder.append("${event.endY},")
                csvBuilder.append("${event.velocityX},")
                csvBuilder.append("${event.velocityY},")
                csvBuilder.append("${event.distance},")
                csvBuilder.append("${event.pressure},")
                csvBuilder.append("${event.screenWidth},")
                csvBuilder.append("${event.screenHeight}\n")
            }

            // Export path data separately - one row per point
            val pathCsvBuilder = StringBuilder()
            pathCsvBuilder.append("swipeId,pointIndex,x,y,timestamp,pressure\n")

            events.forEach { event ->
                event.path.forEachIndexed { index, point ->
                    pathCsvBuilder.append("${event.id},")
                    pathCsvBuilder.append("$index,")
                    pathCsvBuilder.append("${point.x},")
                    pathCsvBuilder.append("${point.y},")
                    pathCsvBuilder.append("${point.timestamp},")
                    pathCsvBuilder.append("${point.pressure ?: 0f}\n")
                }
            }

            val fileName = "swipe_events_${getTimestamp()}.csv"
            val file = getOutputFile(fileName)
            file.writeText(csvBuilder.toString())

            val pathFileName = "swipe_paths_${getTimestamp()}.csv"
            val pathFile = getOutputFile(pathFileName)
            pathFile.writeText(pathCsvBuilder.toString())

            Log.d("DataExporter", "Exported swipe events to CSV at ${file.absolutePath}")
            Log.d("DataExporter", "Exported swipe paths to CSV at ${pathFile.absolutePath}")

            return@withContext true
        } catch (e: Exception) {
            Log.e("DataExporter", "Failed to export swipe events as CSV", e)
            return@withContext false
        }
    }

    suspend fun exportViewEvents(events: List<ViewEvent>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()

            events.forEach { event ->
                val eventJson = JSONObject().apply {
                    put("id", event.id)
                    put("sessionId", event.sessionId)
                    put("timestamp", event.timestamp)
                    put("videoId", event.videoId)
                    put("watchDurationMs", event.watchDurationMs)
                    put("watchPercentage", event.watchPercentage)
                }
                jsonArray.put(eventJson)
            }

            val fileName = "view_events_${getTimestamp()}.json"
            val file = getOutputFile(fileName)

            file.writeText(jsonArray.toString(4))
            Log.d("DataExporter", "Exported view events to ${file.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e("DataExporter", "Failed to export view events", e)
            return@withContext false
        }
    }

    private fun getOutputFile(fileName: String): File {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "TikTokVasp")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, fileName)
    }

    private fun getSessionDirectory(participantId: String, category: String): File {
        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "TikTokVasp/$participantId/$category"
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }
}