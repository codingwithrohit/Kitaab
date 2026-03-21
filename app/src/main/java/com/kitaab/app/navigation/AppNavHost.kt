package com.kitaab.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kitaab.app.feature.auth.AuthViewModel
import com.kitaab.app.feature.auth.LoginScreen
import com.kitaab.app.feature.auth.OnboardingScreen
import com.kitaab.app.feature.auth.SignUpScreen
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
            // SplashScreen manages its own SplashViewModel via hiltViewModel() internally.
            // It only needs two navigation callbacks — no onSplashFinished needed.
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home.route) {
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
                onNavigateToSignUp = {
                    navController.navigate(Route.SignUp.route)
                },
            )
        }

        composable(Route.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.Home.route) {
            // Each composable() block in NavHost has its own Hilt ViewModel scope.
            // The ViewModel is scoped to this back-stack entry — correct behaviour.
            val viewModel: AuthViewModel = hiltViewModel()
            PlaceholderScreen(
                name = "Home",
                deleteAccount = { viewModel.deleteAccount() },
            )
        }

        composable(Route.Explore.route) { PlaceholderScreen("Explore") }
        composable(Route.Post.route) { PlaceholderScreen("Post") }
        composable(Route.Inbox.route) { PlaceholderScreen("Inbox") }
        composable(Route.Profile.route) { PlaceholderScreen("Profile") }
    }
}

@Composable
private fun PlaceholderScreen(
    name: String,
    deleteAccount: (() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (deleteAccount != null) {
            Button(onClick = deleteAccount) {
                Text("Delete Account")
            }
        }
    }
}