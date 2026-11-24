package com.dreamy.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamy.mobile.data.remote.SessionEvent
import com.dreamy.mobile.data.remote.SessionEvents
import com.dreamy.mobile.data.repository.AuthRepository
import com.dreamy.mobile.domain.model.Role
import com.dreamy.mobile.domain.model.User
import com.dreamy.mobile.navigation.AdminDashboardRoute
import com.dreamy.mobile.navigation.HomeRoute
import com.dreamy.mobile.navigation.LoginRoute
import com.dreamy.mobile.navigation.MotherDashboardRoute
import com.dreamy.mobile.navigation.UnauthorizedRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Root navigation request that clears the back stack. */
data class ResetTo(val route: Any)

fun dashboardFor(user: User): Any = if (user.role == Role.ADMIN) AdminDashboardRoute else MotherDashboardRoute

/** App-level session state: start destination, signed-in user, logout and session expiry. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val auth: AuthRepository,
    sessionEvents: SessionEvents,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<Any?>(null)
    /** Null while the stored session is being read. */
    val startRoute: StateFlow<Any?> = _startRoute.asStateFlow()

    val user: StateFlow<User?> = auth.session.map { it?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val resets = Channel<ResetTo>(Channel.BUFFERED)
    val navigation = resets.receiveAsFlow()

    init {
        viewModelScope.launch {
            val session = auth.currentSession()
            _startRoute.value = session?.let { dashboardFor(it.user) } ?: HomeRoute
            // Validates the stored token; a 401 is reported through SessionEvents.
            if (session != null) auth.refreshUser()
        }
        viewModelScope.launch {
            sessionEvents.flow.collect { event ->
                when (event) {
                    SessionEvent.Expired -> {
                        auth.logout()
                        resets.send(ResetTo(LoginRoute))
                    }
                    SessionEvent.Forbidden -> resets.send(ResetTo(UnauthorizedRoute))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
            resets.send(ResetTo(LoginRoute))
        }
    }
}
