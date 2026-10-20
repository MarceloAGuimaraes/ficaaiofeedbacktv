package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

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
import com.ficaaiofeedback.ficaaiofeedbacktv.data.FixedSeriesRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.data.SeriesRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.components.AppTitle
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.components.SeriesCard

@Composable
fun SeriesListScreen(
    modifier: Modifier = Modifier,
    repository: SeriesRepository = remember { FixedSeriesRepository() },
    onSeriesClick: (Series) -> Unit = {},
) {
    var series by remember { mutableStateOf<List<Series>>(emptyList()) }
    var focusedSeriesId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(repository) {
        series = repository.getSeries()
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTitle(modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp))
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
