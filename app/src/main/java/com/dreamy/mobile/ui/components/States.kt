package com.dreamy.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.ui.theme.Coral
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.ui.theme.Navy

/** Loading / error / content state of data fetched for a screen. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val data: T) : Load<T>
}

fun <T> Result<T>.toLoad(): Load<T> = fold({ Load.Ready(it) }, { Load.Failed(it.message.orEmpty()) })

/** Renders a spinner, an error card with retry, or [content]. Empty states are handled by [content]. */
@Composable
fun <T> LoadContent(state: Load<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(state.message, onRetry)
        is Load.Ready -> content(state.data)
    }
}

@Composable
fun LoadingState() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = Cyan) }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    DreamyCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Muted(message)
            PrimaryButton("Réessayer", onRetry, Modifier.padding(top = 16.dp))
        }
    }
}

/** `role="alert"` message: coral tint. */
@Composable
fun ErrorBanner(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    Text(
        message,
        modifier = modifier
            .fillMaxWidth()
            .background(Coral.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        color = Navy,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** `role="status"` message: light blue tint. */
@Composable
fun InfoBanner(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    Text(
        message,
        modifier = modifier
            .fillMaxWidth()
            .background(Light, RoundedCornerShape(12.dp))
            .padding(12.dp),
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** Replaces `window.confirm()`. */
@Composable
fun ConfirmDialog(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text("Confirmer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
