package com.example.arcoresnippet.screen.arcore

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.arcoresnippet.R
import com.google.ar.core.Anchor
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.RecordingConfig
import com.google.ar.core.RecordingStatus
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.node.ViewNode
import io.github.sceneview.node.ViewNode2
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlinx.coroutines.launch
import java.io.File
import java.util.EnumSet
import kotlin.math.log
import kotlin.math.sqrt

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
            fileUri = it
        )
    }
}

@Composable
fun ARCoreScreenContent(
    recorderState: ARCoreRecorderState,
    fileUri: Uri,
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

    val sphereNode = remember {
        SphereNode(
            engine = engine,
            radius = 0.05f, // 5cm radius = 10cm diameter
            materialInstance = materialLoader.createColorInstance(Color.Blue)
        )
    }

    var setMarkerPlaced by remember { mutableStateOf(false) }

    var sceneView by remember { mutableStateOf<ARSceneView?>(null) }
    var viewNode by remember { mutableStateOf<ViewNode?>(null) }

    LaunchedEffect(sceneView) {
        sceneView?.let {
            viewNode = ViewNode(
                engine = engine,
                modelLoader = modelLoader,
                viewAttachmentManager = ViewAttachmentManager(
                    context,
                    it
                ).apply { onResume() }
            ).apply {
                loadView(context, R.layout.view_location)
                rotation = Rotation(x = 0f, y = 0f, z = 90f)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            nodes.forEach { node ->
                node.childNodes.forEach { it.destroy() }
            }

            nodes.clear()
        }
    }

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
                    val earth = session.earth
                    if (!spherePlaced && frame.camera.trackingState == TrackingState.TRACKING) {
                        if (recorderState == ARCoreRecorderState.RECORDING &&
                            session.recordingStatus == RecordingStatus.NONE
                        ) {
                            try {
                                val recordingConfig = RecordingConfig(session)
                                    .setMp4DatasetUri(fileUri)
                                    .setAutoStopOnPause(true)
                                session.startRecording(recordingConfig)
                                Log.d("ARDebug", "Recording started to: $fileUri")
                            } catch (e: Exception) {
                                Log.e("ARDebug", "Failed to start recording", e)
                            }
                        }

                        val cameraPose = frame.camera.pose
                        val targetPose = cameraPose.compose(
                            Pose.makeTranslation(0f, 0f, -0.2f)
                        )
                        val anchor = session.createAnchor(targetPose)
                        val anchorNode = AnchorNode(
                            engine = engine,
                            anchor = anchor
                        ).apply {
                            addChildNode(sphereNode)
                        }

                        this.addChildNode(anchorNode)
                        spherePlaced = true
                    }

                    if (!setMarkerPlaced && earth != null && earth.trackingState == TrackingState.TRACKING) {
                        val altitude = earth.cameraGeospatialPose.altitude - 1.5

                        val destLat = 40.2060241
                        val destLng = 44.4997323

                        val earthAnchor = earth.createAnchor(
                            destLat,
                            destLng,
                            altitude,
                            0f, 0f, 0f, 1f
                        )

                        // 4. Wrap it in an AnchorNode and add to scene
                        val anchorNode = AnchorNode(engine, earthAnchor).apply {
                            addChildNode(viewNode!!)
                        }

                        nodes.add(anchorNode)
                        setMarkerPlaced = true
                        Log.d("ARDebug", "Geospatial Marker Placed at $destLat, $destLng")
                    }

                    tryLog(
                        lastLogTime = lastLogTime,
                        frame = frame,
                        earth = earth,
                        nodes = nodes,
                        setLastLogTime = {
                            lastLogTime = it
                        }
                    )
                }
            }

            sceneView!!
        },
//        update = {
//            it.childNodes = nodes
//        }
    )
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