package com.dreamy.mobile.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Failure carrying the user-facing (French) message returned by the backend. */
class ApiException(message: String) : Exception(message)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Runs a network call and maps every failure to an [ApiException] with a displayable message. */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val message = e.response()?.errorBody()?.string()
        ?.let { runCatching { errorJson.decodeFromString<ErrorResponse>(it).error }.getOrNull() }
    Result.failure(ApiException(message ?: "Une erreur est survenue. Réessayez."))
} catch (e: IOException) {
    Result.failure(ApiException("Connexion indisponible. Vérifiez le serveur et réessayez."))
} catch (e: SerializationException) {
    Result.failure(ApiException("Réponse du serveur invalide."))
}
