package com.dreamy.mobile.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dreamy.mobile.MainViewModel
import com.dreamy.mobile.dashboardFor
import com.dreamy.mobile.domain.model.Role
import com.dreamy.mobile.domain.model.User
import com.dreamy.mobile.ui.admin.AdminDashboardScreen
import com.dreamy.mobile.ui.auth.LoginScreen
import com.dreamy.mobile.ui.auth.UnauthorizedScreen
import com.dreamy.mobile.ui.birthdays.BirthdaysScreen
import com.dreamy.mobile.ui.children.ChildDetailScreen
import com.dreamy.mobile.ui.children.ChildFormScreen
import com.dreamy.mobile.ui.children.ChildrenScreen
import com.dreamy.mobile.ui.events.EventDetailScreen
import com.dreamy.mobile.ui.events.EventFormScreen
import com.dreamy.mobile.ui.events.EventsScreen
import com.dreamy.mobile.ui.home.HomeScreen
import com.dreamy.mobile.ui.mother.MotherDashboardScreen
import com.dreamy.mobile.ui.presentation.PresentationScreen
import com.dreamy.mobile.ui.shell.AppShell
import com.dreamy.mobile.ui.theme.Pale

@Composable
fun DreamyNavHost(main: MainViewModel = hiltViewModel()) {
    val start by main.startRoute.collectAsStateWithLifecycle()
    val user by main.user.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val startRoute = start
    if (startRoute == null) {
        Box(Modifier.fillMaxSize().background(Pale))
        return
    }
    LaunchedEffect(nav) { main.navigation.collect { nav.resetTo(it.route) } }
    val shell = ShellContext(nav, { user }, main::logout)

    NavHost(nav, startDestination = startRoute) {
        composable<HomeRoute> {
            HomeScreen(onLogin = { nav.navigate(LoginRoute) }, onPresentation = { nav.navigate(PresentationRoute) })
        }
        composable<LoginRoute> {
            LoginScreen(onHome = { nav.resetTo(HomeRoute) }, onLoggedIn = { nav.resetTo(dashboardFor(it)) })
        }
        composable<UnauthorizedRoute> { UnauthorizedScreen(onBackToLogin = main::logout) }
        composable<PresentationRoute> {
            PresentationScreen(onHome = { if (!nav.popBackStack()) nav.resetTo(HomeRoute) })
        }

        shellScreen<MotherDashboardRoute>(shell, Tab.Dashboard, admin = { false }, tabRoot = true) { modifier, _ ->
            MotherDashboardScreen(
                modifier = modifier,
                userName = user?.name.orEmpty(),
                onChild = { nav.navigate(ChildDetailRoute(it, admin = false)) },
                onChildren = { nav.openTab(ChildrenRoute(admin = false), admin = false) },
                onAddChild = { nav.navigate(ChildFormRoute()) },
                onBirthdays = { nav.openTab(BirthdaysRoute(admin = false), admin = false) },
                onEvent = { nav.navigate(EventDetailRoute(it, admin = false)) },
            )
        }
        shellScreen<AdminDashboardRoute>(shell, Tab.Dashboard, admin = { true }, tabRoot = true) { modifier, _ ->
            AdminDashboardScreen(
                modifier = modifier,
                onEvent = { nav.navigate(EventDetailRoute(it, admin = true)) },
                onCreateEvent = { nav.navigate(EventFormRoute()) },
            )
        }
        shellScreen<ChildrenRoute>(shell, Tab.Children, admin = { it.admin }, tabRoot = true) { modifier, route ->
            ChildrenScreen(
                modifier = modifier,
                admin = route.admin,
                onChild = { nav.navigate(ChildDetailRoute(it, route.admin)) },
                onAddChild = { nav.navigate(ChildFormRoute()) },
            )
        }
        shellScreen<ChildDetailRoute>(shell, Tab.Children, admin = { it.admin }) { modifier, route ->
            ChildDetailScreen(
                modifier = modifier,
                admin = route.admin,
                onEdit = { nav.navigate(ChildFormRoute(it)) },
                onDeleted = { nav.popBackStack() },
            )
        }
        shellScreen<ChildFormRoute>(shell, Tab.Children, admin = { false }) { modifier, route ->
            ChildFormScreen(
                modifier = modifier,
                onSaved = { id ->
                    if (route.id == null) {
                        nav.navigate(ChildDetailRoute(id, admin = false)) { popUpTo<ChildFormRoute> { inclusive = true } }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
        shellScreen<BirthdaysRoute>(shell, Tab.Birthdays, admin = { it.admin }, tabRoot = true) { modifier, route ->
            BirthdaysScreen(
                modifier = modifier,
                admin = route.admin,
                onEvent = { nav.navigate(EventDetailRoute(it, route.admin)) },
                onPrepareBirthday = { nav.navigate(EventFormRoute(birthday = true)) },
            )
        }
        shellScreen<EventsRoute>(shell, Tab.Events, admin = { it.admin }, tabRoot = true) { modifier, route ->
            EventsScreen(
                modifier = modifier,
                admin = route.admin,
                onEvent = { nav.navigate(EventDetailRoute(it, route.admin)) },
                onCreate = { nav.navigate(EventFormRoute()) },
            )
        }
        shellScreen<EventDetailRoute>(shell, Tab.Events, admin = { it.admin }) { modifier, route ->
            EventDetailScreen(
                modifier = modifier,
                admin = route.admin,
                onEdit = { nav.navigate(EventFormRoute(it)) },
                onDeleted = { nav.popBackStack() },
            )
        }
        shellScreen<EventFormRoute>(shell, Tab.Events, admin = { true }) { modifier, route ->
            EventFormScreen(
                modifier = modifier,
                onSaved = { id ->
                    if (route.id == null) {
                        nav.navigate(EventDetailRoute(id, admin = true)) { popUpTo<EventFormRoute> { inclusive = true } }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
    }
}

private class ShellContext(
    val nav: NavHostController,
    val user: () -> User?,
    val logout: () -> Unit,
)

/** Destination wrapped in the role shell; also enforces the role like the web layouts' `pageUser(role)`. */
private inline fun <reified T : Any> NavGraphBuilder.shellScreen(
    shell: ShellContext,
    tab: Tab,
    noinline admin: (T) -> Boolean,
    tabRoot: Boolean = false,
    noinline content: @Composable (Modifier, T) -> Unit,
) {
    composable<T> { entry ->
        val route = entry.toRoute<T>()
        val isAdmin = admin(route)
        val user = shell.user()
        if (user != null && (user.role == Role.ADMIN) != isAdmin) {
            LaunchedEffect(Unit) { shell.nav.resetTo(UnauthorizedRoute) }
            return@composable
        }
        AppShell(
            admin = isAdmin,
            userName = user?.name.orEmpty(),
            selectedTab = tab,
            onTab = { shell.nav.openTab(it.route(isAdmin), isAdmin) },
            onLogout = shell.logout,
            onBack = if (tabRoot) null else ({ shell.nav.popBackStack() }),
        ) { modifier -> content(modifier, route) }
    }
}

/** Clears the whole back stack, e.g. after login or logout. */
private fun NavHostController.resetTo(route: Any) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}

/** Switches section like the sidebar links, keeping one copy of each tab. */
private fun NavHostController.openTab(route: Any, admin: Boolean) = navigate(route) {
    popUpTo(Tab.Dashboard.route(admin)) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
