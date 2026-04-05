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
import com.kitaab.app.feature.post.multi.ReviewPublishScreen
import com.kitaab.app.feature.post.multi.StagingTrayScreen

fun NavGraphBuilder.multiPostNavGraph(
    navController: NavHostController,
    onSessionComplete: (successCount: Int, bookCount: Int) -> Unit,
) {
    navigation(
        startDestination = Route.MultiPostTray.route,
        route = Route.MultiPost.route,
    ) {
        composable(Route.MultiPostTray.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                runCatching {
                    navController.getBackStackEntry(Route.MultiPost.route)
                }.getOrNull()
            } ?: return@composable

            val viewModel: MultiPostViewModel = hiltViewModel(parentEntry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is MultiPostEvent.NavigateToReview ->
                            navController.navigate(Route.MultiPostReview.route)

                        is MultiPostEvent.SessionAbandoned ->
                            navController.popBackStack(Route.MultiPost.route, inclusive = true)

                        else -> Unit
                    }
                }
            }

            LaunchedEffect(state.isInitializing) {
                if (state.isInitializing) return@LaunchedEffect
                if (state.showResumeBanner) return@LaunchedEffect
                if (state.sessionId == null) {
                    viewModel.openSessionDefaultsSheet()
                }
            }

            if (state.showResumeBanner) {
                ResumeBannerDialog(
                    bookCount = state.totalBookCount,
                    onResume = { viewModel.onResumeBannerAccepted() },
                    onDiscard = { viewModel.onResumeBannerDismissed() },
                )
            }

            StagingTrayScreen(
                viewModel = viewModel,
                onNavigateToReview = {
                    navController.navigate(Route.MultiPostReview.route)
                },
                onDiscardSession = {
                    navController.popBackStack(Route.MultiPost.route, inclusive = true)
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

                        is MultiPostEvent.PublishPartialFailure -> Unit
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