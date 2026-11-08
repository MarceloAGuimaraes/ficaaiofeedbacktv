package com.ficaaiofeedback.ficaaiofeedbacktv.ui.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig

/** Log tag shared by the player screen and its helpers, so `adb logcat -s VideoPlayerScreen` shows everything. */
internal const val TAG = "VideoPlayerScreen"

@OptIn(UnstableApi::class)
internal class PlayerLogger : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        val state = when (playbackState) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> playbackState.toString()
        }
        Log.d(TAG, "Playback state: $state")
    }

    override fun onTracksChanged(tracks: Tracks) {
        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                Log.d(
                    TAG,
                    "Text track ${format.sampleMimeType}/${format.codecs} " +
                        "supported=${group.isTrackSupported(i)} selected=${group.isTrackSelected(i)}",
                )
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "Playback error ${error.errorCodeName}: ${error.message}", error)
        // URLs carry access tokens, so request details are only logged in debug builds.
        if (!BuildConfig.DEBUG) return
        when (val cause = error.cause) {
            is HttpDataSource.InvalidResponseCodeException -> Log.e(
                TAG,
                "HTTP ${cause.responseCode} for ${cause.dataSpec.uri}\n" +
                    "headers: ${cause.headerFields}\n" +
                    "body: ${cause.responseBody.decodeToString().take(500)}",
            )
            is HttpDataSource.HttpDataSourceException ->
                Log.e(TAG, "Network failure for ${cause.dataSpec.uri}", cause)
        }
    }
}
