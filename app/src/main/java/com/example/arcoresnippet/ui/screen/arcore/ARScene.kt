package com.example.arcoresnippet.ui.screen.arcore

import android.media.Image
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.arcoresnippet.R
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Pose
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.TrackingState
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ViewNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberViewNodeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.EnumSet

@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    currentDestination: LatLng?,
    currentPath: List<LatLng>?,
    setDestination: (LatLng) -> Unit,
    onSourceLocationChanged: (LatLng) -> Unit,
    collectStats: (
        anchor: Anchor,
        frame: Frame,
        cameraGeo: GeospatialPose,
        destLan: Double,
        destLng: Double,
        altitude: Double
    ) -> Unit,
    setHorizontalAccuracy: (Double) -> Unit
) {
    val view = LocalView.current

    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val windowManager = rememberViewNodeManager()
        .apply { this.resume(LocalView.current) }

    val markerNode = remember {
        ViewNode(
            engine = engine,
            materialLoader = materialLoader,
            windowManager = windowManager
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location),
                contentDescription = null,
                modifier = Modifier.size(2000.dp),
                tint = Color.Cyan
            )
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var pathFindingEnabled by remember { mutableStateOf(false) }
    var visibleSegments by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }

    var sphereAnchor by remember { mutableStateOf<Anchor?>(null) }
    var markerAnchor by remember { mutableStateOf<Anchor?>(null) }
    var groundAltitude by remember { mutableDoubleStateOf(0.0) }
    var replaceMarker by remember { mutableStateOf(false) }
    var locationTrackerJob by remember { mutableStateOf<Job?>(null) }
    var sourceLocation by remember { mutableStateOf<Float3?>(null) }

    DisposableEffect(currentDestination) {
        replaceMarker = true
        if (pathFindingEnabled) {
            locationTrackerJob = coroutineScope.launch {
                while (true) {
                    sourceLocation?.let {
                        onSourceLocationChanged(
                            LatLng(it.x.toDouble(), it.y.toDouble())
                        )
                    }

                    delay(30000)
                }
            }
        }

        onDispose {
            locationTrackerJob?.cancel()
            locationTrackerJob = null
        }
    }

    Box(modifier) {
        ARSceneView(
            engine = engine,
            materialLoader = materialLoader,
            sessionConfiguration = { session, config ->
                val filter = CameraConfigFilter(session).apply {
                    setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
                }

                session.getSupportedCameraConfigs(filter)
                    .firstOrNull()
                    ?.let { session.cameraConfig = it }

                config.geospatialMode = Config.GeospatialMode.ENABLED
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                config.focusMode = Config.FocusMode.FIXED
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

                if (
                    session.isDepthModeSupported(Config.DepthMode.AUTOMATIC) &&
                    session.isSemanticModeSupported(Config.SemanticMode.ENABLED)
                ) {
                    pathFindingEnabled = true
                    config.depthMode = Config.DepthMode.AUTOMATIC
                    config.semanticMode = Config.SemanticMode.ENABLED
                } else {
                    pathFindingEnabled = false
                    config.depthMode = Config.DepthMode.DISABLED
                    config.semanticMode = Config.SemanticMode.DISABLED
                }
            },
            viewNodeWindowManager = windowManager,
            onSessionUpdated = { session, frame ->
                val earth = session.earth ?: return@ARSceneView

                if (sphereAnchor == null && frame.camera.trackingState == TrackingState.TRACKING) {
                    val cameraPose = frame.camera.pose
                    val targetPose = cameraPose.compose(
                        Pose.makeTranslation(0f, 0f, -0.2f)
                    )

                    sphereAnchor = session.createAnchor(targetPose)
                }

                if (replaceMarker) {
                    val destLat = currentDestination?.latitude ?: 0.0
                    val destLng = currentDestination?.longitude ?: 0.0

                    earth.resolveAnchorOnTerrainAsync(
                        destLat,
                        destLng,
                        0.2,
                        0f, 0f, 0f, 1f
                    ) { earthAnchor, state ->
                        if (state == Anchor.TerrainAnchorState.SUCCESS && earth.trackingState == TrackingState.TRACKING) {
                            markerAnchor = earthAnchor

                            try {
                                Log.d("ARDebug", "Getting ground altitude")
                                Log.d("ARDebug", "Earth tracking state is ${earth.trackingState.name}")
                                val anchorGeoPose = earth.getGeospatialPose(earthAnchor.pose)
                                groundAltitude = anchorGeoPose.altitude
                            } catch (e: Exception) {
                                Log.e("ARDebug", "Failed to get ground altitude: $e")
                            }

                            Log.d("ARDebug", "Geospatial Marker Placed at $destLat, $destLng")
                        } else {
                            Log.e("ARDebug", "Terrain Anchor failed: $state")
                        }
                    }

                    replaceMarker = false
                }

                sourceLocation = Float3(
                    earth.cameraGeospatialPose.latitude.toFloat(),
                    earth.cameraGeospatialPose.longitude.toFloat(),
                    earth.cameraGeospatialPose.altitude.toFloat()
                )

                if (earth.trackingState == TrackingState.TRACKING) {
                    if (currentDestination == null) {
                        val cameraGeo = earth.cameraGeospatialPose

                        setDestination(
                            LatLng(
                                cameraGeo.latitude,
                                cameraGeo.longitude
                            )
                        )
                    }
                }

                if (markerNode.parent != null)
                    markerNode.lookAt(frame.camera.pose.position)

                markerAnchor?.let { anchor ->
                    val cameraGeo = earth.cameraGeospatialPose
                    val destLat = currentDestination?.latitude ?: 0.0
                    val destLng = currentDestination?.longitude ?: 0.0

                    collectStats(
                        anchor,
                        frame,
                        cameraGeo,
                        destLat,
                        destLng,
                        groundAltitude
                    )

                    if (pathFindingEnabled) {
                        val semanticImage = frame.acquireSemanticImage()
                        val cameraPose = frame.camera.pose

                        val interpolatedLatLngs = mutableListOf<LatLng>()
                        currentPath?.let { path ->
                            for (i in 0 until path.size - 1) {
                                val start = path[i]
                                val end = path[i + 1]

                                // Add the original waypoint
                                interpolatedLatLngs.add(start)

                                // Add 8 intermediate points
                                for (j in 1..8) {
                                    val fraction = j.toDouble() / 9.0 // divide into 9 segments
                                    val lat = start.latitude + (end.latitude - start.latitude) * fraction
                                    val lng = start.longitude + (end.longitude - start.longitude) * fraction
                                    interpolatedLatLngs.add(LatLng(lat, lng))
                                }
                            }
                            // Add the final destination point
                            if (path.isNotEmpty()) interpolatedLatLngs.add(path.last())
                        }

                        val pathPoints = interpolatedLatLngs.map {
                            PathPoint(
                                latLng = it,
                                earth = earth,
                                frame = frame,
                                altitude = groundAltitude,
                                viewWidth = view.width.toFloat(),
                                viewHeight = view.height.toFloat()
                            ).apply { checkOcclusion(
                                semanticImage = semanticImage,
                                cameraPose = cameraPose
                            ) }
                        }

                        visibleSegments = assembleSegments(
                            startPoint = Offset(
                                view.width.toFloat() / 2,
                                view.height.toFloat()
                            ),
                            points = pathPoints
                        )
                    } else {
                        val endPoint = PathPoint(
                            pose = anchor.pose,
                            frame = frame,
                            viewWidth = view.width.toFloat(),
                            viewHeight = view.height.toFloat()
                        )

                        visibleSegments = listOf(
                            listOf(
                                Offset(
                                    view.width.toFloat() / 2,
                                    view.height.toFloat()
                                ),
                                endPoint.screenOffset
                            )
                        )
                    }
                }

                setHorizontalAccuracy(earth.cameraGeospatialPose.horizontalAccuracy)
            }
        ) {
            sphereAnchor?.let { anchor ->
                AnchorNode(anchor = anchor) {
                    SphereNode(
                        radius = 0.05f, // 5cm radius = 10cm diameter
                        materialInstance = materialLoader.createColorInstance(Color.Blue)
                    )
                }
            }

            markerAnchor?.let { anchor ->
                AnchorNode(
                    anchor = anchor,
                    apply = {
                        if (markerNode.parent != this)
                            addChildNode(markerNode)
                    }
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            visibleSegments.forEach { segment ->
                if (segment.size > 1) {
                    val path = Path().apply {
                        moveTo(segment[0].x, segment[0].y)
                        for (i in 1 until segment.size) {
                            lineTo(segment[i].x, segment[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Cyan.copy(alpha = 0.7f),
                        style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

/** Groups a sequence of points into drawable segments based on visibility */
private fun assembleSegments(
    startPoint: Offset,
    points: List<PathPoint>
): List<List<Offset>> {
    val segments = mutableListOf<MutableList<Offset>>()
    var currentSegment = mutableListOf(startPoint)

    points.forEach { pt ->
        if (pt.isVisible) {
            currentSegment.add(pt.screenOffset)
        } else if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment)
            currentSegment = mutableListOf()
        }
    }

    if (currentSegment.isNotEmpty())
        segments.add(currentSegment)

    return segments
}