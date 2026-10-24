package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.ficaaiofeedback.ficaaiofeedbacktv.data.RemoteSeriesRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.data.SeriesRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.components.AppTitle
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.components.SeriesCard
import java.io.IOException

@Composable
fun SeriesListScreen(
    modifier: Modifier = Modifier,
    repository: SeriesRepository = remember { RemoteSeriesRepository() },
    onSeriesClick: (Series) -> Unit = {},
) {
    var series by remember { mutableStateOf<List<Series>>(emptyList()) }
    var focusedSeriesId by remember { mutableStateOf<Int?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        try {
            series = repository.getSeries()
        } catch (e: IOException) {
            Log.e("SeriesListScreen", "Failed to load series", e)
            loadFailed = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTitle(modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp))
        if (loadFailed) {
            Text(
                text = "Não foi possível carregar as séries.",
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(series) { item ->
                SeriesCard(
                    series = item,
                    notFocused = focusedSeriesId != null && focusedSeriesId != item.id,
                    onFocusChanged = { focusedSeriesId = item.id },
                    onClick = onSeriesClick,
                )
            }
        }
    }
}
