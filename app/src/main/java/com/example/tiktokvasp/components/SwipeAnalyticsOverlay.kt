package com.example.tiktokvasp.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiktokvasp.tracking.SwipeAnalytics
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.tracking.SwipePoint

/**
 * A composable that visualizes detailed analytics for swipe gestures
 */
@Composable
fun SwipeAnalyticsOverlay(
    swipeEvent: SwipeEvent?,
    swipeAnalytics: SwipeAnalytics?,
    showDebugInfo: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Only render if we have data and debug mode is enabled
        if (showDebugInfo && swipeEvent != null && swipeAnalytics != null) {
            // Swipe path visualization
            SwipePathVisualizer(
                swipeEvent = swipeEvent,
                modifier = Modifier.fillMaxSize()
            )

            // Analytics panel
            AnalyticsPanel(
                swipeEvent = swipeEvent,
                swipeAnalytics = swipeAnalytics,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(220.dp)
            )
        }
    }
}

/**
 * Visualizes the path of a swipe gesture
 */
@Composable
fun SwipePathVisualizer(
    swipeEvent: SwipeEvent,
    modifier: Modifier = Modifier
) {
    val path = swipeEvent.path
    if (path.isEmpty()) return

    Canvas(modifier = modifier) {
        // Draw the path as a line
        val swipePath = Path()
        swipePath.moveTo(path.first().x, path.first().y)

        path.drop(1).forEach { point ->
            swipePath.lineTo(point.x, point.y)
        }

        // Draw the path with varying opacity
        drawPath(
            path = swipePath,
            color = when (swipeEvent.direction) {
                com.example.tiktokvasp.tracking.SwipeDirection.UP -> Color(0x80FF00FF)
                com.example.tiktokvasp.tracking.SwipeDirection.DOWN -> Color(0x8000FFFF)
                com.example.tiktokvasp.tracking.SwipeDirection.LEFT -> Color(0x80FFFF00)
                com.example.tiktokvasp.tracking.SwipeDirection.RIGHT -> Color(0x8000FF00)
            },
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )

        // Draw points along the path
        path.forEachIndexed { index, point ->
            // Only draw a subset of points to avoid clutter
            if (index % 5 == 0) {
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(point.x, point.y)
                )
            }
        }

        // Draw start and end points
        drawCircle(
            color = Color.Green,
            radius = 12f,
            center = Offset(path.first().x, path.first().y)
        )

        drawCircle(
            color = Color.Red,
            radius = 12f,
            center = Offset(path.last().x, path.last().y)
        )
    }
}

/**
 * Panel displaying detailed analytics about a swipe
 */
@Composable
fun AnalyticsPanel(
    swipeEvent: SwipeEvent,
    swipeAnalytics: SwipeAnalytics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(12.dp)
    ) {
        Text(
            text = "Swipe Analytics",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        AnalyticsRow("Direction", swipeEvent.direction.toString())
        AnalyticsRow("Duration", "${swipeEvent.swipeDurationMs} ms")
        AnalyticsRow("Distance", "${swipeEvent.distance.toInt()} px")
        AnalyticsRow("Points", "${swipeEvent.path.size}")
        AnalyticsRow("Velocity", "X: ${swipeEvent.velocityX.toInt()}, Y: ${swipeEvent.velocityY.toInt()}")

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Quality Metrics",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnalyticsProgressBar("Straightness", swipeAnalytics.straightness)
        AnalyticsProgressBar("Smoothness", swipeAnalytics.smoothness)
        AnalyticsProgressBar("Speed Consistency", swipeAnalytics.speedConsistency)
    }
}

@Composable
fun AnalyticsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun AnalyticsProgressBar(
    label: String,
    value: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${(value * 100).toInt()}%",
                color = Color.White,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Progress bar background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            // Progress bar fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            value > 0.8f -> Color(0xFF4CAF50) // Green
                            value > 0.5f -> Color(0xFFFFC107) // Yellow
                            else -> Color(0xFFF44336) // Red
                        }
                    )
            )
        }
    }
}