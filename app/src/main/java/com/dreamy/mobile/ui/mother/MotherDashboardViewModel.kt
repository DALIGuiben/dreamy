package com.dreamy.mobile.ui.mother

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.data.repository.EventRepository
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.byNextBirthday
import com.dreamy.mobile.domain.model.pendingApprovals
import com.dreamy.mobile.domain.model.upcoming
import com.dreamy.mobile.ui.components.Load
import com.dreamy.mobile.ui.components.toLoad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MotherDashboard(
    val children: List<Child>,
    val birthdays: List<Child>,
    val upcomingEvents: List<Event>,
    val pending: Int,
)

@HiltViewModel
class MotherDashboardViewModel @Inject constructor(
    private val children: ChildRepository,
    private val events: EventRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Load<MotherDashboard>>(Load.Loading)
    val state: StateFlow<Load<MotherDashboard>> = _state.asStateFlow()

    /** Called on every resume so the page reflects changes made on other screens. */
    fun refresh() {
        if (_state.value !is Load.Ready) _state.value = Load.Loading
        viewModelScope.launch {
            val kids = async { children.children() }
            val list = async { events.events() }
            _state.value = kids.await().mapCatching { childList ->
                // The backend already hides drafts and other mothers' events.
                val eventList = list.await().getOrThrow()
                MotherDashboard(
                    children = childList,
                    birthdays = childList.byNextBirthday().take(3),
                    upcomingEvents = eventList.upcoming().take(4),
                    pending = eventList.pendingApprovals(),
                )
            }.toLoad()
        }
    }
}
