package com.ficaaiofeedback.ficaaiofeedbacktv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.ficaaiofeedback.ficaaiofeedbacktv.model.PlaybackServer

/** Asks which server to play from before the player opens. Back dismisses it. */
@Composable
fun ServerPickerDialog(onSelect: (PlaybackServer) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val firstOption = remember { FocusRequester() }
        Column(
            modifier = Modifier
                .width(300.dp)
                .background(Color(0xFF1C1C1C), RoundedCornerShape(16.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Escolha o servidor",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            PlaybackServer.entries.forEachIndexed { index, server ->
                Button(
                    onClick = { onSelect(server) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .tapToClick { onSelect(server) }
                        .then(if (index == 0) Modifier.focusRequester(firstOption) else Modifier),
                    // The theme's focused color is close to this dialog's background, so the focused
                    // option looked empty; white makes it stand out.
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0xFF3A3A3A),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text(text = server.label)
                }
            }
        }
        // Servidor 1 starts focused, so one press of the remote keeps today's behavior.
        LaunchedEffect(Unit) { firstOption.requestFocus() }
    }
}
