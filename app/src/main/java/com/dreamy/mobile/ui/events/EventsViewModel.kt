package com.dreamy.mobile.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamy.mobile.data.repository.EventRepository
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.ui.components.Load
import com.dreamy.mobile.ui.components.toLoad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(private val repository: EventRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<List<Event>>>(Load.Loading)
    val state: StateFlow<Load<List<Event>>> = _state.asStateFlow()

    fun refresh() {
        if (_state.value !is Load.Ready) _state.value = Load.Loading
        viewModelScope.launch { _state.value = repository.events().toLoad() }
    }
}
