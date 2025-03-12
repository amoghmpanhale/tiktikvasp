package com.example.tiktokvasp.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.tiktokvasp.model.Video
import kotlin.math.abs

@Composable
fun VideoPlayer(
    video: Video,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dragStartY by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f  // Minimum distance to trigger a swipe

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragStartY = it.y },
                    onDragEnd = { /* Not used */ },
                    onDragCancel = { /* Not used */ },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val dragDistance = change.position.y - dragStartY

                        // When drag ends with sufficient distance, trigger swipe
                        if (abs(dragDistance) > swipeThreshold) {
                            if (dragDistance > 0) {
                                // Swiped down
                                onSwipeDown()
                            } else {
                                // Swiped up
                                onSwipeUp()
                            }
                            // Reset drag start position
                            dragStartY = change.position.y
                        }
                    }
                )
            }
    ) {
        // ExoPlayer implementation
        val exoPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                setMediaItem(MediaItem.fromUri(video.uri))
                prepare()
            }
        }

        DisposableEffect(video.uri) {
            exoPlayer.setMediaItem(MediaItem.fromUri(video.uri))
            exoPlayer.prepare()
            exoPlayer.play()

            onDispose {
                exoPlayer.release()
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}