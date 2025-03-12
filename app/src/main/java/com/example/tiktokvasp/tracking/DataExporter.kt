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
                    put("id", event.id)
                    put("sessionId", event.sessionId)
                    put("timestamp", event.timestamp)
                    put("videoId", event.videoId)
                    put("direction", event.direction.toString())
                    put("swipeDurationMs", event.swipeDurationMs)
                }
                jsonArray.put(eventJson)
            }

            val fileName = "swipe_events_${getTimestamp()}.json"
            val file = getOutputFile(fileName)

            file.writeText(jsonArray.toString(4))
            Log.d("DataExporter", "Exported swipe events to ${file.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e("DataExporter", "Failed to export swipe events", e)
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