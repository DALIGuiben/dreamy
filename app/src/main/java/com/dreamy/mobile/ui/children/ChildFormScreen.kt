package com.dreamy.mobile.ui.children

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.ui.components.DateField
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.DreamyTextField
import com.dreamy.mobile.ui.components.ErrorBanner
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SectionTitle
import java.time.format.DateTimeFormatter

private val dateDisplay = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** `app/mother/children/new` and `.../[id]/edit` with `components/child-form.tsx`. */
@Composable
fun ChildFormScreen(
    modifier: Modifier,
    onSaved: (String) -> Unit,
    viewModel: ChildFormViewModel = hiltViewModel(),
) {
    val load by viewModel.load.collectAsStateWithLifecycle()
    val f by viewModel.form.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        LoadContent(load, viewModel::retry) { firstName ->
            if (firstName == null) {
                PageHeader("Nouveau profil", "Ajouter un enfant")
            } else {
                PageHeader("Profil de $firstName", "Modifier le profil")
            }

            DreamyCard {
                SectionTitle("Informations de base", Modifier.padding(bottom = 20.dp))
                DreamyTextField(
                    f.firstName,
                    { v -> viewModel.update { it.copy(firstName = v, firstNameError = null) } },
                    "Prénom *",
                    error = f.firstNameError,
                )
                DateField(
                    f.birthDate,
                    { v -> viewModel.update { it.copy(birthDate = v, birthDateError = null) } },
                    "Date de naissance *",
                    display = { it.format(dateDisplay) },
                    modifier = Modifier.padding(top = 16.dp),
                    error = f.birthDateError,
                )
                DreamyTextField(
                    f.avatar,
                    { v -> viewModel.update { it.copy(avatar = v.take(4)) } },
                    "Avatar fictif (emoji)",
                    modifier = Modifier.padding(top = 16.dp),
                )
                DreamyTextField(
                    f.description,
                    { v -> viewModel.update { it.copy(description = v) } },
                    "Présentation",
                    modifier = Modifier.padding(top = 16.dp),
                    minLines = 3,
                )
            }

            DreamyCard {
                SectionTitle("Personnalité et envies")
                Muted(
                    "Séparez plusieurs éléments par une virgule. Tous ces champs sont facultatifs.",
                    Modifier.padding(top = 8.dp, bottom = 4.dp),
                    MaterialTheme.typography.bodyMedium,
                )
                ChildListField.entries.forEach { field ->
                    DreamyTextField(
                        f.lists[field].orEmpty(),
                        { v -> viewModel.update { it.copy(lists = it.lists + (field to v)) } },
                        field.label,
                        placeholder = field.placeholder,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }

            DreamyCard {
                Text("Résumé approuvé pour les organisateurs", style = MaterialTheme.typography.labelLarge)
                Muted(
                    "Partagez seulement les informations utiles à la préparation. Les notes privées restent dans votre espace.",
                    Modifier.padding(top = 4.dp, bottom = 12.dp),
                    MaterialTheme.typography.bodyMedium,
                )
                DreamyTextField(f.summary, { v -> viewModel.update { it.copy(summary = v) } }, label = null, minLines = 4)
            }

            ErrorBanner(f.error)
            PrimaryButton(
                when {
                    f.submitting -> "Enregistrement…"
                    viewModel.childId != null -> "Enregistrer les modifications"
                    else -> "Créer le profil"
                },
                { viewModel.submit(onSaved) },
                enabled = !f.submitting,
            )
        }
    }
}
