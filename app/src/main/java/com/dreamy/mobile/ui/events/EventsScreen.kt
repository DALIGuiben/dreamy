package com.dreamy.mobile.ui.events

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.ui.components.EventList
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PrimaryButton

/** `app/{mother,admin}/events/page.tsx`. */
@Composable
fun EventsScreen(
    modifier: Modifier,
    admin: Boolean,
    onEvent: (String) -> Unit,
    onCreate: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        if (admin) {
            PageHeader("Organisation", "Événements SOS", "Préparer, demander les accords et suivre chaque projet.")
            PrimaryButton("Créer un événement", onCreate)
        } else {
            PageHeader(
                "À partager",
                "Événements",
                "Consultez les projets qui concernent vos enfants et donnez votre avis.",
            )
        }
        LoadContent(state, viewModel::refresh) { events ->
            EventList(
                events = events,
                empty = if (admin) "Aucun événement créé." else "Aucun événement à consulter pour le moment.",
                onEvent = onEvent,
                emptyActionLabel = if (admin) "Créer un événement" else null,
                onEmptyAction = if (admin) onCreate else null,
            )
        }
    }
}
