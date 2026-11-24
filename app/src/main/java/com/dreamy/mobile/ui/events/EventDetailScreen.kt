package com.dreamy.mobile.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.domain.model.ApprovalStatus
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventStatus
import com.dreamy.mobile.domain.model.EventType
import com.dreamy.mobile.ui.components.BulletSection
import com.dreamy.mobile.ui.components.ConfirmDialog
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.DreamyTextField
import com.dreamy.mobile.ui.components.ErrorBanner
import com.dreamy.mobile.ui.components.Eyebrow
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageTitle
import com.dreamy.mobile.ui.components.PaleBox
import com.dreamy.mobile.ui.components.Pill
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SecondaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import com.dreamy.mobile.ui.components.YesNoField
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.util.formatFr
import com.dreamy.mobile.util.label

/** `app/{mother,admin}/events/[id]/page.tsx`: `components/event-view.tsx` + `event-actions.tsx`. */
@Composable
fun EventDetailScreen(
    modifier: Modifier,
    admin: Boolean,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.event.collectAsStateWithLifecycle()
    val actions by viewModel.actions.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        LoadContent(state, viewModel::refresh) { event ->
            EventView(event, admin, onEdit)
            if (admin && !event.isLocked) AdminActions(event, actions, viewModel, onDeleted)
            if (!admin) MotherActions(event, actions, viewModel)
            ErrorBanner(actions.error)
        }
    }
}

