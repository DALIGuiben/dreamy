package com.dreamy.mobile.ui.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.domain.model.EventType
import com.dreamy.mobile.ui.components.DateField
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.DreamyTextField
import com.dreamy.mobile.ui.components.DropdownField
import com.dreamy.mobile.ui.components.ErrorBanner
import com.dreamy.mobile.ui.components.InfoBanner
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SecondaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import com.dreamy.mobile.ui.components.TimeField
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import java.time.format.DateTimeFormatter

private val dateDisplay = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** `app/admin/events/new` and `.../[id]/edit` with `components/event-form.tsx`. */
@Composable
fun EventFormScreen(
    modifier: Modifier,
    onSaved: (String) -> Unit,
    viewModel: EventFormViewModel = hiltViewModel(),
) {
    val load by viewModel.load.collectAsStateWithLifecycle()
    val f by viewModel.form.collectAsStateWithLifecycle()
    val birthday = f.type == EventType.BIRTHDAY
    PageColumn(modifier) {
        LoadContent(load, viewModel::retry) { data ->
            if (data.editingTitle == null) {
                PageHeader("Nouveau projet", "Créer un événement")
            } else {
                PageHeader("Projet d’événement", "Modifier ${data.editingTitle}")
            }

            DreamyCard {
                SectionTitle("Informations essentielles")
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    DropdownField(
                        "Type",
                        listOf(EventType.SOS_EVENT to "Événement collectif SOS", EventType.BIRTHDAY to "Anniversaire"),
                        f.type,
                        { v -> viewModel.update { it.copy(type = v) } },
                    )
                    DreamyTextField(f.title, { v -> viewModel.update { it.copy(title = v, titleError = null) } }, "Titre *", error = f.titleError)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateField(
                            f.date,
                            { v -> viewModel.update { it.copy(date = v, dateError = null) } },
                            "Date *",
                            display = { it.format(dateDisplay) },
                            modifier = Modifier.weight(1f),
                            error = f.dateError,
                        )
                        TimeField(
                            f.time,
                            { v -> viewModel.update { it.copy(time = v, dateError = null) } },
                            "Heure *",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    DreamyTextField(
                        f.location,
                        { v -> viewModel.update { it.copy(location = v, locationError = null) } },
                        "Lieu *",
                        error = f.locationError,
                    )
                    DreamyTextField(
                        f.category,
                        { v -> viewModel.update { it.copy(category = v) } },
                        "Catégorie",
                        placeholder = "Atelier créatif, sortie culturelle…",
                    )
                    if (birthday) {
                        DropdownField(
                            "Enfant fêté *",
                            listOf("" to "Choisir") + data.options.map { it.id to it.firstName },
                            f.birthdayChildId,
                            { v -> viewModel.update { it.copy(birthdayChildId = v) } },
                        )
                    }
                    DreamyTextField(f.description, { v -> viewModel.update { it.copy(description = v) } }, "Description", minLines = 3)
                }
            }

            DreamyCard {
                SectionTitle("Enfants participants")
                Muted(
                    "Leur maman pourra donner son accord lorsque vous lui soumettrez le projet.",
                    Modifier.padding(top = 4.dp, bottom = 12.dp),
                    MaterialTheme.typography.bodyMedium,
                )
                data.options.forEach { child ->
                    val checked = child.id in f.childIds
                    Row(
                        Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Light), RoundedCornerShape(12.dp))
                            .toggleable(checked, role = Role.Checkbox) { viewModel.toggleChild(child.id, it) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Cyan), modifier = Modifier.padding(12.dp))
                        Column {
                            Text(child.firstName, fontWeight = FontWeight.Bold)
                            Muted(child.motherName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            DreamyCard {
                SectionTitle("Préparation")
                Muted(
                    "Chaque suggestion IA est modifiable et nécessite votre validation.",
                    Modifier.padding(top = 4.dp),
                    MaterialTheme.typography.bodyMedium,
                )
                SecondaryButton(
                    when {
                        f.suggesting -> "Génération…"
                        birthday -> "Suggérer un anniversaire"
                        else -> "Générer des activités avec l’IA"
                    },
                    viewModel::suggest,
                    Modifier.padding(top = 12.dp),
                    enabled = !f.suggesting,
                    icon = Icons.Outlined.AutoAwesome,
                )
                InfoBanner(f.aiNote, Modifier.padding(top = 16.dp))
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (birthday) DreamyTextField(f.theme, { v -> viewModel.update { it.copy(theme = v) } }, "Thème")
                    DreamyTextField(
                        f.activities,
                        { v -> viewModel.update { it.copy(activities = v) } },
                        "Activités (une par ligne)",
                        minLines = 5,
                    )
                    if (birthday) {
                        DreamyTextField(
                            f.giftIdeas,
                            { v -> viewModel.update { it.copy(giftIdeas = v) } },
                            "Idées de cadeaux (une par ligne)",
                            minLines = 5,
                        )
                    }
                    DreamyTextField(
                        f.checklist,
                        { v -> viewModel.update { it.copy(checklist = v) } },
                        "Liste de préparation (une tâche par ligne)",
                        minLines = 5,
                    )
                    DreamyTextField(
                        f.preparationNotes,
                        { v -> viewModel.update { it.copy(preparationNotes = v) } },
                        "Notes de préparation",
                        minLines = 5,
                    )
                }
            }

            ErrorBanner(f.error)
            PrimaryButton(
                when {
                    f.submitting -> "Enregistrement…"
                    viewModel.eventId != null -> "Enregistrer les modifications"
                    else -> "Créer le brouillon"
                },
                { viewModel.submit(onSaved) },
                enabled = !f.submitting,
            )
        }
    }
}
