package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

import android.net.Uri
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import java.io.IOException

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
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setReadTimeoutMs(30_000)
        // Playlist and segment requests both hit our API; anything off-host goes out without the key.
        val apiHost = Uri.parse(BuildConfig.API_BASE_URL).host
        val apiDataSourceFactory = ResolvingDataSource.Factory(httpDataSourceFactory) { dataSpec ->
            if (dataSpec.uri.host == apiHost) {
                dataSpec.withAdditionalHeaders(mapOf("api-key" to BuildConfig.API_KEY))
            } else {
                dataSpec
            }
        }
        val dataSourceFactory = DataSource.Factory {
            SubtitleTolerantDataSource(apiDataSourceFactory.createDataSource())
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
        val playlistUrl = "${BuildConfig.API_BASE_URL}/series/seasons/${series.id}/episodes/${episode.number}/get_playlist"
        Log.v("VideoPlayerScreen", playlistUrl)
        val subtitleUrl = "${BuildConfig.API_BASE_URL}/series/${series.id}/episodes/${episode.number}/get_subtitle"
        val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            // Flagged as default so subtitles start on; viewers can switch them off.
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        // The URLs have no file extensions, so ExoPlayer needs to be told the formats.
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(playlistUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setSubtitleConfigurations(listOf(subtitle))
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
                    setShowSubtitleButton(true)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { playerView -> playerView.requestFocus() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Turns any failure loading the subtitle into an empty subtitle file, so a broken subtitle never
 * stops playback. Every other request (playlists, chunks) fails as usual.
 */
@OptIn(UnstableApi::class)
private class SubtitleTolerantDataSource(private val upstream: DataSource) : DataSource {
    private var isSubtitle = false
    private var failed = false

    override fun addTransferListener(transferListener: TransferListener) =
        upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        isSubtitle = dataSpec.uri.path.orEmpty().endsWith("/get_subtitle")
        failed = false
        return try {
            upstream.open(dataSpec)
        } catch (e: IOException) {
            if (!isSubtitle) throw e
            onSubtitleError(e)
            0
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (failed) return C.RESULT_END_OF_INPUT
        return try {
            upstream.read(buffer, offset, length)
        } catch (e: IOException) {
            if (!isSubtitle) throw e
            onSubtitleError(e)
            C.RESULT_END_OF_INPUT
        }
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() = upstream.close()

    private fun onSubtitleError(e: IOException) {
        failed = true
        Log.w("VideoPlayerScreen", "Subtitle unavailable, playing without it", e)
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

    override fun onTracksChanged(tracks: Tracks) {
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                Log.d(
                    "VideoPlayerScreen",
                    "Text track ${format.sampleMimeType}/${format.codecs} " +
                        "supported=${group.isTrackSupported(i)} selected=${group.isTrackSelected(i)}",
                )
            }
        }
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
