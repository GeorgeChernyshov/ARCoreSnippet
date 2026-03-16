package com.example.arcoresnippet.screen.arcore

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Pose
import com.google.ar.core.RecordingConfig
import com.google.ar.core.RecordingStatus
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import java.io.File
import java.util.EnumSet
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
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val nodes = rememberNodes()
    var lastLogTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            nodes.forEach { node ->
                node.childNodes.forEach { it.destroy() }
            }

            nodes.clear()
        }
    }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        sessionConfiguration = { session, config ->
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
            config.depthMode = Config.DepthMode.DISABLED
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        },
        childNodes = nodes,
        onSessionUpdated = { session, frame ->
            val earth = session.earth
            if (nodes.isEmpty() && frame.camera.trackingState == TrackingState.TRACKING) {
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

                val sphere = SphereNode(
                    engine = engine,
                    radius = 0.05f, // 5cm radius = 10cm diameter
                    materialInstance = materialLoader.createColorInstance(Color.Blue)
                )

                val cameraPose = frame.camera.pose
                val targetPose = cameraPose.compose(
                    Pose.makeTranslation(0f, 0f, -0.2f)
                )
                val anchor = session.createAnchor(targetPose)

                val anchorNode = AnchorNode(
                    engine = engine,
                    anchor = anchor
                ).apply {
                    addChildNode(sphere)
                }

                nodes.add(anchorNode)
            }

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastLogTime >= 3000) { // 3000ms = 3 seconds
                nodes.firstOrNull()?.let { node ->
                    val anchorNode = node as? AnchorNode
                    val anchor = anchorNode?.anchor

                    if (anchor != null && anchor.trackingState == TrackingState.TRACKING) {
                        val logContent = StringBuilder()
                        logContent.appendLine("--- 3s Update ---")
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

                        Log.d("ARDebug", logContent.toString())

                        lastLogTime = currentTime
                    } else if (anchor?.trackingState != TrackingState.TRACKING) {
                        Log.w("ARDebug", "Anchor Lost Tracking: ${anchor?.trackingState}")
                        lastLogTime = currentTime
                    }
                }
            }
        }
    )
}