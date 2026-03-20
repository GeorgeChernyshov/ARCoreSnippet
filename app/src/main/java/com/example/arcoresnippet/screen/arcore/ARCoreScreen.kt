package com.example.arcoresnippet.screen.arcore

import android.net.Uri
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.arcoresnippet.R
import com.example.arcoresnippet.theme.ARCoreSnippetTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor.TerrainAnchorState
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.RecordingStatus
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.distanceTo
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.node.ViewNode
import io.github.sceneview.node.ViewNode2
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import java.io.File
import java.util.EnumSet
import kotlin.collections.remove
import kotlin.math.sqrt
import kotlin.random.Random

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
            recorderState = uiState.recorderState,
            fileUri = it,
            currentDestination = uiState.destination,
            isMapBottomSheetShown = uiState.mapsBottomSheetShown,
            showMapBottomSheet = viewModel::showMapBottomSheet,
            hideMapBottomSheet = viewModel::hideMapBottomSheet,
            setDestination = viewModel::setDestination
        )
    }
}

@Composable
fun ARCoreScreenContent(
    recorderState: ARCoreRecorderState,
    fileUri: Uri,
    currentDestination: LatLng?,
    isMapBottomSheetShown: Boolean,
    showMapBottomSheet: () -> Unit,
    hideMapBottomSheet: () -> Unit,
    setDestination: (LatLng) -> Unit
) {
    val context = LocalContext.current

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val nodes = rememberNodes()
    var lastLogTime by remember { mutableLongStateOf(0L) }
    var spherePlaced by remember { mutableStateOf(false) }
    val viewWindowManager = remember { ViewNode2.WindowManager(context) }
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope

    var markerAnchorNode by remember { mutableStateOf<AnchorNode?>(null) }
    var trackingStatus by remember { mutableStateOf("Not Tracking") }
    var localDistanceX by remember { mutableFloatStateOf(0f) }
    var localDistanceY by remember { mutableFloatStateOf(0f) }
    var localDistanceZ by remember { mutableFloatStateOf(0f) }
    var earthDistanceX by remember { mutableDoubleStateOf(0.0) }
    var earthDistanceY by remember { mutableDoubleStateOf(0.0) }
    var earthDistanceZ by remember { mutableDoubleStateOf(0.0) }
    var groundAltitude by remember { mutableDoubleStateOf(0.0) }
    var hAcc by remember { mutableDoubleStateOf(0.0) }
    val currentDestinationState by rememberUpdatedState(currentDestination)


    val sphereNode = remember {
        SphereNode(
            engine = engine,
            radius = 0.05f, // 5cm radius = 10cm diameter
            materialInstance = materialLoader.createColorInstance(Color.Blue)
        )
    }

    var earth by remember { mutableStateOf<Earth?>(null) }
    var sceneView by remember { mutableStateOf<ARSceneView?>(null) }
    var viewNode by remember { mutableStateOf<ViewNode?>(null) }

    LaunchedEffect(sceneView) {
        sceneView?.let {
            val node = ViewNode(
                engine = engine,
                modelLoader = modelLoader,
                viewAttachmentManager = ViewAttachmentManager(
                    context,
                    it
                ).apply { onResume() }
            ).apply {
                loadView(context, R.layout.view_location)
//                rotation = Rotation(x = 0f, y = 0f, z = 90f)
//                setScale(100f)
            }

            viewNode = node
        }
    }

    LaunchedEffect(currentDestination) {
        replaceMarker(
            sceneView = sceneView ?: return@LaunchedEffect,
            earth = earth ?: return@LaunchedEffect,
            destLat = currentDestination?.latitude ?: 0.0,
            destLng = currentDestination?.longitude ?: 0.0,
            markerViewNode = viewNode ?: return@LaunchedEffect,
            markerAnchorNode = markerAnchorNode,
            setMarkerAnchorNode = { markerAnchorNode = it },
            setGroundAltitude = { groundAltitude = it }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            nodes.forEach { node ->
                node.childNodes.forEach { it.destroy() }
            }

            nodes.clear()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                sceneView = ARSceneView(
                    context = context,
                    sharedEngine = engine,
                    sharedModelLoader = modelLoader,
                    sharedMaterialLoader = materialLoader,
                ).apply {
                    this.sessionConfiguration = { session, config ->
                        if (recorderState == ARCoreRecorderState.PLAYBACK) {
                            val file = File(fileUri.path ?: "")
                            if (file.exists() && file.length() > 0) {
                                try {
                                    session.setPlaybackDatasetUri(fileUri)
                                    config.geospatialMode = Config.GeospatialMode.DISABLED
                                    config.updateMode = Config.UpdateMode.BLOCKING

                                    Log.d("ARDebug", "Playback Configured: ${file.length()} bytes")
                                } catch (e: Exception) {
                                    Log.e("ARDebug", "Playback setup failed", e)
                                }
                            }
                        } else {
                            val filter = CameraConfigFilter(session).apply {
                                setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
                            }
                            session.getSupportedCameraConfigs(filter).firstOrNull()?.let {
                                session.cameraConfig = it
                            }

                            config.geospatialMode = Config.GeospatialMode.ENABLED
                            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                        }

                        config.focusMode = Config.FocusMode.FIXED
                        config.depthMode = Config.DepthMode.AUTOMATIC
                        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    }

                    this.onSessionUpdated = { session, frame ->
                        if (session.earth != earth)
                            earth = session.earth

//                        if (!spherePlaced && frame.camera.trackingState == TrackingState.TRACKING) {
//                            if (recorderState == ARCoreRecorderState.RECORDING &&
//                                session.recordingStatus == RecordingStatus.NONE
//                            ) {
////                            try {
////                                val recordingConfig = RecordingConfig(session)
////                                    .setMp4DatasetUri(fileUri)
////                                    .setAutoStopOnPause(true)
////                                session.startRecording(recordingConfig)
////                                Log.d("ARDebug", "Recording started to: $fileUri")
////                            } catch (e: Exception) {
////                                Log.e("ARDebug", "Failed to start recording", e)
////                            }
//                            }
//
//                            val cameraPose = frame.camera.pose
//                            val targetPose = cameraPose.compose(
//                                Pose.makeTranslation(0f, 0f, -2f)
//                            )
//                            val anchor = session.createAnchor(targetPose)
//                            val anchorNode = AnchorNode(
//                                engine = engine,
//                                anchor = anchor
//                            ).apply {
//                                addChildNode(sphereNode)
//                            }
//
//                            this.addChildNode(anchorNode)
//                            spherePlaced = true
//                        }
                        if (
                            currentDestinationState == null &&
                            earth != null &&
                            earth!!.trackingState == TrackingState.TRACKING &&
                            viewNode != null
                        ) {
                            val cameraGeo = earth!!.cameraGeospatialPose

                            setDestination(
                                LatLng(
                                    cameraGeo.latitude,
                                    cameraGeo.longitude
                                )
                            )
                        }

                        // Anchor successfully put so we can safely use the view node
                        if (markerAnchorNode != null)
                            viewNode?.lookAt(frame.camera.pose.position)

                        tryLog(
                            lastLogTime = lastLogTime,
                            frame = frame,
                            earth = earth,
                            nodes = nodes,
                            setLastLogTime = {
                                lastLogTime = it
                            }
                        )

                        markerAnchorNode?.let { anchor ->
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

                            val cameraGeo = earth?.cameraGeospatialPose
                            val destLat = currentDestinationState?.latitude ?: 0.0
                            val destLng = currentDestinationState?.longitude ?: 0.0

                            earthDistanceX = (destLat - (cameraGeo?.latitude ?: 0.0)) * 111111 // meters
                            earthDistanceY = (destLng - (cameraGeo?.longitude ?: 0.0)) * 111111 // meters
                            earthDistanceZ = (groundAltitude - (cameraGeo?.altitude ?: 0.0))
                        }

                        hAcc = earth?.cameraGeospatialPose?.horizontalAccuracy ?: 0.0
                    }
                }

                sceneView!!
            },
//        update = {
//            it.childNodes = nodes
//        }
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
            recorderState = ARCoreRecorderState.INITIAL,
            fileUri = Uri.EMPTY,
            isMapBottomSheetShown = false,
            currentDestination = LatLng(0.0, 0.0),
            showMapBottomSheet = {},
            hideMapBottomSheet = {},
            setDestination = {}
        )
    }
}

