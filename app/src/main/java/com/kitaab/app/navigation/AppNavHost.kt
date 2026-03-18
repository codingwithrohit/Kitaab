package com.kitaab.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kitaab.app.feature.auth.LoginScreen
import com.kitaab.app.feature.auth.OnboardingScreen
import com.kitaab.app.feature.auth.SplashScreen

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
                onSplashFinished = {
                    navController.navigate(Route.Onboarding.route) {
                        popUpTo(Route.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    // signup route — next feature branch
                }
            )
        }
        composable(Route.Home.route)    { PlaceholderScreen("Home") }
        composable(Route.Explore.route) { PlaceholderScreen("Explore") }
        composable(Route.Post.route)    { PlaceholderScreen("Post") }
        composable(Route.Inbox.route)   { PlaceholderScreen("Inbox") }
        composable(Route.Profile.route) { PlaceholderScreen("Profile") }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier.fillMaxSize(),
    ) {
        Text(
            text  = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}