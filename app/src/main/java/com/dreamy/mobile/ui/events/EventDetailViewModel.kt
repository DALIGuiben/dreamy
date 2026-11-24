package com.dreamy.mobile.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dreamy.mobile.data.remote.FeedbackRequest
import com.dreamy.mobile.data.repository.EventRepository
import com.dreamy.mobile.domain.model.ApprovalStatus
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventStatus
import com.dreamy.mobile.navigation.EventDetailRoute
import com.dreamy.mobile.ui.components.Load
import com.dreamy.mobile.ui.components.toLoad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackDraft(
    val enjoyed: Boolean = true,
    val comfortable: Boolean = true,
    val favoriteActivity: String = "",
    val comment: String = "",
)

/** Form state of `components/event-actions.tsx`. */
data class EventActionsState(
    val busy: Boolean = false,
    val error: String? = null,
    val taskTitle: String = "",
    /** Mother's optional message per participant id. */
    val comments: Map<String, String> = emptyMap(),
    /** Feedback form per child id. */
    val feedback: Map<String, FeedbackDraft> = emptyMap(),
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventRepository,
) : ViewModel() {
    private val eventId = savedStateHandle.toRoute<EventDetailRoute>().id

    private val _event = MutableStateFlow<Load<Event>>(Load.Loading)
    val event: StateFlow<Load<Event>> = _event.asStateFlow()

    private val _actions = MutableStateFlow(EventActionsState())
    val actions: StateFlow<EventActionsState> = _actions.asStateFlow()

    /** Called on resume; a previous action's error no longer applies to the reloaded event. */
    fun refresh() {
        if (_event.value is Load.Failed) _event.value = Load.Loading
        _actions.update { it.copy(error = null) }
        viewModelScope.launch {
            val result = repository.event(eventId)
            if (result.isSuccess || _event.value !is Load.Ready) _event.value = result.toLoad()
        }
    }

    // --- Organiser actions ---

    fun changeStatus(status: EventStatus) = run { repository.changeStatus(eventId, status) }

    fun deleteDraft(onDeleted: () -> Unit) = run(reload = false) {
        repository.delete(eventId).onSuccess { onDeleted() }
    }

    fun setTaskCompleted(taskId: String, completed: Boolean) =
        run { repository.setTaskCompleted(eventId, taskId, completed) }

    fun deleteTask(taskId: String) = run { repository.deleteTask(eventId, taskId) }

    fun onTaskTitle(value: String) = _actions.update { it.copy(taskTitle = value) }

    fun addTask() {
        val title = _actions.value.taskTitle.trim()
        if (title.isEmpty()) return
        run { repository.addTask(eventId, title).onSuccess { _actions.update { it.copy(taskTitle = "") } } }
    }

    // --- Mother actions ---

    fun onComment(participantId: String, value: String) =
        _actions.update { it.copy(comments = it.comments + (participantId to value)) }

    fun review(participantId: String, status: ApprovalStatus) {
        val comment = _actions.value.comments[participantId]?.ifBlank { null }
        run { repository.review(eventId, participantId, status, comment) }
    }

    fun feedbackDraft(childId: String): FeedbackDraft = _actions.value.feedback[childId]
        ?: (_event.value as? Load.Ready)?.data?.feedback?.firstOrNull { it.childId == childId }?.let {
            FeedbackDraft(it.enjoyed, it.comfortable, it.favoriteActivity.orEmpty(), it.comment.orEmpty())
        }
        ?: FeedbackDraft()

    fun onFeedback(childId: String, transform: (FeedbackDraft) -> FeedbackDraft) =
        _actions.update { it.copy(feedback = it.feedback + (childId to transform(feedbackDraft(childId)))) }

    fun saveFeedback(childId: String) {
        val draft = feedbackDraft(childId)
        run {
            repository.feedback(
                eventId,
                FeedbackRequest(
                    childId = childId,
                    enjoyed = draft.enjoyed,
                    comfortable = draft.comfortable,
                    favoriteActivity = draft.favoriteActivity.ifBlank { null },
                    comment = draft.comment.ifBlank { null },
                ),
            )
        }
    }

    /** Runs one mutation at a time, then reloads the event like `router.refresh()`. */
    private fun run(reload: Boolean = true, block: suspend () -> Result<*>) {
        if (_actions.value.busy) return
        _actions.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = block()
            if (reload && result.isSuccess) {
                repository.event(eventId).onSuccess { _event.value = Load.Ready(it) }
            }
            _actions.update {
                it.copy(busy = false, error = result.exceptionOrNull()?.let { e -> e.message ?: "Opération impossible." })
            }
        }
    }
}
