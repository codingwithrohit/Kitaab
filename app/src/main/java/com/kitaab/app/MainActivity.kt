package com.kitaab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kitaab.app.navigation.MainScreen
import com.kitaab.app.ui.theme.KitaabTheme

class MainActivity : ComponentActivity() {
    private var keepSplash = true // controls system splash

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KitaabTheme {
                MainScreen(
                    onSplashReady = {
                        keepSplash = false
                    },
                )
            }
        }
    }
}
