package com.ficaaiofeedback.ficaaiofeedbacktv.data

import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.model.SeriesCatalog
import org.json.JSONObject

interface SeriesRepository {
    suspend fun getSeries(): SeriesCatalog
}

class RemoteSeriesRepository(
    private val url: String = "${BuildConfig.API_BASE_URL}/series/seasons",
) : SeriesRepository {
    override suspend fun getSeries(): SeriesCatalog = getFromApi(url) { body ->
        val catalog = JSONObject(body)
        SeriesCatalog(
            watching = catalog.getJSONArray("watching").map { it.toSeries() },
            toWatch = catalog.getJSONArray("to_watch").map { it.toSeries() },
        )
    }
}

private fun JSONObject.toSeries() = Series(
    id = getInt("id"),
    tmdbId = getInt("tmdb_id"),
    title = getString("name"),
    season = getInt("season"),
    posterUrl = getString("poster"),
    description = getString("overview"),
    episodes = getJSONArray("episodes").map { it.toEpisode() },
)

private fun JSONObject.toEpisode() = Episode(
    number = getInt("episode_number"),
    title = getString("name"),
)
