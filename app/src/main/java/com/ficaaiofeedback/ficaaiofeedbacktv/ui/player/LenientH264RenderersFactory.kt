package com.ficaaiofeedback.ficaaiofeedbacktv.ui.player

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoRendererEventListener

/**
 * Some TV boxes (e.g. Rockchip RK322x on Android 7) advertise lower H.264 levels than they really
 * decode, so media3 marks their 720p streams as unsupported: greyed out in the quality menu and
 * never picked by Auto. This video renderer treats such an H.264 stream as supported when the
 * profile/level is the only objection, i.e. a decoder on the device still accepts its size and
 * frame rate.
 */
@OptIn(UnstableApi::class)
internal class LenientH264RenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>,
    ) {
        // Same setup as DefaultRenderersFactory's video renderer; the app uses no extension renderers.
        val builder = MediaCodecVideoRenderer.Builder(context)
            .setCodecAdapterFactory(codecAdapterFactory)
            .setMediaCodecSelector(mediaCodecSelector)
            .setAllowedJoiningTimeMs(allowedVideoJoiningTimeMs)
            .setEnableDecoderFallback(enableDecoderFallback)
            .setEventHandler(eventHandler)
            .setEventListener(eventListener)
            .setMaxDroppedFramesToNotify(MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY)
        out.add(LenientH264VideoRenderer(builder))
    }
}

@OptIn(UnstableApi::class)
private class LenientH264VideoRenderer(builder: Builder) : MediaCodecVideoRenderer(builder) {
    override fun supportsFormat(mediaCodecSelector: MediaCodecSelector, format: Format): Int {
        val capabilities = super.supportsFormat(mediaCodecSelector, format)
        if (format.sampleMimeType != MimeTypes.VIDEO_H264 ||
            RendererCapabilities.getFormatSupport(capabilities) != C.FORMAT_EXCEEDS_CAPABILITIES
        ) {
            return capabilities
        }
        val sizeAndRateAccepted = MediaCodecUtil
            .getDecoderInfos(MimeTypes.VIDEO_H264, /* secure= */ false, /* tunneling= */ false)
            .any { it.isVideoSizeAndRateSupportedV21(format.width, format.height, format.frameRate.toDouble()) }
        if (!sizeAndRateAccepted) return capabilities
        Log.i(TAG, "Treating ${format.codecs} ${format.width}x${format.height} as supported despite its H.264 level")
        return RendererCapabilities.create(
            C.FORMAT_HANDLED,
            RendererCapabilities.getAdaptiveSupport(capabilities),
            RendererCapabilities.getTunnelingSupport(capabilities),
            RendererCapabilities.getHardwareAccelerationSupport(capabilities),
            RendererCapabilities.getDecoderSupport(capabilities),
        )
    }
}
