package com.example.tiktokvasp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    onStartSession: (participantId: String, folderName: String) -> Unit
) {
    // Get state from ViewModel
    val availableFolders by viewModel.availableFolders.collectAsState()
    val participantId by viewModel.participantId.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            // Section title for folders
            Text(
                text = "Select a Video Folder",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 8.dp)
            )

            // Scrollable list of folders or "No folders found" message
            if (availableFolders.isEmpty()) {
                // No folders found message
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
            } else {
                // Scrollable list of folders
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start Session button
            Button(
                onClick = {
                    if (participantId.isNotEmpty() && selectedFolder != null) {
                        onStartSession(participantId, selectedFolder!!)
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
        }
    }
}