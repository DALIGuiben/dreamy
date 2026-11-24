package com.dreamy.mobile.ui.children

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.ui.components.Avatar
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.DreamyTextField
import com.dreamy.mobile.ui.components.EmptyState
import com.dreamy.mobile.ui.components.LoadContent
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PillRow
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.util.age

/** `app/mother/children/page.tsx` and `app/admin/children/page.tsx`. */
@Composable
fun ChildrenScreen(
    modifier: Modifier,
    admin: Boolean,
    onChild: (String) -> Unit,
    onAddChild: () -> Unit,
    viewModel: ChildrenViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    PageColumn(modifier) {
        if (admin) {
            PageHeader("Planification", "Enfants", "Informations approuvées pour préparer les événements.")
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DreamyTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    label = null,
                    placeholder = "Rechercher par prénom",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                )
                PrimaryButton("Chercher", viewModel::search)
            }
        } else {
            PageHeader("Profils", "Mes enfants", "Leurs envies, leurs idées et ce qui les met à l’aise.")
            PrimaryButton("Ajouter un enfant", onAddChild)
        }
        LoadContent(state, viewModel::refresh) { children ->
            if (children.isEmpty()) {
                if (admin) EmptyState("Aucun enfant trouvé.") else EmptyState("Aucun profil créé.", "Ajouter un enfant", onAddChild)
            }
            children.forEach { child ->
                DreamyCard(onClick = { onChild(child.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(child.avatar, size = if (admin) 48.dp else 56.dp)
                        Column(Modifier.padding(start = 12.dp)) {
                            if (admin) {
                                Text("${child.firstName} · ${child.birthDate.age()} ans", fontWeight = FontWeight.Bold)
                                Muted("Maman : ${child.motherName}", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(child.firstName, style = MaterialTheme.typography.titleLarge)
                                Muted("${child.birthDate.age()} ans", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    PillRow(child.interests.take(3), Modifier.padding(top = 16.dp))
                    Text("Voir le profil →", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
