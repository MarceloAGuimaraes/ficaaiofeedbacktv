package com.ficaaiofeedback.ficaaiofeedbacktv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series
import com.ficaaiofeedback.ficaaiofeedbacktv.ui.theme.BrandPurple

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeriesCard(
    series: Series,
    modifier: Modifier = Modifier,
    onClick: (Series) -> Unit = {},
    onFocusChanged: (Int) -> Unit = {},
    notFocused: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        onClick = { onClick(series) },
        interactionSource = interactionSource,
        modifier = modifier
            .size(width = 240.dp, height = 340.dp)
            .alpha(if (notFocused) 0.5f else 1f)
            .onFocusChanged { if (it.isFocused) onFocusChanged(series.id) },
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(3.dp, BrandPurple)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.15f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = series.posterUrl,
                contentDescription = series.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (isFocused) {
                FocusedCardOverlay(series)
            } else {
                CompactCardOverlay(series)
            }
        }
    }
}

@Composable
private fun BoxScope.CompactCardOverlay(series: Series) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                    startY = 100f,
                )
            )
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(12.dp, bottom = 20.dp)
    ) {
        Text(
            text = series.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Text(
            text = "Season ${series.season}",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun BoxScope.FocusedCardOverlay(series: Series) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            text = series.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = series.description,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            maxLines = 6,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
