package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    series: Series,
    episode: Episode,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        // The backend takes ~10s to build the playlist; ExoPlayer's default read timeout is 8s.
        val dataSourceFactory = DefaultHttpDataSource.Factory().setReadTimeoutMs(30_000)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }
    DisposableEffect(Unit) {
        val listener = PlayerLogger()
        exoPlayer.addListener(listener)
        val playlistUrl = "${BuildConfig.API_BASE_URL}/series/seasons/${series.id}/episodes/${episode.number}/get_playlist"
        Log.v("VideoPlayerScreen", playlistUrl)
        // The URL has no .m3u8 extension, so ExoPlayer needs to be told it's HLS.
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(playlistUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        )
        exoPlayer.prepare()
        exoPlayer.play()
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowShuffleButton(false)
                    setShowSubtitleButton(false)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { playerView -> playerView.requestFocus() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(UnstableApi::class)
private class PlayerLogger : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        val state = when (playbackState) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> playbackState.toString()
        }
        Log.d("VideoPlayerScreen", "Playback state: $state")
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e("VideoPlayerScreen", "Playback error ${error.errorCodeName}: ${error.message}", error)
        // URLs carry access tokens, so request details are only logged in debug builds.
        if (!BuildConfig.DEBUG) return
        when (val cause = error.cause) {
            is HttpDataSource.InvalidResponseCodeException -> Log.e(
                "VideoPlayerScreen",
                "HTTP ${cause.responseCode} for ${cause.dataSpec.uri}\n" +
                    "headers: ${cause.headerFields}\n" +
                    "body: ${cause.responseBody.decodeToString().take(500)}",
            )
            is HttpDataSource.HttpDataSourceException ->
                Log.e("VideoPlayerScreen", "Network failure for ${cause.dataSpec.uri}", cause)
        }
    }
}
