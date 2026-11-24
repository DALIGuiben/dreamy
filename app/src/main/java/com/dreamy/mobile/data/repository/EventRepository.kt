package com.dreamy.mobile.data.repository

import com.dreamy.mobile.data.remote.ApprovalRequest
import com.dreamy.mobile.data.remote.BirthdayPlanRequest
import com.dreamy.mobile.data.remote.DreamyApi
import com.dreamy.mobile.data.remote.EventPlanRequest
import com.dreamy.mobile.data.remote.EventRequest
import com.dreamy.mobile.data.remote.FeedbackRequest
import com.dreamy.mobile.data.remote.StatusRequest
import com.dreamy.mobile.data.remote.TaskPatch
import com.dreamy.mobile.data.remote.TaskRequest
import com.dreamy.mobile.data.remote.apiCall
import com.dreamy.mobile.domain.model.ApprovalStatus
import com.dreamy.mobile.domain.model.BirthdayPlan
import com.dreamy.mobile.domain.model.Event
import com.dreamy.mobile.domain.model.EventPlan
import com.dreamy.mobile.domain.model.EventStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(private val api: DreamyApi) {

    /** Admins get every event; mothers only non-draft events involving their children. */
    suspend fun events(): Result<List<Event>> = apiCall { api.events() }

    suspend fun event(id: String): Result<Event> = apiCall { api.event(id) }

    /** Creates the event when [id] is null; returns the saved event's id. */
    suspend fun save(id: String?, body: EventRequest): Result<String> = apiCall {
        if (id == null) api.createEvent(body).id else api.updateEvent(id, body).id
    }

    suspend fun delete(id: String): Result<Unit> = apiCall { api.deleteEvent(id) }

    suspend fun changeStatus(id: String, status: EventStatus): Result<Unit> =
        apiCall { api.changeStatus(id, StatusRequest(status)) }

    suspend fun addTask(id: String, title: String): Result<Unit> = apiCall { api.addTask(id, TaskRequest(title)) }

    suspend fun setTaskCompleted(id: String, taskId: String, completed: Boolean): Result<Unit> =
        apiCall { api.updateTask(id, taskId, TaskPatch(completed)) }

    suspend fun deleteTask(id: String, taskId: String): Result<Unit> = apiCall { api.deleteTask(id, taskId) }

    suspend fun review(id: String, participantId: String, status: ApprovalStatus, comment: String?): Result<Unit> =
        apiCall { api.review(id, participantId, ApprovalRequest(status, comment)) }

    suspend fun feedback(id: String, body: FeedbackRequest): Result<Unit> = apiCall { api.feedback(id, body) }

    suspend fun birthdayPlan(childId: String): Result<BirthdayPlan> =
        apiCall { api.birthdayPlan(BirthdayPlanRequest(childId)) }

    suspend fun eventPlan(title: String, category: String, childIds: List<String>): Result<EventPlan> =
        apiCall { api.eventPlan(EventPlanRequest(title, category, childIds)) }
}
