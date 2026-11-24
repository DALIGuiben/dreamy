package com.dreamy.mobile.ui.mother

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.ui.components.Avatar
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.EmptyState
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.Pill
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import com.dreamy.mobile.ui.components.StatCard
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.util.age
import com.dreamy.mobile.util.formatFr
import com.dreamy.mobile.util.nextBirthday

/** `app/mother/dashboard/page.tsx`. */
@Composable
fun MotherDashboardScreen(
    modifier: Modifier,
    userName: String,
    onChild: (String) -> Unit,
    onChildren: () -> Unit,
    onAddChild: () -> Unit,
    onBirthdays: () -> Unit,
    onEvent: (String) -> Unit,
    viewModel: MotherDashboardViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        PageHeader(
            eyebrow = "Votre espace",
            title = "Bonjour ${userName.substringBefore(' ')} 👋",
            subtitle = "Les petits détails qui rendent les grands moments uniques.",
        )
        LoadContent(state, viewModel::refresh) { data ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Outlined.People, data.children.size, "Enfants suivis", Modifier.weight(1f))
                StatCard(Icons.Outlined.FavoriteBorder, data.birthdays.size, "Prochains anniversaires", Modifier.weight(1f))
                StatCard(Icons.Outlined.CalendarMonth, data.pending, "Accords en attente", Modifier.weight(1f))
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Mes enfants", Modifier.weight(1f))
                Text("Tout voir →", fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onChildren))
            }
            if (data.children.isEmpty()) {
                EmptyState("Aucun enfant pour le moment.", "Ajouter un enfant", onAddChild)
            } else {
                data.children.take(4).forEach { child ->
                    DreamyCard(onClick = { onChild(child.id) }, contentPadding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(child.avatar)
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(child.firstName, fontWeight = FontWeight.Bold)
                                Muted("${child.birthDate.age()} ans", style = MaterialTheme.typography.bodyMedium)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
            PrimaryButton("Ajouter un enfant", onAddChild)

            SectionTitle("À venir", Modifier.padding(top = 8.dp))
            DreamyCard {
                Text("Anniversaires", style = MaterialTheme.typography.titleMedium)
                if (data.birthdays.isEmpty()) {
                    Muted("Aucun anniversaire à afficher.", Modifier.padding(top = 12.dp), MaterialTheme.typography.bodyMedium)
                }
                data.birthdays.forEachIndexed { index, child ->
                    if (index > 0) HorizontalDivider(color = Light)
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(child.firstName, Modifier.weight(1f))
                        Pill(child.birthDate.nextBirthday().formatFr(), coral = true)
                    }
                }
                Text(
                    "Voir les anniversaires →",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp).clickable(onClick = onBirthdays),
                )
            }
            DreamyCard {
                Text("Événements", style = MaterialTheme.typography.titleMedium)
                if (data.upcomingEvents.isEmpty()) {
                    Muted("Aucun événement à venir.", Modifier.padding(top = 12.dp), MaterialTheme.typography.bodyMedium)
                }
                data.upcomingEvents.forEachIndexed { index, event ->
                    if (index > 0) HorizontalDivider(color = Light)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(event.id) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(event.title, Modifier.weight(1f))
                        Muted(event.eventDate.formatFr(), Modifier.padding(start = 8.dp), MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
