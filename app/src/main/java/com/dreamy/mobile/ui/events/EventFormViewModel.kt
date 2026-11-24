package com.dreamy.mobile.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dreamy.mobile.data.remote.EventRequest
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.data.repository.EventRepository
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventType
import com.dreamy.mobile.navigation.EventFormRoute
import com.dreamy.mobile.ui.components.Load
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class EventFormState(
    val type: EventType = EventType.SOS_EVENT,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val location: String = "",
    val birthdayChildId: String = "",
    val theme: String = "",
    /** Multi-line text areas: one item per line. */
    val activities: String = "",
    val giftIdeas: String = "",
    val checklist: String = "",
    val preparationNotes: String = "",
    val childIds: Set<String> = emptySet(),
    val titleError: String? = null,
    val dateError: String? = null,
    val locationError: String? = null,
    val error: String? = null,
    val aiNote: String? = null,
    val suggesting: Boolean = false,
    val submitting: Boolean = false,
)

/** Data the form needs before it can be shown: child options and, when editing, the event title. */
data class EventFormData(val options: List<Child>, val editingTitle: String?)

@HiltViewModel
class EventFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val children: ChildRepository,
    private val events: EventRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<EventFormRoute>()
    val eventId: String? = route.id

    private val _load = MutableStateFlow<Load<EventFormData>>(Load.Loading)
    val load: StateFlow<Load<EventFormData>> = _load.asStateFlow()

    private val _form = MutableStateFlow(EventFormState(type = if (route.birthday) EventType.BIRTHDAY else EventType.SOS_EVENT))
    val form: StateFlow<EventFormState> = _form.asStateFlow()

    init {
        retry()
    }

    fun retry() {
        _load.value = Load.Loading
        viewModelScope.launch {
            val options = async { children.children() }
            val event = eventId?.let { id -> async { events.event(id) } }
            _load.value = runCatching {
                val childList = options.await().getOrThrow()
                val existing = event?.await()?.getOrThrow()
                if (existing != null) {
                    check(!existing.isLocked) { "Cet événement ne peut plus être modifié." }
                    _form.value = existing.toForm()
                }
                EventFormData(childList, existing?.title)
            }.fold({ Load.Ready(it) }, { Load.Failed(it.message.orEmpty()) })
        }
    }

    fun update(transform: (EventFormState) -> EventFormState) = _form.update(transform)

    fun toggleChild(id: String, selected: Boolean) =
        _form.update { it.copy(childIds = if (selected) it.childIds + id else it.childIds - id) }

    /** Fills the preparation fields from the AI plan; the organiser reviews before saving. */
    fun suggest() {
        val f = _form.value
        if (f.suggesting) return
        val missing = when {
            f.type == EventType.BIRTHDAY && f.birthdayChildId.isEmpty() -> "Choisissez d’abord l’enfant à célébrer."
            f.type == EventType.SOS_EVENT && f.childIds.isEmpty() -> "Choisissez au moins un enfant."
            else -> null
        }
        if (missing != null) {
            _form.update { it.copy(error = missing) }
            return
        }
        _form.update { it.copy(suggesting = true, error = null) }
        viewModelScope.launch {
            val result = if (f.type == EventType.BIRTHDAY) {
                events.birthdayPlan(f.birthdayChildId).map { plan ->
                    { s: EventFormState ->
                        s.copy(
                            theme = plan.theme,
                            activities = plan.activities.joinLines(),
                            giftIdeas = plan.giftIdeas.joinLines(),
                            preparationNotes = (
                                listOf(plan.explanation) +
                                    plan.decorations.map { "Décoration : $it" } +
                                    plan.accommodations.map { "Adaptation : $it" }
                                ).joinToString("\n"),
                            checklist = plan.checklist.joinLines(),
                        )
                    }
                }
            } else {
                events.eventPlan(f.title, f.category, f.childIds.toList()).map { plan ->
                    { s: EventFormState ->
                        s.copy(
                            activities = plan.activities.joinLines(),
                            preparationNotes = (
                                listOf(plan.groupOrganization) +
                                    plan.sequence.map { "Étape : $it" } +
                                    plan.materials.map { "Matériel : $it" } +
                                    plan.accommodations.map { "Adaptation : $it" }
                                ).joinToString("\n"),
                            checklist = plan.checklist.joinLines(),
                        )
                    }
                }
            }
            result
                .onSuccess { apply ->
                    _form.update {
                        apply(it).copy(aiNote = "Suggestions chargées dans le formulaire. Relisez et modifiez avant d’enregistrer.")
                    }
                }
                .onFailure { e -> _form.update { it.copy(error = e.message ?: "Suggestions indisponibles.") } }
            _form.update { it.copy(suggesting = false) }
        }
    }

    fun submit(onSaved: (String) -> Unit) {
        val f = _form.value
        if (f.submitting) return
        val titleError = if (f.title.isBlank()) "Titre requis." else null
        val dateError = if (f.date == null || f.time == null) "Date et heure requises." else null
        val locationError = if (f.location.isBlank()) "Lieu requis." else null
        val error = if (f.type == EventType.BIRTHDAY && f.birthdayChildId !in f.childIds) "L’enfant fêté doit participer." else null
        _form.update { it.copy(titleError = titleError, dateError = dateError, locationError = locationError, error = error) }
        if (titleError != null || dateError != null || locationError != null || error != null || f.date == null || f.time == null) return

        val body = EventRequest(
            type = f.type,
            title = f.title.trim(),
            description = f.description.ifBlank { null },
            category = f.category.ifBlank { null },
            eventDate = ZonedDateTime.of(f.date, f.time, ZoneId.systemDefault()).toOffsetDateTime().toString(),
            location = f.location.trim(),
            birthdayChildId = f.birthdayChildId.takeIf { f.type == EventType.BIRTHDAY },
            theme = f.theme.ifBlank { null },
            plannedActivities = f.activities.items(),
            suggestedGiftIdeas = f.giftIdeas.items(),
            preparationNotes = f.preparationNotes.ifBlank { null },
            childIds = f.childIds.toList(),
            checklist = f.checklist.items(),
        )
        _form.update { it.copy(submitting = true) }
        viewModelScope.launch {
            events.save(eventId, body)
                .onSuccess(onSaved)
                .onFailure { e -> _form.update { it.copy(error = e.message ?: "Enregistrement impossible.") } }
            _form.update { it.copy(submitting = false) }
        }
    }
}

private fun List<String>.joinLines() = joinToString("\n")

private fun String.items() = split('\n').map(String::trim).filter(String::isNotEmpty)

private fun Event.toForm(): EventFormState {
    val local = eventDate.atZone(ZoneId.systemDefault())
    return EventFormState(
        type = type,
        title = title,
        description = description.orEmpty(),
        category = category.orEmpty(),
        date = local.toLocalDate(),
        time = local.toLocalTime().withSecond(0).withNano(0),
        location = location,
        birthdayChildId = birthdayChildId.orEmpty(),
        theme = theme.orEmpty(),
        activities = plannedActivities.joinLines(),
        giftIdeas = suggestedGiftIdeas.joinLines(),
        checklist = tasks.map { it.title }.joinLines(),
        preparationNotes = preparationNotes.orEmpty(),
        childIds = participants.map { it.childId }.toSet(),
    )
}
