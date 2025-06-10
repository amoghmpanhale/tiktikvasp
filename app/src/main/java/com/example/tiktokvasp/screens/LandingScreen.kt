package com.example.tiktokvasp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tiktokvasp.viewmodel.LandingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    viewModel: LandingViewModel = viewModel(),
    onStartSession: (participantId: String, folderName: String, durationMinutes: Int, autoGeneratePngs: Boolean, randomStopsEnabled: Boolean, randomStopFrequency: Int, randomStopDuration: Int, minPauseDuration: Int) -> Unit
) {
    // Get state from ViewModel
    val availableFolders by viewModel.availableFolders.collectAsState()
    val participantId by viewModel.participantId.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    // Session configuration
    var sessionDuration by remember { mutableStateOf(10) } // Default 10 minutes
    var autoGeneratePngs by remember { mutableStateOf(true) } // Default true

    // Random stops configuration - simplified, no user input needed
    val randomStopsEnabled by viewModel.randomStopsEnabled.collectAsState()

    // Fixed values for random stops - these are no longer configurable by user
    val randomStopFrequency = 45 // Average 45 seconds (30-60 range)
    val randomStopDuration = 22500 // Average 22.5 seconds (15-30 range)
    val minPauseDuration = 10 // Not used anymore, but keeping for compatibility

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Title
                Text(
                    text = "TikTok Scrolling Study",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                // Participant ID input
                OutlinedTextField(
                    value = participantId,
                    onValueChange = { viewModel.setParticipantId(it) },
                    label = { Text("Participant ID") },
                    placeholder = { Text("Enter participant ID") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color(0xFFFF0050),
                        unfocusedBorderColor = Color(0xFF3A3A3A),
                        focusedLabelColor = Color(0xFFFF0050),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFFFF0050)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                // Session Duration slider
                Text(
                    text = "Session Duration: $sessionDuration minutes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.Start)
                        .padding(vertical = 8.dp)
                )

                Slider(
                    value = sessionDuration.toFloat(),
                    onValueChange = { sessionDuration = it.toInt() },
                    valueRange = 1f..60f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF0050),
                        activeTrackColor = Color(0xFFFF0050),
                        inactiveTrackColor = Color(0xFF3A3A3A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Auto-generate PNGs toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-generate swipe pattern images",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = autoGeneratePngs,
                        onCheckedChange = { autoGeneratePngs = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF0050),
                            checkedTrackColor = Color(0x80FF0050),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF3A3A3A)
                        )
                    )
                }

                // Random Stops Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable random stops",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = randomStopsEnabled,
                        onCheckedChange = { viewModel.setRandomStopsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF0050),
                            checkedTrackColor = Color(0x80FF0050),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF3A3A3A)
                        )
                    )
                }

                // Information about random stops (no user configuration needed)
                if (randomStopsEnabled) {
                    Text(
                        text = "Random pauses will occur every 30-60 seconds and last 15-30 seconds each.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Section title for folders
                Text(
                    text = "Select a Video Folder",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.Start)
                        .padding(vertical = 16.dp)
                )
            }

            // Scrollable list of folders or "No folders found" message
            if (availableFolders.isEmpty()) {
                item {
                    // No folders found message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Video Folders Found",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please add video folders to the app's storage directory",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Scrollable list of folders
                items(availableFolders) { folder ->
                    val isSelected = selectedFolder == folder

                    Button(
                        onClick = {
                            viewModel.selectFolder(folder)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFFFF0050) else Color(0xFF2A2A2A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = folder,
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Start Session button
                Button(
                    onClick = {
                        if (participantId.isNotEmpty() && selectedFolder != null) {
                            onStartSession(
                                participantId,
                                selectedFolder!!,
                                sessionDuration,
                                autoGeneratePngs,
                                randomStopsEnabled,
                                randomStopFrequency,
                                randomStopDuration,
                                minPauseDuration
                            )
                        }
                    },
                    enabled = participantId.isNotEmpty() && selectedFolder != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0050),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF666666),
                        disabledContentColor = Color(0xFFAAAAAA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Start Session",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add some space at the bottom for scrolling
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}