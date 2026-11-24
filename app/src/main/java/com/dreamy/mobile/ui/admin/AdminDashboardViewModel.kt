package com.dreamy.mobile.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.data.repository.EventRepository
import com.dreamy.mobile.domain.model.Event
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

data class AdminDashboard(val childCount: Int, val upcomingEvents: List<Event>, val pending: Int)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val children: ChildRepository,
    private val events: EventRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<Load<AdminDashboard>>(Load.Loading)
    val state: StateFlow<Load<AdminDashboard>> = _state.asStateFlow()

    fun refresh() {
        if (_state.value !is Load.Ready) _state.value = Load.Loading
        viewModelScope.launch {
            val kids = async { children.children() }
            val list = async { events.events() }
            _state.value = kids.await().mapCatching { childList ->
                val eventList = list.await().getOrThrow()
                AdminDashboard(
                    childCount = childList.size,
                    upcomingEvents = eventList.upcoming().take(6),
                    pending = eventList.pendingApprovals(),
                )
            }.toLoad()
        }
    }
}
