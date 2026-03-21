package com.example.arcoresnippet.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.arcoresnippet.ui.navigation.Screen
import com.example.arcoresnippet.ui.screen.arcore.ARCoreScreen
import com.example.arcoresnippet.ui.screen.maps.MapsScreen
import com.example.arcoresnippet.ui.screen.source.SourceScreen
import com.example.arcoresnippet.ui.screen.welcome.WelcomeScreen
import com.example.arcoresnippet.ui.theme.ARCoreSnippetTheme
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
                startDestination = Screen.Welcome
            ) {
                composable<Screen.ARCore> { backStackEntry ->
                    val args = backStackEntry.toRoute<Screen.ARCore>()
                    ARCoreScreen(args.recordingPath)
                }

                composable<Screen.Map> {
                    MapsScreen()
                }

                composable<Screen.Source> {
                    SourceScreen(
                        onCameraClick = {
                            navController.navigate(Screen.ARCore(null))
                        },
                        onRecordingClick = { path ->
                            navController.navigate(Screen.ARCore(path))
                        }
                    )
                }

                composable<Screen.Welcome> {
                    WelcomeScreen(
                        onNextClick = {
                            navController.navigate(Screen.Source)
                        }
                    )
                }
            }
        }
    }
}