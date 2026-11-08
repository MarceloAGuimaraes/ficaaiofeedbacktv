package com.ficaaiofeedback.ficaaiofeedbacktv.data

import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Subtitle
import org.json.JSONArray
import org.json.JSONObject

interface SubtitleRepository {
    /** The episode's subtitle options, best match first. */
    suspend fun getSubtitles(seasonId: Int, episodeNumber: Int): List<Subtitle>
}

class RemoteSubtitleRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) : SubtitleRepository {
    override suspend fun getSubtitles(seasonId: Int, episodeNumber: Int): List<Subtitle> =
        getFromApi("$baseUrl/series/$seasonId/episodes/$episodeNumber/subtitles") { body ->
            JSONArray(body).map { it.toSubtitle() }.sortedByDescending { it.score }
        }
}

private fun JSONObject.toSubtitle() = Subtitle(
    id = getString("IDSubtitleFile"),
    label = getString("SubFileName").removeSuffix(".srt"),
    downloadUrl = getString("SubDownloadLink"),
    encoding = if (isNull("SubEncoding")) "UTF-8" else getString("SubEncoding"),
    score = getDouble("Score"),
)
