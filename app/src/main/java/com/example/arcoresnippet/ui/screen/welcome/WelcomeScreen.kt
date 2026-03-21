package com.example.arcoresnippet.ui.screen.welcome

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.arcoresnippet.ui.AppState
import com.example.arcoresnippet.ui.AppViewModel
import com.example.arcoresnippet.R
import com.example.arcoresnippet.ui.theme.ARCoreSnippetTheme
import com.google.ar.core.ArCoreApk

@Composable
fun WelcomeScreen(onNextClick: () -> Unit) {
    val context = LocalContext.current
    val viewModel: AppViewModel = hiltViewModel()
    val appState by viewModel.appState.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::setCameraPermissionGranted
    )

    val coarseLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::setCoarseLocationPermissionGranted
    )

    val fineLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = viewModel::setFineLocationPermissionGranted
    )

    LaunchedEffect(Unit) {
        viewModel.checkPermissions()
        val availability = ArCoreApk.getInstance()
            .checkAvailability(context)

        viewModel.setARSupported(availability.isSupported)
    }

    WelcomeScreenContent(
        appState = appState,
        grantCameraPermission = {
            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        },
        grantCoarseLocationPermission = {
            coarseLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        },
        grantFineLocationPermission = {
            fineLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        },
        onNextClick = onNextClick
    )
}

@Composable
fun WelcomeScreenContent(
    appState: AppState,
    grantCameraPermission: () -> Unit,
    grantCoarseLocationPermission: () -> Unit,
    grantFineLocationPermission: () -> Unit,
    onNextClick: () -> Unit
) {
    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(stringResource(R.string.welcome_text))
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.welcome_ar_support_title))

                    Text(
                        text = stringResource(
                            if (appState.arSupported)
                                R.string.welcome_ar_support_supported
                            else R.string.welcome_ar_support_not_supported
                        )
                    )
                }
            }

            item {
                PermissionBlock(
                    title = stringResource(R.string.welcome_permission_camera),
                    isGranted = appState.cameraPermissionGranted,
                    grantPermission = grantCameraPermission
                )
            }

            item {
                PermissionBlock(
                    title = stringResource(R.string.welcome_permission_coarse_location),
                    isGranted = appState.coarseLocationPermissionGranted,
                    grantPermission = grantCoarseLocationPermission
                )
            }

            item {
                PermissionBlock(
                    title = stringResource(R.string.welcome_permission_fine_location),
                    isGranted = appState.fineLocationPermissionGranted,
                    grantPermission = grantFineLocationPermission
                )
            }

            if (appState.allPermissionsGranted) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.welcome_good_to_go))

                        Button(onClick = onNextClick) {
                            Text(stringResource(R.string.welcome_button_next))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBlock(
    title: String,
    isGranted: Boolean,
    grantPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title)

        if (isGranted) {
            Text(stringResource(R.string.welcome_permission_granted))
        } else {
            Button(grantPermission) {
                Text(stringResource(R.string.welcome_button_grant_permission))
            }
        }
    }
}

@Composable
@Preview
fun PermissionBlockPreview() {
    ARCoreSnippetTheme {
        PermissionBlock(
            title = stringResource(R.string.welcome_permission_camera),
            isGranted = true,
            grantPermission = {}
        )
    }
}

@Composable
@Preview
fun WelcomeScreenContentPreview() {
    ARCoreSnippetTheme {
        WelcomeScreenContent(
            appState = AppState(),
            grantCameraPermission = {},
            grantCoarseLocationPermission = {},
            grantFineLocationPermission = {},
            onNextClick = {}
        )
    }
}