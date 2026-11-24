package com.dreamy.mobile.ui.children

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.NoteInputMethod
import com.dreamy.mobile.ui.components.ChildProfile
import com.dreamy.mobile.ui.components.ConfirmDialog
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.DreamyTextField
import com.dreamy.mobile.ui.components.DropdownField
import com.dreamy.mobile.ui.components.ErrorBanner
import com.dreamy.mobile.ui.components.Eyebrow
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PaleBox
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SecondaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import com.dreamy.mobile.ui.theme.Coral
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.util.formatShortFr

/** `app/{mother,admin}/children/[id]/page.tsx`. Admins see approved fields only, never notes. */
@Composable
fun ChildDetailScreen(
    modifier: Modifier,
    admin: Boolean,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: ChildDetailViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val childState by viewModel.child.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    PageColumn(modifier) {
        LoadContent(childState, viewModel::refresh) { child ->
            if (admin) {
                Eyebrow("Informations approuvées · Notes privées non accessibles")
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    SecondaryButton("Supprimer le profil", { confirmDelete = true }, enabled = notes.busy == null)
                    SecondaryButton("Modifier le profil", { onEdit(child.id) })
                }
                if (notes.errorFrom == Busy.Delete) ErrorBanner(notes.error)
            }
            ChildProfile(child)
            if (!admin) NotesSection(child, notes, viewModel)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Supprimer définitivement ce profil et ses notes ?",
            onConfirm = { viewModel.deleteChild(onDeleted) },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** `components/notes.tsx`: private notes, voice dictation and AI organisation. */
@Composable
private fun NotesSection(child: Child, state: NotesState, viewModel: ChildDetailViewModel) {
    val context = LocalContext.current
    var noteToDelete by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording() else viewModel.microphoneUnavailable()
    }
    val busy = state.busy != null

    SectionTitle("Notes de la maman", Modifier.padding(top = 16.dp))
    Muted(
        "Ces notes restent privées. Seules les informations que vous approuvez rejoignent le profil.",
        style = MaterialTheme.typography.bodyMedium,
    )
    DreamyCard {
        DreamyTextField(
            value = state.content,
            onValueChange = viewModel::onContent,
            label = if (state.editingId != null) "Modifier la note" else "Nouvelle note",
            placeholder = "Racontez une préférence, une envie ou une idée…",
            minLines = 5,
        )
        FlowRow(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.recording) {
                SecondaryButton("Arrêter · ${state.seconds}s / ${MAX_RECORDING_SECONDS}s", viewModel::stopRecording, icon = Icons.Outlined.Stop)
            } else {
                SecondaryButton(
                    "Enregistrer avec ma voix",
                    {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) viewModel.startRecording() else permission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    enabled = !busy,
                    icon = Icons.Outlined.Mic,
                )
            }
            DropdownField(
                label = null,
                options = listOf("fr" to "Français", "ar" to "Arabe"),
                selected = state.language,
                onSelect = viewModel::onLanguage,
                modifier = Modifier.width(160.dp),
            )
        }
        if (state.recording) {
            Text("● Enregistrement en cours", color = Coral, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        }
        state.audio?.let {
            PaleBox(Modifier.padding(top = 16.dp)) {
                Text(
                    "Enregistrement prêt (${state.seconds}s). L’audio sera envoyé à GroqCloud uniquement si vous lancez la transcription.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        if (state.busy == Busy.Transcription) "Transcription…" else "Transcrire",
                        viewModel::transcribe,
                        enabled = !busy,
                    )
                    SecondaryButton("Annuler", viewModel::discardAudio)
                }
            }
        }
        Muted(
            "La dictée utilise un service cloud tiers. Relisez et corrigez le texte avant de l’enregistrer. " +
                "L’arabe tunisien est pris en charge au mieux des capacités du service.",
            Modifier.padding(top = 12.dp),
            MaterialTheme.typography.bodySmall,
        )
        FlowRow(
            Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryButton(
                when {
                    state.busy == Busy.Save -> "Enregistrement…"
                    state.editingId != null -> "Enregistrer les modifications"
                    else -> "Enregistrer la note"
                },
                viewModel::saveNote,
                enabled = state.content.isNotBlank() && !busy,
            )
            if (state.editingId != null) SecondaryButton("Annuler", viewModel::cancelEdit)
            SecondaryButton(
                if (state.busy == Busy.Organize) "Organisation…" else "Organiser avec l’IA",
                viewModel::organize,
                enabled = state.content.isNotBlank() && !busy,
                icon = Icons.Outlined.AutoAwesome,
            )
        }
        if (state.errorFrom != Busy.Delete) ErrorBanner(state.error, Modifier.padding(top = 16.dp))
    }

    state.suggestions?.let { suggestions ->
        DreamyCard {
            Text("Suggestions à vérifier", style = MaterialTheme.typography.titleLarge)
            Muted(
                "Modifiez les éléments ci-dessous, puis approuvez ou rejetez. Rien n’est ajouté automatiquement.",
                Modifier.padding(top = 4.dp),
                MaterialTheme.typography.bodyMedium,
            )
            SuggestionField.entries.forEach { field ->
                DreamyTextField(
                    value = suggestions[field].orEmpty(),
                    onValueChange = { viewModel.onSuggestion(field, it) },
                    label = field.label,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Approuver ces éléments", viewModel::approveSuggestions, enabled = !busy)
                SecondaryButton("Rejeter", viewModel::rejectSuggestions)
            }
        }
    }

    child.notes.forEach { note ->
        DreamyCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (note.inputMethod == NoteInputMethod.VOICE) "NOTE DICTÉE" else "NOTE ÉCRITE") +
                        " · ${note.createdAt.formatShortFr()}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.edit(note) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Modifier la note", tint = Navy)
                }
                IconButton(onClick = { noteToDelete = note.id }, enabled = !busy) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Supprimer la note", tint = Navy)
                }
            }
            Text(note.content)
        }
    }
    if (child.notes.isEmpty()) Muted("Aucune note pour le moment.", style = MaterialTheme.typography.bodyMedium)

    noteToDelete?.let { id ->
        ConfirmDialog(
            "Supprimer cette note ?",
            onConfirm = { viewModel.deleteNote(id) },
            onDismiss = { noteToDelete = null },
        )
    }
}
