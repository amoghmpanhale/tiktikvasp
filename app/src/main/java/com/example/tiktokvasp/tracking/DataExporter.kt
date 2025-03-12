package com.example.tiktokvasp.tracking

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataExporter(private val context: Context) {

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

                    // Add calculated metrics that may be useful for analysis
                    put("normalizedDistanceX", event.endX - event.startX / event.screenWidth.toFloat())
                    put("normalizedDistanceY", event.endY - event.startY / event.screenHeight.toFloat())

                    // Duration in seconds for easier analysis
                    put("durationSeconds", event.swipeDurationMs / 1000.0)
                }
                jsonArray.put(eventJson)
            }

            val fileName = "detailed_swipe_events_${getTimestamp()}.json"
            val file = getOutputFile(fileName)

            file.writeText(jsonArray.toString(4))
            Log.d("DataExporter", "Exported detailed swipe events to ${file.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e("DataExporter", "Failed to export detailed swipe events", e)
            return@withContext false
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

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }
}