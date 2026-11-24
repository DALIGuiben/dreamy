package com.dreamy.mobile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dreamy.mobile.domain.model.Role
import com.dreamy.mobile.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Session(val token: String, val user: User)

/** Persists the access token and signed-in user (replaces the web session cookie). */
@Singleton
class SessionStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val session: Flow<Session?> = dataStore.data.map { prefs ->
        val token = prefs[TOKEN] ?: return@map null
        val role = prefs[ROLE]?.let { runCatching { Role.valueOf(it) }.getOrNull() } ?: return@map null
        Session(
            token = token,
            user = User(
                id = prefs[USER_ID].orEmpty(),
                name = prefs[NAME].orEmpty(),
                email = prefs[EMAIL].orEmpty(),
                role = role,
            ),
        )
    }

    suspend fun save(token: String, user: User) {
        dataStore.edit { prefs ->
            prefs[TOKEN] = token
            writeUser(prefs, user)
        }
    }

    suspend fun updateUser(user: User) {
        dataStore.edit { prefs -> if (prefs[TOKEN] != null) writeUser(prefs, user) }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun writeUser(prefs: androidx.datastore.preferences.core.MutablePreferences, user: User) {
        prefs[USER_ID] = user.id
        prefs[NAME] = user.name
        prefs[EMAIL] = user.email
        prefs[ROLE] = user.role.name
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val EMAIL = stringPreferencesKey("email")
        val ROLE = stringPreferencesKey("role")
    }
}
