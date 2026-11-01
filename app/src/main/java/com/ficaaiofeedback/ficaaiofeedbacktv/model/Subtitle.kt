package com.ficaaiofeedback.ficaaiofeedbacktv.model

data class Subtitle(
    val id: String,
    val label: String,
    /** Gzipped SRT on OpenSubtitles. */
    val downloadUrl: String,
    /** Text encoding of the SRT inside the gzip, e.g. CP1252. */
    val encoding: String,
    val score: Double,
)
