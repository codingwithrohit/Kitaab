package com.kitaab.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kitaab.app.feature.post.PostEntrySheet
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Controls the Post entry bottom sheet
    var showPostEntrySheet by remember { mutableStateOf(false) }

    // Snackbar for publish-complete toast
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val hideBottomNav =
        currentRoute == null ||
                currentRoute == Route.Splash.route ||
                currentRoute == Route.Onboarding.route ||
                currentRoute == Route.Login.route ||
                currentRoute == Route.SignUp.route ||
                currentRoute == Route.ProfileSetup.route ||
                currentRoute == Route.EditProfile.route ||
                currentRoute == Route.Post.route ||
                currentRoute?.startsWith("listing_detail/") == true ||
                currentRoute?.startsWith("seller_profile/") == true ||
                currentRoute?.startsWith("chat/") == true ||
                currentRoute?.startsWith("donation_requests/") == true ||
                currentRoute?.startsWith("edit_listing/") == true ||
                // Multi-post screens — all hide the bottom nav
                currentRoute == Route.MultiPostTray.route ||
                currentRoute == Route.MultiPostReview.route

    Scaffold(
        bottomBar = {
            if (!hideBottomNav) {
                KitaabBottomBar(
                    navController = navController,
                    onPostFabClick = { showPostEntrySheet = true },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(
                if (hideBottomNav) PaddingValues(0.dp) else innerPadding,
            ),
            onMultiPostComplete = { successCount, bookCount ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "$successCount listing${if (successCount == 1) "" else "s"} published · $bookCount book${if (bookCount == 1) "" else "s"}",
                    )
                }
            },
        )
    }

    // Post entry sheet — shown when FAB is tapped
    if (showPostEntrySheet) {
        PostEntrySheet(
            onDismiss = { showPostEntrySheet = false },
            onSingleBook = {
                showPostEntrySheet = false
                navController.navigate(Route.Post.route)
            },
            onMultipleBooks = {
                showPostEntrySheet = false
                navController.navigate(Route.MultiPost.route)
            },
        )
    }
}

@Composable
private fun KitaabBottomBar(
    navController: NavHostController,
    onPostFabClick: () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        BottomNavItem.entries.forEachIndexed { index, item ->

            // FAB slot sits between Explore (index 1) and Inbox (index 2)
            if (index == 2) {
                PostFabSlot(onClick = onPostFabClick)
            }

            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun PostFabSlot(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(64.dp),
    ) {
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Post a book",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}