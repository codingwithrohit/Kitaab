package com.kitaab.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.kitaab.app.feature.explore.ExploreScreen
import com.kitaab.app.feature.home.HomeScreen
import com.kitaab.app.feature.listing.ListingDetailScreen
import com.kitaab.app.feature.post.PostScreen
import com.kitaab.app.feature.profile.ProfileSetupScreen
import com.kitaab.app.ui.theme.Teal500

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
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
                // After signup, always go to ProfileSetup — new users never
                // have profile_complete = true yet
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

        composable(Route.ListingDetail.route) {
            ListingDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { sellerId, listingId ->
                    navController.navigate(Route.Chat.createRoute("${sellerId}_${listingId}"))
                },
                onNavigateToDonationRequest = { listingId ->
                    // Phase 3 — donation request bottom sheet
                    // For now navigate back
                    navController.popBackStack()
                },
                onSellerClick = { userId ->
                    navController.navigate(Route.SellerProfile.createRoute(userId))
                },
                onSimilarListingClick = { listingId ->
                    navController.navigate(Route.ListingDetail.createRoute(listingId))
                },
            )
        }
        composable(Route.SellerProfile.route) {
            PlaceholderScreen("Seller Profile")
        }

        composable(Route.Chat.route) {
            PlaceholderScreen("Chat")
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
        composable(Route.Inbox.route) { PlaceholderScreen("Inbox") }
        composable(Route.Profile.route) { PlaceholderScreen("Profile") }
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