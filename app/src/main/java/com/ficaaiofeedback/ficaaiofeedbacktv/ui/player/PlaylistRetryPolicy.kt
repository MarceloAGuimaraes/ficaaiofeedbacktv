package com.ficaaiofeedback.ficaaiofeedbacktv.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * get_playlist is retried only once, 5s after the first failure, instead of media3's default of
 * several quick retries; a second failure ends playback with the error. Errors media3 never
 * retries (e.g. a malformed playlist) still fail right away, and other loads keep the defaults.
 */
@OptIn(UnstableApi::class)
internal class PlaylistRetryPolicy : DefaultLoadErrorHandlingPolicy() {
    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val defaultDelayMs = super.getRetryDelayMsFor(loadErrorInfo)
        val isPlaylist = loadErrorInfo.loadEventInfo.uri.path.orEmpty().endsWith("/get_playlist")
        if (!isPlaylist || defaultDelayMs == C.TIME_UNSET) return defaultDelayMs
        return if (loadErrorInfo.errorCount <= PLAYLIST_MAX_RETRIES) PLAYLIST_RETRY_DELAY_MS else C.TIME_UNSET
    }

    private companion object {
        const val PLAYLIST_MAX_RETRIES = 1
        const val PLAYLIST_RETRY_DELAY_MS = 5_000L
    }
}