@Composable
private fun EventView(event: Event, admin: Boolean, onEdit: (String) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Eyebrow(event.type.label)
            PageTitle(event.title, Modifier.padding(top = 6.dp))
            Muted("${event.eventDate.formatFr()} · ${event.location}", Modifier.padding(top = 6.dp))
        }
        Pill(event.status.label, coral = event.status == EventStatus.CHANGES_REQUESTED)
    }

    DreamyCard {
        SectionTitle("Le projet")
        event.category?.let { Muted(it, Modifier.padding(top = 8.dp), MaterialTheme.typography.bodyMedium) }
        event.description?.let { Text(it, Modifier.padding(top = 16.dp)) }
        event.theme?.let {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Thème : ") }
                    append(it)
                },
                Modifier.padding(top = 16.dp),
            )
        }
        BulletSection("Activités", event.plannedActivities)
        if (event.type == EventType.BIRTHDAY) BulletSection("Idées de cadeaux", event.suggestedGiftIdeas)
        event.preparationNotes?.let {
            Text("Notes de préparation", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
            Muted(it, Modifier.padding(top = 8.dp))
        }
    }

    DreamyCard {
        SectionTitle("Enfants concernés")
        event.participants.forEach { p ->
            PaleBox(Modifier.padding(top = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.childFirstName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Pill(p.approvalStatus.label)
                }
                p.motherComment?.let { Muted(it, Modifier.padding(top = 8.dp), MaterialTheme.typography.bodyMedium) }
            }
        }
    }

    if (event.feedback.isNotEmpty()) {
        DreamyCard {
            SectionTitle("Retours après l’événement")
            event.feedback.forEach { f ->
                PaleBox(Modifier.padding(top = 12.dp)) {
                    Text(
                        event.participants.firstOrNull { it.childId == f.childId }?.childFirstName.orEmpty(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Muted(
                        "Apprécié : ${if (f.enjoyed) "Oui" else "Non"} · À l’aise : ${if (f.comfortable) "Oui" else "Non"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    f.favoriteActivity?.let { Text("Activité préférée : $it", style = MaterialTheme.typography.bodyMedium) }
                    f.comment?.let { Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }

    DreamyCard {
        SectionTitle("Préparation")
        if (event.tasks.isEmpty()) {
            Muted("Aucune tâche ajoutée.", Modifier.padding(top = 12.dp), MaterialTheme.typography.bodyMedium)
        }
        event.tasks.forEach { task ->
            Row(Modifier.padding(top = 12.dp)) {
                Text(if (task.completed) "✓" else "○", color = if (task.completed) Cyan else Navy)
                Text(
                    task.title,
                    Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                )
            }
        }
    }
    if (admin && !event.isLocked) {
        SecondaryButton("Modifier le projet", { onEdit(event.id) }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AdminActions(
    event: Event,
    actions: EventActionsState,
    viewModel: EventDetailViewModel,
    onDeleted: () -> Unit,
) {
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val status = event.status
    DreamyCard {
        SectionTitle("Suivi du projet")
        Muted("Demandez l’accord des mamans avant de confirmer.", Modifier.padding(top = 4.dp), MaterialTheme.typography.bodyMedium)
        FlowRow(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val enabled = !actions.busy
            if (status == EventStatus.DRAFT || status == EventStatus.CHANGES_REQUESTED) {
                PrimaryButton("Demander les accords", { viewModel.changeStatus(EventStatus.AWAITING_APPROVAL) }, enabled = enabled)
            }
            if (status == EventStatus.AWAITING_APPROVAL) {
                PrimaryButton("Confirmer l’événement", { viewModel.changeStatus(EventStatus.CONFIRMED) }, enabled = enabled)
            }
            if (status == EventStatus.CONFIRMED) {
                PrimaryButton("Marquer comme terminé", { viewModel.changeStatus(EventStatus.COMPLETED) }, enabled = enabled)
            }
            SecondaryButton(
                "Annuler",
                { confirm = "Annuler cet événement ?" to { viewModel.changeStatus(EventStatus.CANCELLED) } },
                enabled = enabled,
            )
            if (status == EventStatus.DRAFT) {
                SecondaryButton(
                    "Supprimer le brouillon",
                    { confirm = "Supprimer ce brouillon ?" to { viewModel.deleteDraft(onDeleted) } },
                    enabled = enabled,
                )
            }
        }
    }
    DreamyCard {
        SectionTitle("Liste de préparation")
        event.tasks.forEach { task ->
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = { viewModel.setTaskCompleted(task.id, it) },
                    enabled = !actions.busy,
                    colors = CheckboxDefaults.colors(checkedColor = Cyan),
                    modifier = Modifier.semantics { contentDescription = "Terminer ${task.title}" },
                )
                Text(task.title, Modifier.weight(1f))
                TextButton(onClick = { viewModel.deleteTask(task.id) }, enabled = !actions.busy) {
                    Text("Supprimer", color = Navy, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DreamyTextField(actions.taskTitle, viewModel::onTaskTitle, label = null, placeholder = "Nouvelle tâche", modifier = Modifier.weight(1f))
            SecondaryButton("Ajouter", viewModel::addTask, enabled = !actions.busy && actions.taskTitle.isNotBlank())
        }
    }
    confirm?.let { (text, action) -> ConfirmDialog(text, action) { confirm = null } }
}

@Composable
private fun MotherActions(event: Event, actions: EventActionsState, viewModel: EventDetailViewModel) {
    val reviewOpen = event.status == EventStatus.AWAITING_APPROVAL || event.status == EventStatus.CHANGES_REQUESTED
    DreamyCard {
        SectionTitle("Votre avis")
        event.participants.forEach { p ->
            PaleBox(Modifier.padding(top = 20.dp)) {
                Text(p.childFirstName, fontWeight = FontWeight.SemiBold)
                Muted("Statut : ${p.approvalStatus.label}", Modifier.padding(top = 4.dp), MaterialTheme.typography.bodyMedium)
                if (reviewOpen) {
                    DreamyTextField(
                        actions.comments[p.id].orEmpty(),
                        { viewModel.onComment(p.id, it) },
                        "Votre message (facultatif)",
                        modifier = Modifier.padding(top = 12.dp),
                        minLines = 2,
                    )
                    FlowRow(
                        Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PrimaryButton("Approuver", { viewModel.review(p.id, ApprovalStatus.APPROVED) }, enabled = !actions.busy)
                        SecondaryButton(
                            "Demander un changement",
                            { viewModel.review(p.id, ApprovalStatus.CHANGES_REQUESTED) },
                            enabled = !actions.busy,
                        )
                        SecondaryButton("Décliner", { viewModel.review(p.id, ApprovalStatus.DECLINED) }, enabled = !actions.busy)
                    }
                }
                if (event.status == EventStatus.COMPLETED) {
                    val draft = viewModel.feedbackDraft(p.childId)
                    Text("Retour après l’événement", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                    if (event.feedback.any { it.childId == p.childId }) {
                        Muted(
                            "Un retour est déjà enregistré. Vous pouvez le modifier.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        YesNoField("L’enfant a apprécié ?", draft.enjoyed) { v -> viewModel.onFeedback(p.childId) { it.copy(enjoyed = v) } }
                        YesNoField("L’enfant était à l’aise ?", draft.comfortable) { v ->
                            viewModel.onFeedback(p.childId) { it.copy(comfortable = v) }
                        }
                        DreamyTextField(
                            draft.favoriteActivity,
                            { v -> viewModel.onFeedback(p.childId) { it.copy(favoriteActivity = v) } },
                            label = null,
                            placeholder = "Activité préférée",
                        )
                        DreamyTextField(
                            draft.comment,
                            { v -> viewModel.onFeedback(p.childId) { it.copy(comment = v) } },
                            label = null,
                            placeholder = "Commentaire",
                        )
                    }
                    PrimaryButton(
                        "Enregistrer le retour",
                        { viewModel.saveFeedback(p.childId) },
                        Modifier.padding(top = 12.dp),
                        enabled = !actions.busy,
                    )
                }
            }
        }
    }
}
