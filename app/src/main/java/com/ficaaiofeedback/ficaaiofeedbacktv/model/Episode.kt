package com.ficaaiofeedback.ficaaiofeedbacktv.model

data class Episode(
    val id: Int,
    val number: Int,
    val title: String,
    val link: String = "",
)
