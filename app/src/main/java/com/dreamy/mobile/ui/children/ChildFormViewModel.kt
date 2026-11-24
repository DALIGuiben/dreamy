package com.dreamy.mobile.ui.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dreamy.mobile.data.remote.ChildRequest
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.navigation.ChildFormRoute
import com.dreamy.mobile.ui.components.Load
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Comma-separated list fields of `components/child-form.tsx`: label and placeholder. */
enum class ChildListField(val label: String, val placeholder: String) {
    Personality("Personnalité", "Curieux, calme"),
    Interests("Centres d’intérêt", "Dessin, animaux"),
    Activities("Activités préférées", "Peinture, lecture"),
    Colors("Couleurs préférées", "Bleu, vert"),
    Themes("Thèmes d’anniversaire", "Nature, espace"),
    Gifts("Souhaits de cadeaux", "Crayons de couleur"),
    Comfort("Préférences de confort", "Petits groupes, endroit calme"),
}

data class ChildFormState(
    val firstName: String = "",
    val birthDate: LocalDate? = null,
    val avatar: String = "🌟",
    val description: String = "",
    val lists: Map<ChildListField, String> = emptyMap(),
    val summary: String = "",
    val firstNameError: String? = null,
    val birthDateError: String? = null,
    val error: String? = null,
    val submitting: Boolean = false,
)

@HiltViewModel
class ChildFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChildRepository,
) : ViewModel() {
    val childId: String? = savedStateHandle.toRoute<ChildFormRoute>().id

    /** Prefill load for the edit form; immediately ready for a new child. */
    private val _load = MutableStateFlow<Load<String?>>(if (childId == null) Load.Ready(null) else Load.Loading)
    val load: StateFlow<Load<String?>> = _load.asStateFlow()

    private val _form = MutableStateFlow(ChildFormState())
    val form: StateFlow<ChildFormState> = _form.asStateFlow()

    init {
        if (childId != null) loadChild(childId)
    }

    fun retry() {
        childId?.let(::loadChild)
    }

    private fun loadChild(id: String) {
        _load.value = Load.Loading
        viewModelScope.launch {
            repository.child(id)
                .onSuccess { child ->
                    _form.value = child.toForm()
                    _load.value = Load.Ready(child.firstName)
                }
                .onFailure { _load.value = Load.Failed(it.message.orEmpty()) }
        }
    }

    fun update(transform: (ChildFormState) -> ChildFormState) = _form.update(transform)

    fun submit(onSaved: (String) -> Unit) {
        val f = _form.value
        if (f.submitting) return
        val firstNameError = if (f.firstName.isBlank()) "Prénom requis" else null
        val birthDateError = if (f.birthDate == null) "Date requise" else null
        _form.update { it.copy(firstNameError = firstNameError, birthDateError = birthDateError, error = null) }
        if (firstNameError != null || f.birthDate == null) return

        fun list(field: ChildListField) =
            f.lists[field].orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        val body = ChildRequest(
            firstName = f.firstName.trim(),
            birthDate = f.birthDate.toString(),
            avatar = f.avatar.ifBlank { null },
            description = f.description.ifBlank { null },
            personalityTags = list(ChildListField.Personality),
            interests = list(ChildListField.Interests),
            favoriteActivities = list(ChildListField.Activities),
            favoriteColors = list(ChildListField.Colors),
            birthdayThemes = list(ChildListField.Themes),
            giftWishes = list(ChildListField.Gifts),
            comfortPreferences = list(ChildListField.Comfort),
            approvedPlanningSummary = f.summary.ifBlank { null },
        )
        _form.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.save(childId, body)
                .onSuccess(onSaved)
                .onFailure { e -> _form.update { it.copy(error = e.message ?: "Enregistrement impossible.") } }
            _form.update { it.copy(submitting = false) }
        }
    }
}

private fun Child.toForm() = ChildFormState(
    firstName = firstName,
    birthDate = birthDate,
    avatar = avatar ?: "🌟",
    description = description.orEmpty(),
    lists = mapOf(
        ChildListField.Personality to personalityTags,
        ChildListField.Interests to interests,
        ChildListField.Activities to favoriteActivities,
        ChildListField.Colors to favoriteColors,
        ChildListField.Themes to birthdayThemes,
        ChildListField.Gifts to giftWishes,
        ChildListField.Comfort to comfortPreferences,
    ).mapValues { it.value.joinToString(", ") },
    summary = approvedPlanningSummary.orEmpty(),
)
