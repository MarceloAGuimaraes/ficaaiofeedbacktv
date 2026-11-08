package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.data.RemoteSubtitleRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.data.SubtitleRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.PlaybackServer
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.player.PlayerLogger
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.player.PlaylistAwareDataSource
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.player.SubtitleDataSource
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.player.TAG
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.player.addQualityButton
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.player.addServerButton
import java.io.IOException

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    series: Series,
    episode: Episode,
    modifier: Modifier = Modifier,
    initialServer: PlaybackServer = PlaybackServer.SERVER_1,
    subtitleRepository: SubtitleRepository = remember { RemoteSubtitleRepository() },
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        // Playlist and segment requests both hit our API; anything off-host goes out without the key.
        val apiHost = Uri.parse(BuildConfig.API_BASE_URL).host
        fun apiDataSourceFactory(readTimeoutMs: Int) =
            ResolvingDataSource.Factory(DefaultHttpDataSource.Factory().setReadTimeoutMs(readTimeoutMs)) { dataSpec ->
                if (dataSpec.uri.host == apiHost) {
                    dataSpec.withAdditionalHeaders(mapOf("api-key" to BuildConfig.API_KEY))
                } else {
                    dataSpec
                }
            }
        // Building a playlist runs a browser on the backend and can take well over a minute, so
        // get_playlist waits up to 2 minutes. Everything else keeps 30s, so a stalled chunk is
        // retried quickly instead of freezing playback (ExoPlayer's default is 8s).
        val playlistDataSourceFactory = apiDataSourceFactory(readTimeoutMs = 120_000)
        val mediaDataSourceFactory = apiDataSourceFactory(readTimeoutMs = 30_000)
        val dataSourceFactory = DataSource.Factory {
            SubtitleDataSource(
                PlaylistAwareDataSource(
                    playlist = playlistDataSourceFactory.createDataSource(),
                    media = mediaDataSourceFactory.createDataSource(),
                )
            )
        }
        // Segment delivery swings around the video's bitrate, so build a deep buffer during fast
        // periods and wait for a 10s cushion before starting or resuming, instead of the 1-2s defaults.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 120_000,
                /* maxBufferMs = */ 120_000,
                /* bufferForPlaybackMs = */ 10_000,
                /* bufferForPlaybackAfterRebufferMs = */ 10_000,
            )
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }
    DisposableEffect(Unit) {
        val listener = PlayerLogger()
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    var server by remember { mutableStateOf(initialServer) }
    var subtitleConfigurations by remember { mutableStateOf<List<MediaItem.SubtitleConfiguration>?>(null) }
    LaunchedEffect(Unit) {
        // Subtitles are optional: if the list can't be loaded, play without them.
        val subtitles = try {
            subtitleRepository.getSubtitles(series.id, episode.number)
        } catch (e: IOException) {
            Log.w(TAG, "Subtitles unavailable, playing without them", e)
            emptyList()
        }
        subtitleConfigurations = subtitles.mapIndexed { index, subtitle ->
            MediaItem.SubtitleConfiguration.Builder(SubtitleDataSource.uriFor(subtitle))
                .setId(subtitle.id)
                .setLabel(subtitle.label)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                // The best-scored option starts on; viewers can pick another or switch them off.
                .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
    }
    LaunchedEffect(server, subtitleConfigurations) {
        val subtitles = subtitleConfigurations ?: return@LaunchedEffect
        val playlistUrl = "${BuildConfig.API_BASE_URL}/series/seasons/${series.id}/episodes/${episode.number}/get_playlist?server=${server.id}"
        Log.v(TAG, playlistUrl)
        // A new server can offer different qualities, so drop any fixed choice and go back to Auto.
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .build()
        // The URLs have no file extensions, so ExoPlayer needs to be told the formats. Switching
        // servers resumes where the viewer was; the first load starts at 0.
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(playlistUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setSubtitleConfigurations(subtitles)
                .build(),
            exoPlayer.currentPosition,
        )
        exoPlayer.prepare()
        exoPlayer.play()
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
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowShuffleButton(false)
                    setShowSubtitleButton(true)
                    addQualityButton(exoPlayer)
                    addServerButton(current = { server }, onSelect = { server = it })
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { playerView -> playerView.requestFocus() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
