package com.example.tiktokvasp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A full-screen gray overlay that appears during random stops in the psychology study
 */
@Composable
fun RandomStopOverlay(
    showText: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Gray.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        if (showText) {
            Text(
                text = "Please wait...",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}