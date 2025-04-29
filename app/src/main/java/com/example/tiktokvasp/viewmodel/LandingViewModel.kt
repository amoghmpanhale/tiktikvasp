package com.example.tiktokvasp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings

class LandingViewModel(application: Application) : AndroidViewModel(application) {
    private val _availableFolders = MutableStateFlow<List<String>>(emptyList())
    val availableFolders: StateFlow<List<String>> = _availableFolders.asStateFlow()

    private val _participantId = MutableStateFlow("")
    val participantId: StateFlow<String> = _participantId.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    // Simplified random stops - just a toggle now
    private val _randomStopsEnabled = MutableStateFlow(false)
    val randomStopsEnabled: StateFlow<Boolean> = _randomStopsEnabled.asStateFlow()

    init {
        loadAvailableFolders()
    }

    fun setParticipantId(id: String) {
        _participantId.value = id
    }

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
    }

    // Simplified random stops setter
    fun setRandomStopsEnabled(enabled: Boolean) {
        _randomStopsEnabled.value = enabled
    }

    fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            false
        }
    }

    fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + getApplication<Application>().packageName)
                getApplication<Application>().startActivity(intent)
            } catch (e: Exception) {
                Log.e("LandingViewModel", "Error requesting MANAGE_EXTERNAL_STORAGE", e)
            }
        }
    }

    private fun loadAvailableFolders() {
        viewModelScope.launch {
            try {
                Log.e("LandingViewModel", "Attempting to load videos via MediaStore")

                // Use MediaStore to get videos
                val videos = mutableMapOf<String, MutableList<String>>()

                val projection = arrayOf(
                    MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.RELATIVE_PATH
                )

                getApplication<Application>().contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)

                    Log.e("LandingViewModel", "Found ${cursor.count} videos in MediaStore")

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn)
                        val path = cursor.getString(pathColumn)

                        // Extract folder name from path
                        val folderName = path.split("/").lastOrNull { it.isNotEmpty() } ?: "Unknown"

                        Log.e("LandingViewModel", "Video: $name, Folder: $folderName, Path: $path")

                        // Group videos by folder
                        if (!videos.containsKey(folderName)) {
                            videos[folderName] = mutableListOf()
                        }
                        videos[folderName]?.add(name)
                    }

                    // Convert to list of folder names that have videos
                    val folders = videos.keys.toList()
                    Log.e("LandingViewModel", "Found ${folders.size} folders with videos: $folders")

                    _availableFolders.value = folders
                } ?: run {
                    Log.e("LandingViewModel", "MediaStore query returned null")
                    _availableFolders.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("LandingViewModel", "Error loading videos via MediaStore", e)
                e.printStackTrace()
                _availableFolders.value = emptyList()
            }
        }
    }

    private fun hasVideoFiles(directory: File): Boolean {
        val videoExtensions = listOf("mp4", "3gp", "mkv", "webm", "avi")
        return directory.listFiles()?.any { file ->
            !file.isDirectory &&
                    file.extension.lowercase() in videoExtensions
        } ?: false
    }
}