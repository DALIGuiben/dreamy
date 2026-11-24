package com.dreamy.mobile.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dreamy.mobile.data.repository.AuthRepository
import com.dreamy.mobile.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val error: String? = null,
    val submitting: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, emailError = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value) }

    fun submit(onSuccess: (User) -> Unit) {
        val current = _state.value
        if (current.submitting) return
        val emailValid = Patterns.EMAIL_ADDRESS.matcher(current.email.trim()).matches()
        if (!emailValid || current.password.isEmpty()) {
            _state.update { it.copy(emailError = if (emailValid) null else "Adresse e-mail invalide.") }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            auth.login(current.email, current.password)
                .onSuccess(onSuccess)
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(submitting = false) }
        }
    }
}
