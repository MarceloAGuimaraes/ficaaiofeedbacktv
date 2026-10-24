package com.ficaaiofeedback.ficaaiofeedbacktv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens.SeriesListScreen
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens.SeriesSeasonScreen
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens.VideoPlayerScreen
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.theme.BrandPurpleLight
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.theme.FicaAiOFeedbackTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FicaAiOFeedbackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(
                        containerColor = BrandPurpleLight,
                        contentColor = Color.Black,
                    ),
                ) {
                    FicaAiOFeedbackApp()
                }
            }
        }
    }
}

@Composable
private fun FicaAiOFeedbackApp() {
    var selectedSeries by remember { mutableStateOf<Series?>(null) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }

    BackHandler(enabled = selectedEpisode != null) { selectedEpisode = null }
    BackHandler(enabled = selectedEpisode == null && selectedSeries != null) { selectedSeries = null }

    val series = selectedSeries
    val episode = selectedEpisode
    when {
        series != null && episode != null -> VideoPlayerScreen(series = series, episode = episode)
        series != null -> SeriesSeasonScreen(series = series, onEpisodeClick = { selectedEpisode = it })
        else -> SeriesListScreen(onSeriesClick = { selectedSeries = it })
    }
}