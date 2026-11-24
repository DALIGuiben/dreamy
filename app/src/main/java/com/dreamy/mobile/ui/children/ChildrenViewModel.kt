package com.dreamy.mobile.ui.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamy.mobile.data.repository.ChildRepository
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.ui.components.Load
import com.dreamy.mobile.ui.components.toLoad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildrenViewModel @Inject constructor(private val repository: ChildRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<List<Child>>>(Load.Loading)
    val state: StateFlow<Load<List<Child>>> = _state.asStateFlow()

    /** Admin search box text (`?q=` on the web page). */
    val query = MutableStateFlow("")
    private var appliedQuery = ""
    private var job: Job? = null

    fun search() {
        appliedQuery = query.value
        _state.value = Load.Loading
        refresh()
    }

    fun refresh() {
        if (_state.value is Load.Failed) _state.value = Load.Loading
        job?.cancel()
        job = viewModelScope.launch { _state.value = repository.children(appliedQuery).toLoad() }
    }
}
