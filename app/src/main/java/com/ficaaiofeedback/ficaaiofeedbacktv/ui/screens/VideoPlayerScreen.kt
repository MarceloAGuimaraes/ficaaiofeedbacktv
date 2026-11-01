package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.R
import com.ficaaiofeedback.ficaaiofeedbacktv.data.RemoteSubtitleRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.data.SubtitleRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Subtitle
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    series: Series,
    episode: Episode,
    modifier: Modifier = Modifier,
    subtitleRepository: SubtitleRepository = remember { RemoteSubtitleRepository() },
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
            SubtitleDataSource(apiDataSourceFactory.createDataSource())
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
    LaunchedEffect(Unit) {
        // Subtitles are optional: if the list can't be loaded, play without them.
        val subtitles = try {
            subtitleRepository.getSubtitles(series.id, episode.number)
        } catch (e: IOException) {
            Log.w("VideoPlayerScreen", "Subtitles unavailable, playing without them", e)
            emptyList()
        }
        val subtitleConfigurations = subtitles.mapIndexed { index, subtitle ->
            MediaItem.SubtitleConfiguration.Builder(SubtitleDataSource.uriFor(subtitle))
                .setId(subtitle.id)
                .setLabel(subtitle.label)
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                // The best-scored option starts on; viewers can pick another or switch them off.
                .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        val playlistUrl = "${BuildConfig.API_BASE_URL}/series/seasons/${series.id}/episodes/${episode.number}/get_playlist"
        Log.v("VideoPlayerScreen", playlistUrl)
        // The URLs have no file extensions, so ExoPlayer needs to be told the formats.
        exoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(playlistUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .setSubtitleConfigurations(subtitleConfigurations)
                .build()
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
                    useController = true
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowShuffleButton(false)
                    setShowSubtitleButton(true)
                    addQualityButton(exoPlayer)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { playerView -> playerView.requestFocus() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Media3's controls have no quality menu, so this adds a button before the subtitle one that opens
 * media3's track dialog for the video variants. "Auto" lets the player switch quality on its own.
 */
@OptIn(UnstableApi::class)
private fun PlayerView.addQualityButton(player: Player) {
    val controls = findViewById<ViewGroup>(androidx.media3.ui.R.id.exo_basic_controls) ?: return
    val button = ImageButton(context, null, 0, androidx.media3.ui.R.style.ExoStyledControls_Button_Bottom)
    button.setImageResource(R.drawable.ic_quality)
    button.contentDescription = "Qualidade"
    button.setOnClickListener {
        TrackSelectionDialogBuilder(context, "Qualidade", player, C.TRACK_TYPE_VIDEO)
            .setShowDisableOption(false)
            .setTrackNameProvider { format ->
                if (format.height > 0) "${format.height}p" else "${format.bitrate / 1000} kbps"
            }
            .build()
            .show()
    }
    // Layout attributes in a style aren't applied to views created in code, so set them here.
    val params = LinearLayout.LayoutParams(
        resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_small_icon_width),
        resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_small_icon_height),
    )
    val margin = resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_small_icon_horizontal_margin)
    params.setMargins(margin, 0, margin, 0)
    controls.addView(button, 0, params)
}

/**
 * Subtitles come straight from OpenSubtitles as gzipped SRT in a legacy encoding (e.g. CP1252),
 * while the player reads SRT as UTF-8. This downloads, unzips and re-encodes them. Any failure
 * becomes an empty subtitle, so a broken subtitle never stops playback. Every other request
 * (playlists, chunks) passes straight through and fails as usual.
 */
@OptIn(UnstableApi::class)
private class SubtitleDataSource(private val upstream: DataSource) : DataSource {
    private var subtitle: ByteArrayInputStream? = null
    private var subtitleUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) =
        upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        val fragment = dataSpec.uri.fragment
        if (fragment == null || !fragment.startsWith(ENCODING_PREFIX)) return upstream.open(dataSpec)
        val encoding = fragment.removePrefix(ENCODING_PREFIX)
        val bytes = try {
            download(dataSpec.uri.buildUpon().fragment(null).build(), encoding)
        } catch (e: IOException) {
            // A cancelled download (the player stopping or switching tracks) isn't a subtitle failure.
            if (e !is InterruptedIOException && e.cause !is InterruptedIOException) {
                Log.w("VideoPlayerScreen", "Subtitle unavailable, playing without it", e)
            }
            ByteArray(0)
        }
        subtitleUri = dataSpec.uri
        subtitle = ByteArrayInputStream(bytes).apply { skip(dataSpec.position) }
        return (bytes.size - dataSpec.position).coerceAtLeast(0)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val subtitle = subtitle ?: return upstream.read(buffer, offset, length)
        if (length == 0) return 0
        val read = subtitle.read(buffer, offset, length)
        return if (read == -1) C.RESULT_END_OF_INPUT else read
    }

    override fun getUri(): Uri? = subtitleUri ?: upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        if (subtitle != null) emptyMap() else upstream.responseHeaders

    override fun close() {
        if (subtitle != null) {
            subtitle = null
            subtitleUri = null
        } else {
            upstream.close()
        }
    }

    private fun download(uri: Uri, encoding: String): ByteArray {
        val raw = try {
            upstream.open(DataSpec(uri))
            DataSourceUtil.readToEnd(upstream)
        } finally {
            upstream.close()
        }
        val isGzip = raw.size >= 2 && raw[0] == 0x1f.toByte() && raw[1] == 0x8b.toByte()
        val srt = if (isGzip) GZIPInputStream(raw.inputStream()).use { it.readBytes() } else raw
        val charset = try {
            Charset.forName(encoding)
        } catch (e: IllegalArgumentException) {
            Charsets.UTF_8
        }
        return String(srt, charset).toByteArray(Charsets.UTF_8)
    }

    companion object {
        // The encoding rides along in the URI fragment, which is never sent over the network.
        private const val ENCODING_PREFIX = "subtitle-encoding="

        fun uriFor(subtitle: Subtitle): Uri =
            Uri.parse(subtitle.downloadUrl).buildUpon()
                .fragment(ENCODING_PREFIX + subtitle.encoding)
                .build()
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
