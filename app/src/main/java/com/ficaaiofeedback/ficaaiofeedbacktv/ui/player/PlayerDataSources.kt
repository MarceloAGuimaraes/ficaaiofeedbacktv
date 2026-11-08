package com.ficaaiofeedback.ficaaiofeedbacktv.ui.player

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Subtitle
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.charset.Charset
import java.util.zip.GZIPInputStream

/** Sends get_playlist requests through [playlist] and every other request through [media]. */
@OptIn(UnstableApi::class)
internal class PlaylistAwareDataSource(
    private val playlist: DataSource,
    private val media: DataSource,
) : DataSource {
    private var current = media

    override fun addTransferListener(transferListener: TransferListener) {
        playlist.addTransferListener(transferListener)
        media.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        current = if (dataSpec.uri.path.orEmpty().endsWith("/get_playlist")) playlist else media
        return current.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        current.read(buffer, offset, length)

    override fun getUri(): Uri? = current.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current.responseHeaders

    override fun close() = current.close()
}

/**
 * Subtitles come straight from OpenSubtitles as gzipped SRT in a legacy encoding (e.g. CP1252),
 * while the player reads SRT as UTF-8. This downloads, unzips and re-encodes them. Any failure
 * becomes an empty subtitle, so a broken subtitle never stops playback. Every other request
 * (playlists, chunks) passes straight through and fails as usual.
 */
@OptIn(UnstableApi::class)
internal class SubtitleDataSource(private val upstream: DataSource) : DataSource {
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
                Log.w(TAG, "Subtitle unavailable, playing without it", e)
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