fun replaceMarker(
    sceneView: ARSceneView,
    earth: Earth,
    destLat: Double,
    destLng: Double,
    markerViewNode: ViewNode,
    markerAnchorNode: AnchorNode?,
    setMarkerAnchorNode: (AnchorNode) -> Unit,
    setGroundAltitude: (Double) -> Unit
) {
    markerAnchorNode?.let {
        sceneView.removeChildNode(it)
        it.destroy()
    }

    earth.resolveAnchorOnTerrainAsync(
        destLat,
        destLng,
        0.5,
        0f, 0f, 0f, 1f
    ) { earthAnchor, state ->
        if (state == TerrainAnchorState.SUCCESS) {
            val anchorNode = AnchorNode(
                sceneView.engine,
                earthAnchor
            ).apply {
                addChildNode(markerViewNode)
            }

            sceneView.addChildNode(anchorNode)
            setMarkerAnchorNode(anchorNode)
            val anchorGeoPose = earth.getGeospatialPose(earthAnchor.pose)
            setGroundAltitude(anchorGeoPose.altitude)
            Log.d("ARDebug", "Geospatial Marker Placed at $destLat, $destLng")
        } else {
            Log.e("ARDebug", "Terrain Anchor failed: $state")
        }
    }
}

fun tryLog(
    lastLogTime: Long,
    frame: Frame,
    earth: Earth?,
    nodes: List<Node>,
    setLastLogTime: (Long) -> Unit
) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastLogTime >= 3000) {
        val logContent = StringBuilder()
        logContent.appendLine("--- 3s Update ---")

        val earthState = earth?.earthState
        val trackingState = earth?.trackingState
        logContent.appendLine("Earth State: $earthState | Tracking State: $trackingState")
        if (earth != null && earth.trackingState == TrackingState.TRACKING) {
            val pose = earth.cameraGeospatialPose
            logContent.append("GPS: ${"%.6f".format(pose.latitude)}, ")
            logContent.append("${"%.6f".format(pose.longitude)} | ")
            logContent.append("Acc: ${"%.1fm".format(pose.horizontalAccuracy)}")
            logContent.appendLine()
        }

        nodes.filterIsInstance<AnchorNode>()
            .forEach { node ->
                logContent.appendLine("-- Anchor Node: ${node.position} --")
                val anchor = node.anchor

                if (anchor.trackingState == TrackingState.TRACKING) {
                    val cameraPose = frame.camera.pose
                    val anchorPose = anchor.pose

                    // Math: Transform Anchor Pose into Camera-Local space
                    val relativePose = cameraPose.inverse().compose(anchorPose)
                    val t = relativePose.translation
                    val distance = sqrt(t[0]*t[0] + t[1]*t[1] + t[2]*t[2])
                    logContent.appendLine("Distance: ${"%.2f".format(distance)}m")
                    logContent.append("Relative Pos: ")
                    logContent.append("x=${"%.2f".format(t[0])}, ")
                    logContent.append("y=${"%.2f".format(t[1])}, ")
                    logContent.append("z=${"%.2f".format(t[2])}")
                    logContent.appendLine()
                    logContent.appendLine("Camera Tracking: ${frame.camera.trackingState}")
                } else if (anchor.trackingState != TrackingState.TRACKING) {
                    logContent.appendLine("Anchor Lost Tracking: ${anchor.trackingState}")
                }

                logContent.appendLine("-- End Node --")
                logContent.appendLine()
            }

        Log.d("ARDebug", logContent.toString())
        setLastLogTime(currentTime)
    }
}