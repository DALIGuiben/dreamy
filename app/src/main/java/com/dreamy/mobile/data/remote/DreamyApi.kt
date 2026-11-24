package com.dreamy.mobile.data.remote

import com.dreamy.mobile.domain.model.BirthdayPlan
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventPlan
import com.dreamy.mobile.domain.model.OrganizedSuggestions
import com.dreamy.mobile.domain.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface DreamyApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @GET("api/me")
    suspend fun me(): User

    // Children and private notes

    @GET("api/children")
    suspend fun children(@Query("q") query: String? = null): List<Child>

    @GET("api/children/{id}")
    suspend fun child(@Path("id") id: String): Child

    @POST("api/children")
    suspend fun createChild(@Body body: ChildRequest): Child

    @PUT("api/children/{id}")
    suspend fun updateChild(@Path("id") id: String, @Body body: ChildRequest): Child

    @DELETE("api/children/{id}")
    suspend fun deleteChild(@Path("id") id: String)

    @POST("api/children/{id}/notes")
    suspend fun createNote(@Path("id") childId: String, @Body body: NoteRequest)

    @PUT("api/children/{id}/notes/{noteId}")
    suspend fun updateNote(@Path("id") childId: String, @Path("noteId") noteId: String, @Body body: NoteRequest)

    @DELETE("api/children/{id}/notes/{noteId}")
    suspend fun deleteNote(@Path("id") childId: String, @Path("noteId") noteId: String)

    // Events

    @GET("api/events")
    suspend fun events(): List<Event>

    @GET("api/events/{id}")
    suspend fun event(@Path("id") id: String): Event

    @POST("api/events")
    suspend fun createEvent(@Body body: EventRequest): Event

    @PUT("api/events/{id}")
    suspend fun updateEvent(@Path("id") id: String, @Body body: EventRequest): Event

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String)

    @POST("api/events/{id}/status")
    suspend fun changeStatus(@Path("id") id: String, @Body body: StatusRequest)

    @POST("api/events/{id}/tasks")
    suspend fun addTask(@Path("id") id: String, @Body body: TaskRequest)

    @PATCH("api/events/{id}/tasks/{taskId}")
    suspend fun updateTask(@Path("id") id: String, @Path("taskId") taskId: String, @Body body: TaskPatch)

    @DELETE("api/events/{id}/tasks/{taskId}")
    suspend fun deleteTask(@Path("id") id: String, @Path("taskId") taskId: String)

    @PATCH("api/events/{id}/participants/{participantId}")
    suspend fun review(
        @Path("id") id: String,
        @Path("participantId") participantId: String,
        @Body body: ApprovalRequest,
    )

    @POST("api/events/{id}/feedback")
    suspend fun feedback(@Path("id") id: String, @Body body: FeedbackRequest)

    // AI

    @POST("api/ai/organize-note")
    suspend fun organizeNote(@Body body: OrganizeNoteRequest): OrganizedSuggestions

    @POST("api/ai/approve-note")
    suspend fun approveNote(@Body body: ApproveNoteRequest)

    @POST("api/ai/birthday-plan")
    suspend fun birthdayPlan(@Body body: BirthdayPlanRequest): BirthdayPlan

    @POST("api/ai/event-plan")
    suspend fun eventPlan(@Body body: EventPlanRequest): EventPlan

    @Multipart
    @POST("api/transcription")
    suspend fun transcribe(
        @Part("childId") childId: RequestBody,
        @Part("duration") duration: RequestBody,
        @Part("language") language: RequestBody,
        @Part file: MultipartBody.Part,
    ): TranscriptionResponse
}
