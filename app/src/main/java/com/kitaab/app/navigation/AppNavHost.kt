package com.kitaab.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kitaab.app.feature.auth.AuthEvent
import com.kitaab.app.feature.auth.AuthViewModel
import com.kitaab.app.feature.auth.LoginScreen
import com.kitaab.app.feature.auth.OnboardingScreen
import com.kitaab.app.feature.auth.SignUpScreen
import com.kitaab.app.feature.auth.SplashScreen
import com.kitaab.app.feature.chat.ChatScreen
import com.kitaab.app.feature.donation.DonationRequestsScreen
import com.kitaab.app.feature.explore.ExploreScreen
import com.kitaab.app.feature.home.HomeScreen
import com.kitaab.app.feature.inbox.InboxScreen
import com.kitaab.app.feature.listing.ListingDetailScreen
import com.kitaab.app.feature.post.PostScreen
import com.kitaab.app.feature.profile.EditProfileScreen
import com.kitaab.app.feature.profile.ProfileScreen
import com.kitaab.app.feature.profile.ProfileSetupScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onMultiPostComplete: (successCount: Int, bookCount: Int) -> Unit = { _, _ -> },
) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash.route,
        modifier = modifier,
    ) {
        composable(Route.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(Route.ProfileSetup.route) {
                        popUpTo(Route.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Route.Onboarding.route) {
                        popUpTo(Route.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLoginNeedsProfile = {
                    navController.navigate(Route.ProfileSetup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Route.SignUp.route)
                },
            )
        }

        composable(Route.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Route.ProfileSetup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.ProfileSetup.route) {
            ProfileSetupScreen(
                onSetupComplete = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Home.route) {
            val authViewModel: AuthViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                authViewModel.events.collect { event ->
                    when (event) {
                        is AuthEvent.SignOutSuccess,
                        is AuthEvent.DeleteAccountSuccess,
                            -> {
                            navController.navigate(Route.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }

                        else -> Unit
                    }
                }
            }

            HomeScreen(
                onListingClick = { listingId ->
                    navController.navigate(Route.ListingDetail.createRoute(listingId))
                },
                onSearchClick = {
                    navController.navigate(Route.Explore.route)
                },
            )
        }

        composable(
            route = Route.ListingDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("listingId") {
                    type = androidx.navigation.NavType.StringType
                },
                androidx.navigation.navArgument("referrerId") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            ListingDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Route.Chat.createRoute(conversationId))
                },
                onNavigateToDonationRequest = { _ ->
                    // Handled inside ListingDetailScreen as a bottom sheet
                },
                onNavigateToDonationRequests = { listingId ->
                    navController.navigate(Route.DonationRequests.createRoute(listingId))
                },
                onSellerClick = { userId ->
                    navController.navigate(Route.SellerProfile.createRoute(userId))
                },
                onSimilarListingClick = { listingId ->
                    val currentListingId = backStackEntry.arguments?.getString("listingId")
                    navController.navigate(
                        Route.ListingDetail.createRoute(listingId, currentListingId),
                    ) {
                        popUpTo(Route.ListingDetail.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Route.SellerProfile.route) {
            PlaceholderScreen("Seller Profile")
        }

        composable(Route.Explore.route) {
            ExploreScreen(
                onNavigateBack = { navController.popBackStack() },
                onListingClick = { listingId ->
                    navController.navigate(Route.ListingDetail.createRoute(listingId))
                },
            )
        }

        composable(Route.Post.route) {
            PostScreen(
                onPostSuccess = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.Inbox.route) {
            InboxScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Route.Chat.createRoute(conversationId))
                },
            )
        }

        composable(Route.Chat.route) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Route.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate(Route.EditProfile.route)
                },
                onListingClick = { listingId ->
                    navController.navigate(Route.ListingDetail.createRoute(listingId))
                },
            )
        }

        composable(Route.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Route.DonationRequests.route) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: return@composable
            DonationRequestsScreen(
                listingId = listingId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // ── Multi-book posting nested graph ───────────────────────────────────
        multiPostNavGraph(
            navController = navController,
            onSessionComplete = onMultiPostComplete,
        )
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}