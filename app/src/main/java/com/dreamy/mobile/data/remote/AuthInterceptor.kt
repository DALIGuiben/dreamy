package com.dreamy.mobile.data.remote

import com.dreamy.mobile.data.local.SessionStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

enum class SessionEvent { Expired, Forbidden }

/** App-wide signal used to leave protected screens when the backend rejects the session. */
@Singleton
class SessionEvents @Inject constructor() {
    private val events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val flow: SharedFlow<SessionEvent> = events.asSharedFlow()
    fun emit(event: SessionEvent) = events.tryEmit(event)
}

/** Adds the bearer token and reports 401/403 answers on authenticated calls. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
    private val sessionEvents: SessionEvents,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp runs interceptors on its own worker threads, so blocking here is safe.
        val token = runBlocking { sessionStore.session.first()?.token }
        val request = chain.request().newBuilder().apply {
            if (token != null) header("Authorization", "Bearer $token")
        }.build()
        val response = chain.proceed(request)
        if (token != null) {
            when (response.code) {
                401 -> sessionEvents.emit(SessionEvent.Expired)
                403 -> sessionEvents.emit(SessionEvent.Forbidden)
            }
        }
        return response
    }
}
