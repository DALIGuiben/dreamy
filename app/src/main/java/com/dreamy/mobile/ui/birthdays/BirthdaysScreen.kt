package com.dreamy.mobile.ui.birthdays

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.EventList
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PaleBox
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import com.dreamy.mobile.util.formatFr
import com.dreamy.mobile.util.nextBirthday

/** `app/{mother,admin}/birthdays/page.tsx`. */
@Composable
fun BirthdaysScreen(
    modifier: Modifier,
    admin: Boolean,
    onEvent: (String) -> Unit,
    onPrepareBirthday: () -> Unit,
    viewModel: BirthdaysViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        if (admin) {
            PageHeader("Moments spéciaux", "Anniversaires")
            PrimaryButton("Préparer un anniversaire", onPrepareBirthday)
        } else {
            PageHeader("Moments spéciaux", "Anniversaires", "Les dates à venir et les projets à relire ensemble.")
        }
        LoadContent(state, viewModel::refresh) { data ->
            DreamyCard {
                SectionTitle("Prochains anniversaires")
                if (data.upcoming.isEmpty()) {
                    Muted("Aucun anniversaire à afficher.", Modifier.padding(top = 12.dp), MaterialTheme.typography.bodyMedium)
                }
                data.upcoming.forEach { child ->
                    PaleBox(Modifier.padding(top = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(child.firstName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Muted(child.birthDate.nextBirthday().formatFr(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            SectionTitle("Projets d’anniversaire", Modifier.padding(top = 16.dp))
            EventList(
                events = data.projects,
                empty = if (admin) "Aucun anniversaire planifié." else "Aucun projet d’anniversaire pour le moment.",
                onEvent = onEvent,
                emptyActionLabel = if (admin) "Créer un événement" else null,
                onEmptyAction = if (admin) onPrepareBirthday else null,
            )
        }
    }
}
