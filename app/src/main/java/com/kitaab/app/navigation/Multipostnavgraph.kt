package com.kitaab.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.kitaab.app.feature.post.multi.MultiPostEvent
import com.kitaab.app.feature.post.multi.MultiPostViewModel
import com.kitaab.app.feature.post.multi.OrganiseScreen
import com.kitaab.app.feature.post.multi.ReviewPublishScreen
import com.kitaab.app.feature.post.multi.SessionDefaultsScreen
import com.kitaab.app.feature.post.multi.StagingTrayScreen

/**
 * Nested navigation graph for the multi-book posting session.
 *
 * [MultiPostViewModel] is scoped to this graph — it survives navigation between
 * SessionDefaults → Tray → Organise → Review and is cleared only when the user
 * exits the graph entirely (publish success, discard, or back past SessionDefaults).
 *
 * Call this from AppNavHost inside the root NavHost block.
 */
fun NavGraphBuilder.multiPostNavGraph(
    navController: NavHostController,
    onSessionComplete: (successCount: Int, bookCount: Int) -> Unit,
) {
    navigation(
        startDestination = Route.MultiPostSessionDefaults.route,
        route = Route.MultiPost.route,
    ) {
        composable(Route.MultiPostSessionDefaults.route) { backStackEntry ->
            // hiltViewModel with the graph's back-stack entry scopes the VM to the graph
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Route.MultiPost.route)
            }
            val viewModel: MultiPostViewModel = hiltViewModel(parentEntry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            // Consume events
            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MultiPostEvent.NavigateToTray ->
                            navController.navigate(Route.MultiPostTray.route) {
                                // Prevent going back to defaults once tray is open
                                popUpTo(Route.MultiPostSessionDefaults.route) { inclusive = true }
                            }

                        is MultiPostEvent.SessionAbandoned ->
                            navController.popBackStack(Route.MultiPost.route, inclusive = true)

                        else -> Unit
                    }
                }
            }

            // Show resume banner if an incomplete session exists
            if (state.showResumeBanner) {
                ResumeBannerDialog(
                    bookCount = state.totalBookCount,
                    onResume = { viewModel.onResumeBannerAccepted() },
                    onDiscard = { viewModel.onResumeBannerDismissed() },
                )
            }

            SessionDefaultsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack(Route.MultiPost.route, inclusive = true)
                },
            )
        }

        composable(Route.MultiPostTray.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Route.MultiPost.route)
            }
            val viewModel: MultiPostViewModel = hiltViewModel(parentEntry)

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MultiPostEvent.NavigateToOrganise ->
                            navController.navigate(Route.MultiPostOrganise.route)

                        is MultiPostEvent.SessionAbandoned ->
                            navController.popBackStack(Route.MultiPost.route, inclusive = true)

                        else -> Unit
                    }
                }
            }

            StagingTrayScreen(
                viewModel = viewModel,
                onNavigateToOrganise = {
                    navController.navigate(Route.MultiPostOrganise.route)
                },
                onDiscardSession = {
                    navController.popBackStack(Route.MultiPost.route, inclusive = true)
                },
            )
        }

        composable(Route.MultiPostOrganise.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Route.MultiPost.route)
            }
            val viewModel: MultiPostViewModel = hiltViewModel(parentEntry)

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MultiPostEvent.NavigateToReview ->
                            navController.navigate(Route.MultiPostReview.route)

                        else -> Unit
                    }
                }
            }

            OrganiseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToReview = {
                    navController.navigate(Route.MultiPostReview.route)
                },
            )
        }

        composable(Route.MultiPostReview.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Route.MultiPost.route)
            }
            val viewModel: MultiPostViewModel = hiltViewModel(parentEntry)

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MultiPostEvent.PublishComplete -> {
                            navController.popBackStack(Route.MultiPost.route, inclusive = true)
                            onSessionComplete(event.successCount, event.totalBookCount)
                        }

                        is MultiPostEvent.PublishPartialFailure -> {
                            // Stay on review screen — UI shows retry button
                        }

                        else -> Unit
                    }
                }
            }

            ReviewPublishScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

// ── Resume banner dialog ──────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun ResumeBannerDialog(
    bookCount: Int,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDiscard,
        title = { androidx.compose.material3.Text("Continue your session?") },
        text = {
            androidx.compose.material3.Text(
                "You had $bookCount book${if (bookCount == 1) "" else "s"} ready from a previous session.",
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onResume) {
                androidx.compose.material3.Text("Continue")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDiscard) {
                androidx.compose.material3.Text("Start fresh")
            }
        },
    )
}