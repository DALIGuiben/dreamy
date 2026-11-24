package com.dreamy.mobile.data.repository

import com.dreamy.mobile.data.remote.ApproveNoteRequest
import com.dreamy.mobile.data.remote.ChildRequest
import com.dreamy.mobile.data.remote.DreamyApi
import com.dreamy.mobile.data.remote.NoteRequest
import com.dreamy.mobile.data.remote.OrganizeNoteRequest
import com.dreamy.mobile.data.remote.apiCall
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.NoteInputMethod
import com.dreamy.mobile.domain.model.OrganizedSuggestions
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepository @Inject constructor(private val api: DreamyApi) {

    suspend fun children(query: String? = null): Result<List<Child>> =
        apiCall { api.children(query?.trim()?.takeIf { it.isNotEmpty() }) }

    suspend fun child(id: String): Result<Child> = apiCall { api.child(id) }

    /** Creates the child when [id] is null; returns the saved child's id. */
    suspend fun save(id: String?, body: ChildRequest): Result<String> = apiCall {
        if (id == null) api.createChild(body).id else api.updateChild(id, body).id
    }

    suspend fun delete(id: String): Result<Unit> = apiCall { api.deleteChild(id) }

    suspend fun saveNote(childId: String, noteId: String?, content: String, method: NoteInputMethod): Result<Unit> =
        apiCall {
            val body = NoteRequest(content, method)
            if (noteId == null) api.createNote(childId, body) else api.updateNote(childId, noteId, body)
        }

    suspend fun deleteNote(childId: String, noteId: String): Result<Unit> = apiCall { api.deleteNote(childId, noteId) }

    suspend fun organizeNote(childId: String, content: String): Result<OrganizedSuggestions> =
        apiCall { api.organizeNote(OrganizeNoteRequest(childId, content)) }

    suspend fun approveSuggestions(childId: String, suggestions: OrganizedSuggestions): Result<Unit> =
        apiCall { api.approveNote(ApproveNoteRequest(childId, suggestions)) }

    /** Sends an AAC/MP4 recording for transcription; returns the recognised text. */
    suspend fun transcribe(childId: String, audio: File, seconds: Int, language: String): Result<String> = apiCall {
        val text = "text/plain".toMediaType()
        api.transcribe(
            childId = childId.toRequestBody(text),
            duration = seconds.toString().toRequestBody(text),
            language = language.toRequestBody(text),
            file = MultipartBody.Part.createFormData("file", "note.m4a", audio.asRequestBody("audio/mp4".toMediaType())),
        ).text
    }
}
