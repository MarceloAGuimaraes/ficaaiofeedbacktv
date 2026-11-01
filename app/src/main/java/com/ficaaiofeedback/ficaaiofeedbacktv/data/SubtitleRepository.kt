package com.ficaaiofeedback.ficaaiofeedbacktv.data

import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Subtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

interface SubtitleRepository {
    /** The episode's subtitle options, best match first. */
    suspend fun getSubtitles(seasonId: Int, episodeNumber: Int): List<Subtitle>
}

class RemoteSubtitleRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) : SubtitleRepository {
    override suspend fun getSubtitles(seasonId: Int, episodeNumber: Int): List<Subtitle> =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/series/$seasonId/episodes/$episodeNumber/subtitles"
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
                    JSONArray(body).map { it.toSubtitle() }.sortedByDescending { it.score }
                } catch (e: JSONException) {
                    throw IOException("Invalid subtitles payload from $url", e)
                }
            } finally {
                connection.disconnect()
            }
        }

}

private fun JSONObject.toSubtitle() = Subtitle(
    id = getString("IDSubtitleFile"),
    label = getString("SubFileName").removeSuffix(".srt"),
    downloadUrl = getString("SubDownloadLink"),
    encoding = if (isNull("SubEncoding")) "UTF-8" else getString("SubEncoding"),
    score = getDouble("Score"),
)
