package com.dreamy.mobile.ui.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamy.mobile.navigation.Tab
import com.dreamy.mobile.ui.components.BrandLogo
import com.dreamy.mobile.ui.theme.Coral
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.ui.theme.Pale
import com.dreamy.mobile.ui.theme.White

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.Dashboard -> Icons.Outlined.Dashboard
        Tab.Children -> Icons.Outlined.People
        Tab.Birthdays -> Icons.Outlined.FavoriteBorder
        Tab.Events -> Icons.Outlined.CalendarMonth
    }

/**
 * Mobile version of `components/shell.tsx`: brand bar with the account name and logout,
 * and the four sections as a bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    admin: Boolean,
    userName: String,
    selectedTab: Tab,
    onTab: (Tab) -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)?,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = Pale,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = White),
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Navy)
                            }
                        }
                    },
                    title = {
                        Column {
                            BrandLogo(Modifier.width(110.dp))
                            Text(
                                if (admin) "ESPACE ORGANISATION" else "ESPACE MAMAN",
                                color = Coral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                        }
                    },
                    actions = {
                        Text(
                            userName,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(96.dp),
                        )
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Déconnexion", tint = Navy)
                        }
                    },
                )
                HorizontalDivider(color = Light)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = White) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Cyan,
                            selectedTextColor = Navy,
                            unselectedIconColor = Cyan,
                            unselectedTextColor = Navy,
                            indicatorColor = Pale,
                        ),
                    )
                }
            }
        },
    ) { padding -> content(Modifier.padding(padding)) }
}
