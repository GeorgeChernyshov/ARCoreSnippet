package com.example.arcoresnippet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.arcoresnippet.navigation.Screen
import com.example.arcoresnippet.screen.arcore.ARCoreScreen
import com.example.arcoresnippet.screen.maps.MapsScreen
import com.example.arcoresnippet.screen.welcome.WelcomeScreen
import com.example.arcoresnippet.theme.ARCoreSnippetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { App() }
    }

    @Composable
    fun App() {
        val navController = rememberNavController()

        ARCoreSnippetTheme {
            NavHost(
                navController = navController,
                startDestination = Screen.Welcome.route
            ) {
                composable(Screen.ARCore.route) {
                    ARCoreScreen()
                }

                composable(Screen.Map.route) {
                    MapsScreen()
                }

                composable(Screen.Welcome.route) {
                    WelcomeScreen()
                }
            }
        }
    }
}