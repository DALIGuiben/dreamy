package com.dreamy.mobile.data.repository

import com.dreamy.mobile.data.local.Session
import com.dreamy.mobile.data.local.SessionStore
import com.dreamy.mobile.data.remote.DreamyApi
import com.dreamy.mobile.data.remote.LoginRequest
import com.dreamy.mobile.data.remote.apiCall
import com.dreamy.mobile.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: DreamyApi,
    private val sessionStore: SessionStore,
) {
    val session: Flow<Session?> = sessionStore.session

    suspend fun currentSession(): Session? = session.first()

    suspend fun login(email: String, password: String): Result<User> =
        apiCall { api.login(LoginRequest(email.trim().lowercase(), password)) }
            .onSuccess { sessionStore.save(it.token, it.user) }
            .map { it.user }

    /** Re-reads the profile so a renamed or re-roled account is picked up at start-up. */
    suspend fun refreshUser(): Result<User> = apiCall { api.me() }.onSuccess { sessionStore.updateUser(it) }

    suspend fun logout() = sessionStore.clear()
}
