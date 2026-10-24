package com.ficaaiofeedback.ficaaiofeedbacktv.model

data class Series(
    val id: Int,
    val tmdbId: Int,
    val title: String,
    val season: Int,
    val posterUrl: String,
    val description: String,
    val episodes: List<Episode>,
)
