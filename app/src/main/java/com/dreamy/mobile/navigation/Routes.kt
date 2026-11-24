package com.dreamy.mobile.navigation

import kotlinx.serialization.Serializable

// One destination per Next.js route. `admin` selects the /admin/* variant of shared screens.

@Serializable data object HomeRoute // /

@Serializable data object LoginRoute // /login

@Serializable data object UnauthorizedRoute // /unauthorized

@Serializable data object PresentationRoute // /presentation

@Serializable data object MotherDashboardRoute // /mother/dashboard

@Serializable data object AdminDashboardRoute // /admin/dashboard

@Serializable data class ChildrenRoute(val admin: Boolean) // /{role}/children

@Serializable data class ChildDetailRoute(val id: String, val admin: Boolean) // /{role}/children/[id]

@Serializable data class ChildFormRoute(val id: String? = null) // /mother/children/new, /mother/children/[id]/edit

@Serializable data class BirthdaysRoute(val admin: Boolean) // /{role}/birthdays

@Serializable data class EventsRoute(val admin: Boolean) // /{role}/events

@Serializable data class EventDetailRoute(val id: String, val admin: Boolean) // /{role}/events/[id]

@Serializable data class EventFormRoute(val id: String? = null, val birthday: Boolean = false) // /admin/events/new, /admin/events/[id]/edit

/** Bottom navigation tabs (the web sidebar). */
enum class Tab(val label: String) {
    Dashboard("Tableau de bord"),
    Children("Enfants"),
    Birthdays("Anniversaires"),
    Events("Événements"),
    ;

    fun route(admin: Boolean): Any = when (this) {
        Dashboard -> if (admin) AdminDashboardRoute else MotherDashboardRoute
        Children -> ChildrenRoute(admin)
        Birthdays -> BirthdaysRoute(admin)
        Events -> EventsRoute(admin)
    }
}
