package com.example.tiktokvasp.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.tiktokvasp.R
import com.example.tiktokvasp.model.Video

@Composable
fun TikTokOverlay(
    video: Video,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Bottom content (username, description)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "@username",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = video.title,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 2
            )
        }

        // Right side buttons (like, comment, share, etc.)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { /* Do nothing */ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Profile",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Like button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { /* Do nothing */ }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.like),
                        contentDescription = "Like",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "234.5K",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            // Comment button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { /* Do nothing */ }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.comment),
                        contentDescription = "Comment",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "1.2K",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            // Share button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { /* Do nothing */ }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = "Share",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun TikTokTopBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // Reduced horizontal padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Changed to SpaceBetween for better distribution
        ) {
            // Left section with main tabs
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // LIVE button with SVG style
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp) // Reduced padding
                        .height(26.dp)
                        .width(36.dp)
                        .background(
                            color = Color(0xFF0F0F0F),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 11.sp, // Slightly reduced
                        fontWeight = FontWeight.Bold
                    )
                }

                // Main navigation items with minimal spacing
                Text(
                    text = "STEM",
                    color = Color.White,
                    fontSize = 14.sp, // Reduced size slightly
                    modifier = Modifier.padding(horizontal = 6.dp) // Reduced padding
                )

                Text(
                    text = "Explore",
                    color = Color.White,
                    fontSize = 14.sp, // Reduced size slightly
                    modifier = Modifier.padding(horizontal = 6.dp) // Reduced padding
                )

                Text(
                    text = "Following",
                    color = Color.White,
                    fontSize = 14.sp, // Reduced size slightly
                    modifier = Modifier.padding(horizontal = 6.dp) // Reduced padding
                )

                // For You with active indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 6.dp) // Reduced padding
                ) {
                    Text(
                        text = "For You",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, // Reduced size slightly
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(Color.White)
                    )
                }
            }

            // Right section with search icon
            Image(
                painter = painterResource(id = R.drawable.search),
                contentDescription = "Search",
                modifier = Modifier
                    .size(28.dp)
                    .padding(end = 8.dp)
            )
        }
    }
}

@Composable
fun TikTokBottomBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home),
                    contentDescription = "Home",
                    modifier = Modifier.size(30.dp) // Increased from 24.dp
                )
                Text(
                    text = "Home",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.friends),
                    contentDescription = "Friends",
                    modifier = Modifier.size(30.dp) // Increased from 24.dp
                )
                Text(
                    text = "Friends",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // Custom Add button
            CustomAddButton()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.inbox),
                    contentDescription = "Inbox",
                    modifier = Modifier.size(30.dp) // Increased from 24.dp
                )
                Text(
                    text = "Inbox",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    modifier = Modifier.size(30.dp) // Increased from 24.dp
                )
                Text(
                    text = "Profile",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CustomAddButton() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        // Use your PNG image from the drawable folder
        Image(
            painter = painterResource(id = R.drawable.tiktok_add_button),
            contentDescription = "Create",
            modifier = Modifier.size(44.dp)
        )
    }
}