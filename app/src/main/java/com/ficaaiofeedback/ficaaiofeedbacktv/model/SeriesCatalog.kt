package com.ficaaiofeedback.ficaaiofeedbacktv.model

data class SeriesCatalog(
    val watching: List<Series> = emptyList(),
    val toWatch: List<Series> = emptyList(),
)
