package com.kitaab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.ui.graphics.vector.ImageVector

// Post is excluded here — it renders as a FAB, not a standard nav item
enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
) {
    Home(
        route = Route.Home.route,
        icon  = Icons.Outlined.Home,
        label = "Home",
    ),
    Explore(
        route = Route.Explore.route,
        icon  = Icons.Outlined.Explore,
        label = "Explore",
    ),
    Inbox(
        route = Route.Inbox.route,
        icon  = Icons.Outlined.Inbox,
        label = "Inbox",
    ),
    Profile(
        route = Route.Profile.route,
        icon  = Icons.Outlined.AccountCircle,
        label = "Profile",
    ),
}