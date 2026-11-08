package com.ficaaiofeedback.ficaaiofeedbacktv.model

/** The backends a playlist can come from; [id] is sent to get_playlist as `?server=`. */
enum class PlaybackServer(val id: String, val label: String) {
    SERVER_1("server_1", "Servidor 1"),
    SERVER_2("server_2", "Servidor 2"),
}
