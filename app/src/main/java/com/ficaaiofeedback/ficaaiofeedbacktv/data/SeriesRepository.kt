package com.ficaaiofeedback.ficaaiofeedbacktv.data

import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.model.SeriesCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface SeriesRepository {
    suspend fun getSeries(): SeriesCatalog
}

class RemoteSeriesRepository(
    private val url: String = "${BuildConfig.API_BASE_URL}/series/seasons",
) : SeriesRepository {
    override suspend fun getSeries(): SeriesCatalog = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("api-key", BuildConfig.API_KEY)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GET $url failed with HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            try {
                val catalog = JSONObject(body)
                SeriesCatalog(
                    watching = catalog.getJSONArray("watching").map { it.toSeries() },
                    toWatch = catalog.getJSONArray("to_watch").map { it.toSeries() },
                )
            } catch (e: JSONException) {
                throw IOException("Invalid series payload from $url", e)
            }
        } finally {
            connection.disconnect()
        }
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

internal fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
