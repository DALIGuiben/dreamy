package com.dreamy.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventStatus
import com.dreamy.mobile.util.formatFr
import com.dreamy.mobile.util.label

/** `components/event-list.tsx`. */
@Composable
fun EventList(
    events: List<Event>,
    empty: String,
    onEvent: (String) -> Unit,
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
) {
    if (events.isEmpty()) {
        EmptyState(empty, emptyActionLabel, onEmptyAction)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        events.forEach { event ->
            DreamyCard(onClick = { onEvent(event.id) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Eyebrow(event.type.label, Modifier.weight(1f))
                    Pill(event.status.label, coral = event.status == EventStatus.CHANGES_REQUESTED)
                }
                Text(event.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
                Muted(
                    "${event.eventDate.formatFr()} · ${event.location}",
                    Modifier.padding(top = 8.dp),
                    MaterialTheme.typography.bodyMedium,
                )
                Muted("${event.participants.size} participant(s)", style = MaterialTheme.typography.bodyMedium)
                Text("Voir le projet →", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
