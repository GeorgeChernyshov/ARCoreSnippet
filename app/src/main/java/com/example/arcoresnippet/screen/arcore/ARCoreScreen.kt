package com.example.arcoresnippet.screen.arcore

import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import io.github.sceneview.ar.ARScene

@Composable
fun ARCoreScreen() {
    val activity = LocalActivity.current
    var session by remember { mutableStateOf<Session?>(null) }
    var userRequestedInstall by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            if (session == null) {
                when (ArCoreApk.getInstance()
                    .requestInstall(activity, userRequestedInstall)
                ) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        session = Session(activity!!)
                    }

                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        userRequestedInstall = true
                        return@LaunchedEffect
                    }
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Log.e("ARCoreScreen", "User declined installation", e)
            return@LaunchedEffect
        }
    }

    ARScene(
        modifier = Modifier.fillMaxSize(),
    )
}