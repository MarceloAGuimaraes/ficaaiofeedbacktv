package com.ficaaiofeedback.ficaaiofeedbacktv.ui.player

import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import com.ficaaiofeedback.ficaaiofeedbacktv.R
import com.ficaaiofeedback.ficaaiofeedbacktv.model.PlaybackServer

/**
 * Media3's controls have no quality menu, so this adds a button before the subtitle one that opens
 * media3's track dialog for the video variants. "Auto" lets the player switch quality on its own.
 */
@OptIn(UnstableApi::class)
internal fun PlayerView.addQualityButton(player: Player) =
    addControlButton(R.drawable.ic_quality, "Qualidade") {
        TrackSelectionDialogBuilder(context, "Qualidade", player, C.TRACK_TYPE_VIDEO)
            .setShowDisableOption(false)
            .setTrackNameProvider { format ->
                if (format.height > 0) "${format.height}p" else "${format.bitrate / 1000} kbps"
            }
            .build()
            .show()
    }

/** Adds a button that opens a dropdown of playback servers, with the current one checked. */
internal fun PlayerView.addServerButton(current: () -> PlaybackServer, onSelect: (PlaybackServer) -> Unit) =
    addControlButton(R.drawable.ic_server, "Servidor") { button ->
        val menu = PopupMenu(context, button)
        PlaybackServer.entries.forEach { menu.menu.add(0, it.ordinal, it.ordinal, it.label) }
        menu.menu.setGroupCheckable(0, true, true)
        menu.menu.findItem(current().ordinal).isChecked = true
        menu.setOnMenuItemClickListener { item ->
            val selected = PlaybackServer.entries[item.itemId]
            if (selected != current()) onSelect(selected)
            true
        }
        menu.show()
    }

/** Adds a button to the left of media3's own buttons (subtitles, settings) in the control bar. */
private fun PlayerView.addControlButton(icon: Int, description: String, onClick: (View) -> Unit) {
    val controls = findViewById<ViewGroup>(androidx.media3.ui.R.id.exo_basic_controls) ?: return
    val button = ImageButton(context, null, 0, androidx.media3.ui.R.style.ExoStyledControls_Button_Bottom)
    button.setImageResource(icon)
    button.contentDescription = description
    button.setOnClickListener(onClick)
    // Layout attributes in a style aren't applied to views created in code, so set them here.
    val params = LinearLayout.LayoutParams(
        resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_small_icon_width),
        resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_small_icon_height),
    )
    val margin = resources.getDimensionPixelSize(androidx.media3.ui.R.dimen.exo_small_icon_horizontal_margin)
    params.setMargins(margin, 0, margin, 0)
    controls.addView(button, 0, params)
}
