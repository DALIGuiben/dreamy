package com.dreamy.mobile.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.EmptyState
import com.dreamy.mobile.ui.components.Eyebrow
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import com.dreamy.mobile.ui.components.StatCard
import com.dreamy.mobile.util.formatFr
import com.dreamy.mobile.util.label

/** `app/admin/dashboard/page.tsx`. */
@Composable
fun AdminDashboardScreen(
    modifier: Modifier,
    onEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        PageHeader(
            eyebrow = "Organisation SOS",
            title = "Tableau de bord",
            subtitle = "Un aperçu clair des prochains moments à préparer.",
        )
        LoadContent(state, viewModel::refresh) { data ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Outlined.People, data.childCount, "Profils d’enfants", Modifier.weight(1f))
                StatCard(Icons.Outlined.CalendarMonth, data.upcomingEvents.size, "Événements à venir", Modifier.weight(1f))
                StatCard(Icons.Outlined.AssignmentTurnedIn, data.pending, "Accords en attente", Modifier.weight(1f))
            }
            SectionTitle("Prochains événements", Modifier.padding(top = 8.dp))
            PrimaryButton("Créer un événement", onCreateEvent)
            if (data.upcomingEvents.isEmpty()) {
                EmptyState("Aucun événement à venir.", "Créer un événement", onCreateEvent)
            }
            data.upcomingEvents.forEach { event ->
                DreamyCard(onClick = { onEvent(event.id) }) {
                    Eyebrow(event.type.label)
                    Text(event.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                    Muted(
                        "${event.eventDate.formatFr()} · ${event.participants.size} enfant(s)",
                        Modifier.padding(top = 8.dp),
                        MaterialTheme.typography.bodyMedium,
                    )
                    Text("Voir les détails →", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
