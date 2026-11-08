package com.ficaaiofeedback.ficaaiofeedbacktv.data

import com.ficaaiofeedback.ficaaiofeedbacktv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * GETs [url] from our API with the api-key header and parses the body with [parse]. A non-200
 * response or a body that doesn't parse is reported as an [IOException].
 */
internal suspend fun <T> getFromApi(url: String, parse: (String) -> T): T = withContext(Dispatchers.IO) {
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
            parse(body)
        } catch (e: JSONException) {
            throw IOException("Invalid payload from $url", e)
        }
    } finally {
        connection.disconnect()
    }
}

internal fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
