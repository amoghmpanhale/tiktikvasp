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
import android.provider.Settings

class LandingViewModel(application: Application) : AndroidViewModel(application) {
    private val _availableFolders = MutableStateFlow<List<String>>(emptyList())
    val availableFolders: StateFlow<List<String>> = _availableFolders.asStateFlow()

    private val _participantId = MutableStateFlow("")
    val participantId: StateFlow<String> = _participantId.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    init {
        loadAvailableFolders()
    }

    fun setParticipantId(id: String) {
        _participantId.value = id
    }

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
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
                val downloadsDir = File("/storage/emulated/0/Download")
                if (downloadsDir.exists()) {
                    Log.d("LandingViewModel", "Downloads directory found!")
                } else {
                    Log.e("LandingViewModel", "Downloads directory does not exist!")
                }

                // Find all directories that contain video files
                val folders = downloadsDir.listFiles { file ->
                    file.isDirectory && hasVideoFiles(file)
                }?.map { it.name } ?: emptyList()

                Log.d("LandingViewModel", "Found ${folders.size} folders with videos")
                _availableFolders.value = folders

            } catch (e: Exception) {
                Log.e("LandingViewModel", "Error loading folders", e)
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