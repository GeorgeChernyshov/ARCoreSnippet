package com.example.arcoresnippet.ui.screen.arcore

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.arcoresnippet.ui.theme.ARCoreSnippetTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.TrackingState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun ARCoreScreen(recordingPath: String?) {
    val viewModel: ARCoreViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (recordingPath != null) {
            viewModel.openRecording(recordingPath)
        } else {
            viewModel.createNewRecording()
        }
    }

    uiState.fileUri?.let {
        ARCoreScreenContent(
            currentDestination = uiState.destination,
            currentPath = uiState.path,
            isMapBottomSheetShown = uiState.mapsBottomSheetShown,
            showMapBottomSheet = viewModel::showMapBottomSheet,
            hideMapBottomSheet = viewModel::hideMapBottomSheet,
            setDestination = viewModel::setDestination,
            fetchPath = viewModel::fetchRoadPath
        )
    }
}

@Composable
fun ARCoreScreenContent(
    currentDestination: LatLng?,
    currentPath: List<LatLng>?,
    isMapBottomSheetShown: Boolean,
    showMapBottomSheet: () -> Unit,
    hideMapBottomSheet: () -> Unit,
    setDestination: (LatLng) -> Unit,
    fetchPath: (LatLng, LatLng) -> Unit
) {
    var trackingStatus by remember { mutableStateOf("Not Tracking") }
    var localDistanceX by remember { mutableFloatStateOf(0f) }
    var localDistanceY by remember { mutableFloatStateOf(0f) }
    var localDistanceZ by remember { mutableFloatStateOf(0f) }
    var earthDistanceX by remember { mutableDoubleStateOf(0.0) }
    var earthDistanceY by remember { mutableDoubleStateOf(0.0) }
    var earthDistanceZ by remember { mutableDoubleStateOf(0.0) }
    var hAcc by remember { mutableDoubleStateOf(0.0) }

    Box(Modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            currentDestination = currentDestination,
            currentPath = currentPath,
            setDestination = setDestination,
            onSourceLocationChanged = { source ->
                if (currentDestination != null)
                    fetchPath(source, currentDestination)
            },
            collectStats = {
                anchor,
                frame,
                cameraGeo,
                destLat,
                destLng,
                altitude ->

                trackingStatus = if (anchor.trackingState == TrackingState.TRACKING)
                    "Tracking"
                else "Lost Tracking"

                val cameraPose = frame.camera.pose
                val anchorPose = anchor.pose
                val inverseCameraPose = cameraPose.inverse()
                val relativePose = inverseCameraPose.compose(anchorPose)
                val t = relativePose.translation

                localDistanceX = t[1]
                localDistanceY = t[0]
                localDistanceZ = t[2]

                earthDistanceX = (destLat - cameraGeo.latitude) * 111111 // meters
                earthDistanceY = (destLng - cameraGeo.longitude) * 111111 // meters
                earthDistanceZ = altitude - cameraGeo.altitude
            },
            setHorizontalAccuracy = { hAcc = it }
        )

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Text("HORIZONTAL ACCURACY")
            Text(hAcc.toString())
            Spacer(Modifier.height(16.dp))
            Text("TRACKING")
            Text(trackingStatus)
            Spacer(Modifier.height(16.dp))
            Text("LOCAL")
            Text(localDistanceX.toString())
            Text(localDistanceY.toString())
            Text(localDistanceZ.toString())
            Spacer(Modifier.height(16.dp))
            Text("EARTH")
            Text(earthDistanceX.toString())
            Text(earthDistanceY.toString())
            Text(earthDistanceZ.toString())
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = showMapBottomSheet,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(32.dp)
                .align(Alignment.BottomCenter)
        ) {
            Text("Map")
        }

        AnimatedVisibility(
            visible = isMapBottomSheetShown,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column {
                    // Grab handle
                    Box(
                        Modifier
                            .padding(8.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = Color.Gray,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .align(Alignment.CenterHorizontally)
                            .clickable { hideMapBottomSheet() }
                    )

                    // The actual Map
                    SimpleMapView(
                        currentDestination = currentDestination,
                        onLocationSelected = setDestination
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleMapView(
    currentDestination: LatLng?,
    onLocationSelected: (LatLng) -> Unit
) {
    // Starting position (Yerevan)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentDestination ?: LatLng(0.0, 0.0),
            15f
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false, // Set false to avoid location conflicts with ARCore
            mapType = MapType.NORMAL
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false
        ),
        onMapClick = onLocationSelected
    ) {
        currentDestination?.let {
            Marker(
                state = MarkerState(position = it),
                title = "Destination",
                snippet = "Marker in AR"
            )
        }
    }
}

@Composable
@Preview
fun SimpleMapViewPreview() {
    ARCoreSnippetTheme {
        SimpleMapView(
            currentDestination = LatLng(0.0, 0.0),
            onLocationSelected = {}
        )
    }
}

@Composable
@Preview
fun ARCoreScreenContentPreview() {
    ARCoreSnippetTheme {
        ARCoreScreenContent(
            isMapBottomSheetShown = false,
            currentDestination = LatLng(0.0, 0.0),
            currentPath = null,
            showMapBottomSheet = {},
            hideMapBottomSheet = {},
            setDestination = {},
            fetchPath = { _, _ -> }
        )
    }
}