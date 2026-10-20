package com.ficaaiofeedback.ficaaiofeedbacktv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun AppTitle(modifier: Modifier = Modifier) {
    val title = buildAnnotatedString {
        append("Nossas séries")
        withStyle(SpanStyle(color = Color.Red)) {
            append("❤")
        }
    }
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        modifier = modifier,
    )
}
