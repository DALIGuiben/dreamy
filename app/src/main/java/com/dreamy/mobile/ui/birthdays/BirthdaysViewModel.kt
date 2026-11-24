package com.dreamy.mobile.ui.birthdays

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.data.repository.EventRepository
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventType
import com.dreamy.mobile.domain.model.byNextBirthday
import com.dreamy.mobile.navigation.BirthdaysRoute
import com.dreamy.mobile.ui.components.Load
import com.dreamy.mobile.ui.components.toLoad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Birthdays(val upcoming: List<Child>, val projects: List<Event>)

@HiltViewModel
class BirthdaysViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val children: ChildRepository,
    private val events: EventRepository,
) : ViewModel() {
    private val admin = savedStateHandle.toRoute<BirthdaysRoute>().admin
    private val _state = MutableStateFlow<Load<Birthdays>>(Load.Loading)
    val state: StateFlow<Load<Birthdays>> = _state.asStateFlow()

    fun refresh() {
        if (_state.value !is Load.Ready) _state.value = Load.Loading
        viewModelScope.launch {
            val kids = async { children.children() }
            val list = async { events.events() }
            _state.value = kids.await().mapCatching { childList ->
                val sorted = childList.byNextBirthday()
                Birthdays(
                    // The organiser page lists the next eight; a mother sees all of hers.
                    upcoming = if (admin) sorted.take(8) else sorted,
                    projects = list.await().getOrThrow().filter { it.type == EventType.BIRTHDAY },
                )
            }.toLoad()
        }
    }
}
