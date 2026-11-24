@file:UseSerializers(InstantSerializer::class, LocalDateSerializer::class)

package com.dreamy.mobile.domain.model

import com.dreamy.mobile.util.InstantSerializer
import com.dreamy.mobile.util.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.time.LocalDate

@Serializable
enum class Role { MOTHER, ADMIN }

@Serializable
enum class NoteInputMethod { TEXT, VOICE }

@Serializable
enum class EventType { BIRTHDAY, SOS_EVENT }

@Serializable
enum class EventStatus { DRAFT, AWAITING_APPROVAL, CHANGES_REQUESTED, CONFIRMED, COMPLETED, CANCELLED }

@Serializable
enum class ApprovalStatus { PENDING, APPROVED, DECLINED, CHANGES_REQUESTED }

@Serializable
data class User(val id: String, val name: String, val email: String, val role: Role)

@Serializable
data class Child(
    val id: String,
    val motherId: String,
    val motherName: String,
    val firstName: String,
    val birthDate: LocalDate,
    val avatar: String? = null,
    val description: String? = null,
    val personalityTags: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val favoriteActivities: List<String> = emptyList(),
    val favoriteColors: List<String> = emptyList(),
    val birthdayThemes: List<String> = emptyList(),
    val giftWishes: List<String> = emptyList(),
    val comfortPreferences: List<String> = emptyList(),
    val approvedPlanningSummary: String? = null,
    /** Only returned to the child's own mother. */
    val notes: List<ChildNote> = emptyList(),
)

@Serializable
data class ChildNote(
    val id: String,
    val content: String,
    val inputMethod: NoteInputMethod,
    val createdAt: Instant,
)

@Serializable
data class Event(
    val id: String,
    val type: EventType,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val eventDate: Instant,
    val location: String,
    val birthdayChildId: String? = null,
    val theme: String? = null,
    val plannedActivities: List<String> = emptyList(),
    val suggestedGiftIdeas: List<String> = emptyList(),
    val preparationNotes: String? = null,
    val status: EventStatus,
    val participants: List<Participant> = emptyList(),
    val tasks: List<EventTask> = emptyList(),
    val feedback: List<Feedback> = emptyList(),
) {
    val isLocked: Boolean get() = status == EventStatus.COMPLETED || status == EventStatus.CANCELLED
}

@Serializable
data class Participant(
    val id: String,
    val childId: String,
    val childFirstName: String,
    val approvalStatus: ApprovalStatus,
    val motherComment: String? = null,
)

@Serializable
data class EventTask(val id: String, val title: String, val completed: Boolean)

@Serializable
data class Feedback(
    val id: String,
    val childId: String,
    val enjoyed: Boolean,
    val comfortable: Boolean,
    val favoriteActivity: String? = null,
    val comment: String? = null,
)

@Serializable
data class OrganizedSuggestions(
    val interests: List<String>,
    val comfortPreferences: List<String>,
    val giftWishes: List<String>,
    val birthdayThemes: List<String>,
)

@Serializable
data class BirthdayPlan(
    val theme: String,
    val activities: List<String>,
    val giftIdeas: List<String>,
    val decorations: List<String>,
    val accommodations: List<String>,
    val checklist: List<String>,
    val explanation: String,
)

@Serializable
data class EventPlan(
    val activities: List<String>,
    val materials: List<String>,
    val sequence: List<String>,
    val groupOrganization: String,
    val accommodations: List<String>,
    val checklist: List<String>,
)
