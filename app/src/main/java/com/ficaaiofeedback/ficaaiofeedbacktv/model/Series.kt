package com.ficaaiofeedback.ficaaiofeedbacktv.model

data class Series(
    val id: Int,
    val title: String,
    val portugueseTitle: String,
    val season: Int,
    val posterUrl: String,
    val description: String,
    val imdbLink: String,
    val streamUrl: String,
    val episodes: List<Episode>,
)
