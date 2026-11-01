package com.ficaaiofeedback.ficaaiofeedbacktv.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.ficaaiofeedback.ficaaiofeedbacktv.data.RemoteSeriesRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.data.SeriesRepository
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.model.SeriesCatalog
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.components.AppTitle
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.components.SeriesCard
import java.io.IOException

@Composable
fun SeriesListScreen(
    modifier: Modifier = Modifier,
    repository: SeriesRepository = remember { RemoteSeriesRepository() },
    onSeriesClick: (Series) -> Unit = {},
) {
    var catalog by remember { mutableStateOf(SeriesCatalog()) }
    var focusedSeriesId by remember { mutableStateOf<Int?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        try {
            catalog = repository.getSeries()
        } catch (e: IOException) {
            Log.e("SeriesListScreen", "Failed to load series", e)
            loadFailed = true
        }
    }

    // As many card-width columns as fit across the screen; the rest wrap into new rows. Columns
    // stretch to fill the width, so section titles get the whole line even with a single column.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fun seriesCards(series: List<Series>) = items(series) { item ->
            // The grid forces items to the column's width; the Box keeps the card at its own size.
            // Its bottom padding leaves room below each card row for the focused card's 1.15x
            // scale-up, while section titles sit closer to their cards.
            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                SeriesCard(
                    series = item,
                    notFocused = focusedSeriesId != null && focusedSeriesId != item.id,
                    onFocusChanged = { focusedSeriesId = item.id },
                    onClick = onSeriesClick,
                )
            }
        }

        fullWidthItem { AppTitle() }
        if (loadFailed) {
            fullWidthItem { Text(text = "Não foi possível carregar as séries.", fontSize = 18.sp) }
        }
        if (catalog.watching.isNotEmpty()) {
            fullWidthItem { SectionHeader("Continue assistindo") }
            seriesCards(catalog.watching)
        }
        if (catalog.toWatch.isNotEmpty()) {
            fullWidthItem { SectionHeader("Comece a assistir") }
            seriesCards(catalog.toWatch)
        }
    }
}

// A header that takes a whole row, so the cards after it start on a new line.
private fun LazyGridScope.fullWidthItem(content: @Composable () -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }) { content() }

@Composable
private fun SectionHeader(text: String) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.25f))
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            // The bottom padding keeps a focused (scaled-up) card in the next row off the title.
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
    }
}
