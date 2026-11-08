package com.ficaaiofeedback.ficaaiofeedbacktv.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * TV Material's Card and Button only react to D-pad Enter, so taps on touch devices do nothing.
 * This makes a tap call [onClick] too; remote focus and key handling are untouched.
 */
@Composable
fun Modifier.tapToClick(onClick: () -> Unit): Modifier {
    val currentOnClick by rememberUpdatedState(onClick)
    return pointerInput(Unit) { detectTapGestures { currentOnClick() } }
}
