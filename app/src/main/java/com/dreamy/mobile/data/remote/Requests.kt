package com.dreamy.mobile.data.remote

import com.dreamy.mobile.domain.model.ApprovalStatus
import com.dreamy.mobile.domain.model.EventStatus
import com.dreamy.mobile.domain.model.EventType
import com.dreamy.mobile.domain.model.NoteInputMethod
import com.dreamy.mobile.domain.model.OrganizedSuggestions
import com.dreamy.mobile.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: User)

@Serializable
data class ChildRequest(
    val firstName: String,
    /** ISO date `yyyy-MM-dd`. */
    val birthDate: String,
    val avatar: String?,
    val description: String?,
    val personalityTags: List<String>,
    val interests: List<String>,
    val favoriteActivities: List<String>,
    val favoriteColors: List<String>,
    val birthdayThemes: List<String>,
    val giftWishes: List<String>,
    val comfortPreferences: List<String>,
    val approvedPlanningSummary: String?,
)

@Serializable
data class NoteRequest(val content: String, val inputMethod: NoteInputMethod)

@Serializable
data class EventRequest(
    val type: EventType,
    val title: String,
    val description: String?,
    val category: String?,
    /** ISO-8601 with offset. */
    val eventDate: String,
    val location: String,
    val birthdayChildId: String?,
    val theme: String?,
    val plannedActivities: List<String>,
    val suggestedGiftIdeas: List<String>,
    val preparationNotes: String?,
    val childIds: List<String>,
    val checklist: List<String>,
)

@Serializable
data class StatusRequest(val status: EventStatus)

@Serializable
data class TaskRequest(val title: String)

@Serializable
data class TaskPatch(val completed: Boolean)

@Serializable
data class ApprovalRequest(val status: ApprovalStatus, val comment: String?)

@Serializable
data class FeedbackRequest(
    val childId: String,
    val enjoyed: Boolean,
    val comfortable: Boolean,
    val favoriteActivity: String?,
    val comment: String?,
)

@Serializable
data class OrganizeNoteRequest(val childId: String, val content: String)

@Serializable
data class ApproveNoteRequest(val childId: String, val suggestions: OrganizedSuggestions)

@Serializable
data class BirthdayPlanRequest(val childId: String)

@Serializable
data class EventPlanRequest(val title: String, val category: String, val childIds: List<String>)

@Serializable
data class TranscriptionResponse(val text: String)

@Serializable
data class ErrorResponse(val error: String)
