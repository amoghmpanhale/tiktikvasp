package com.example.tiktokvasp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.tiktokvasp.model.Video

@Composable
fun VideoPlayer(
    video: Video,
    onTap: () -> Unit,           // single-tap to play/pause
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Create & remember ExoPlayer
    val exoPlayer = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode    = Player.REPEAT_MODE_ONE
            playWhenReady = true         // auto-start
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
        }
    }

    DisposableEffect(video.uri) {
        // If URI changes, reset media
        exoPlayer.setMediaItem(MediaItem.fromUri(video.uri))
        exoPlayer.prepare()
        exoPlayer.play()
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // only handle taps here; let vertical drags fall through to ViewPager2
            .clickable { onTap() }
    ) {
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
