package com.example.tiktokvasp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tiktokvasp.tracking.BatchAnalytics
import com.example.tiktokvasp.tracking.SwipeAnalytics
import com.example.tiktokvasp.tracking.SwipeDirection
import com.example.tiktokvasp.tracking.SwipeEvent
import com.example.tiktokvasp.tracking.UserBehaviorAnalytics
import com.example.tiktokvasp.viewmodel.DebugViewModel

/**
 * Debug screen to visualize swipe analytics data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeAnalyticsDebugScreen(
    viewModel: DebugViewModel,
    onBackPressed: () -> Unit
) {
    val swipeEvents by viewModel.swipeEvents.collectAsState()
    val batchAnalytics by viewModel.batchAnalytics.collectAsState()
    val userBehaviorAnalytics by viewModel.userBehaviorAnalytics.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var showDetailedView by remember { mutableStateOf(false) }
    var selectedSwipe by remember { mutableStateOf<SwipeEvent?>(null) }
    var selectedAnalytics by remember { mutableStateOf<SwipeAnalytics?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Swipe Analytics Debug") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.exportSwipeData() },
                        enabled = !isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Data"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Toggle for detailed view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show Detailed View",
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = showDetailedView,
                    onCheckedChange = { showDetailedView = it }
                )
            }

            // User behavior analytics summary
            UserBehaviorAnalyticsCard(
                analytics = userBehaviorAnalytics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Batch analytics summary
            BatchAnalyticsCard(
                analytics = batchAnalytics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Individual swipe events
            Text(
                text = "Recent Swipes (${swipeEvents.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(swipeEvents) { event ->
                    SwipeEventItem(
                        swipeEvent = event,
                        onClick = {
                            selectedSwipe = event
                            selectedAnalytics = viewModel.getSwipeAnalytics(event)
                        },
                        isSelected = selectedSwipe?.id == event.id,
                        showDetailedView = showDetailedView,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exporting data...",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun UserBehaviorAnalyticsCard(
    analytics: UserBehaviorAnalytics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "User Behavior Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsItem(
                    label = "Swipe Count",
                    value = analytics.swipeCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                AnalyticsItem(
                    label = "Swipes/Min",
                    value = String.format("%.1f", analytics.swipesPerMinute),
                    modifier = Modifier.weight(1f)
                )

                AnalyticsItem(
                    label = "Preferred",
                    value = analytics.dominantDirection?.toString() ?: "None",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "User Style:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Text(text = analytics.userStyle)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Consistency:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Consistency bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.LightGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(analytics.consistencyScore)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    analytics.consistencyScore > 0.8f -> Color(0xFF4CAF50)
                                    analytics.consistencyScore > 0.5f -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                }
                            )
                    )
                }

                Text(
                    text = "${(analytics.consistencyScore * 100).toInt()}%",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun BatchAnalyticsCard(
    analytics: BatchAnalytics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Swipe Metrics (${analytics.swipeCount} swipes)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // First row of metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsItem(
                    label = "Avg Speed",
                    value = String.format("%.0f px/s", analytics.avgVelocity),
                    modifier = Modifier.weight(1f)
                )

                AnalyticsItem(
                    label = "Avg Duration",
                    value = "${analytics.avgDurationMs} ms",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Second row of metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnalyticsItem(
                    label = "Straightness",
                    value = String.format("%.0f%%", analytics.avgStraightness * 100),
                    modifier = Modifier.weight(1f)
                )

                AnalyticsItem(
                    label = "Smoothness",
                    value = String.format("%.0f%%", analytics.avgSmoothness * 100),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Direction distribution
            Text(
                text = "Direction Distribution",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                analytics.directionDistribution.forEach { (direction, percentage) ->
                    DirectionIndicator(
                        direction = direction,
                        percentage = percentage
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyticsItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DirectionIndicator(
    direction: SwipeDirection,
    percentage: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = percentage))
                .border(1.dp, Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (direction) {
                SwipeDirection.UP -> Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Up",
                    tint = Color.DarkGray
                )
                SwipeDirection.DOWN -> Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Down",
                    tint = Color.DarkGray
                )
                SwipeDirection.LEFT -> Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Left",
                    tint = Color.DarkGray
                )
                SwipeDirection.RIGHT -> Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Right",
                    tint = Color.DarkGray
                )
            }
        }

        Text(
            text = "${(percentage * 100).toInt()}%",
            fontSize = 12.sp
        )
    }
}

@Composable
fun SwipeEventItem(
    swipeEvent: SwipeEvent,
    onClick: () -> Unit,
    isSelected: Boolean,
    showDetailedView: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (swipeEvent.direction) {
                                SwipeDirection.UP -> Color(0x33FF00FF)
                                SwipeDirection.DOWN -> Color(0x3300FFFF)
                                SwipeDirection.LEFT -> Color(0x33FFFF00)
                                SwipeDirection.RIGHT -> Color(0x3300FF00)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (swipeEvent.direction) {
                        SwipeDirection.UP -> Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Up",
                            tint = Color.DarkGray
                        )
                        SwipeDirection.DOWN -> Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Down",
                            tint = Color.DarkGray
                        )
                        SwipeDirection.LEFT -> Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Left",
                            tint = Color.DarkGray
                        )
                        SwipeDirection.RIGHT -> Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Right",
                            tint = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                // Basic info column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${swipeEvent.direction} Swipe",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                        Text(
                            text = "${swipeEvent.swipeDurationMs} ms",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Text(
                        text = "Distance: ${swipeEvent.distance.toInt()} px | Points: ${swipeEvent.path.size}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Velocity indicator
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${Math.abs(swipeEvent.velocityY).toInt()} px/s",
                        fontWeight = FontWeight.Medium,
                        color = when {
                            Math.abs(swipeEvent.velocityY) > 2000 -> Color(0xFF4CAF50)
                            Math.abs(swipeEvent.velocityY) > 1000 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }

            // Detailed view
            if (showDetailedView && isSelected) {
                Spacer(modifier = Modifier.height(8.dp))

                Divider()

                Spacer(modifier = Modifier.height(8.dp))

                // Path visualization
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.2f))
                ) {
                    // Simple representation of the path
                    SwipePathSimpleVisualizer(swipeEvent)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // More detailed metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailMetric(
                        label = "Start",
                        value = "(${swipeEvent.startX.toInt()}, ${swipeEvent.startY.toInt()})"
                    )

                    DetailMetric(
                        label = "End",
                        value = "(${swipeEvent.endX.toInt()}, ${swipeEvent.endY.toInt()})"
                    )

                    DetailMetric(
                        label = "Velocity",
                        value = "X: ${swipeEvent.velocityX.toInt()}\nY: ${swipeEvent.velocityY.toInt()}"
                    )
                }
            }
        }
    }
}

@Composable
fun DetailMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SwipePathSimpleVisualizer(
    swipeEvent: SwipeEvent
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw simplified path from start to end
        drawLine(
            color = when (swipeEvent.direction) {
                SwipeDirection.UP -> Color(0xFFFF00FF)
                SwipeDirection.DOWN -> Color(0xFF00FFFF)
                SwipeDirection.LEFT -> Color(0xFFFFFF00)
                SwipeDirection.RIGHT -> Color(0xFF00FF00)
            },
            start = Offset(
                x = swipeEvent.startX * size.width / swipeEvent.screenWidth,
                y = swipeEvent.startY * size.height / swipeEvent.screenHeight
            ),
            end = Offset(
                x = swipeEvent.endX * size.width / swipeEvent.screenWidth,
                y = swipeEvent.endY * size.height / swipeEvent.screenHeight
            ),
            strokeWidth = 4f
        )

        // Draw start and end points
        drawCircle(
            color = Color.Green,
            radius = 8f,
            center = Offset(
                x = swipeEvent.startX * size.width / swipeEvent.screenWidth,
                y = swipeEvent.startY * size.height / swipeEvent.screenHeight
            )
        )

        drawCircle(
            color = Color.Red,
            radius = 8f,
            center = Offset(
                x = swipeEvent.endX * size.width / swipeEvent.screenWidth,
                y = swipeEvent.endY * size.height / swipeEvent.screenHeight
            )
        )
    }
}