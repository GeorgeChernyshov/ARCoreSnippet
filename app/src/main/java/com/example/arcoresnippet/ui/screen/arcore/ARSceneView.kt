package com.example.arcoresnippet.ui.screen.arcore

import android.content.Context
import android.media.Image
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.arcoresnippet.R
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Anchor.TerrainAnchorState
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Pose
import com.google.ar.core.StreetscapeGeometry
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.position
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.node.ViewNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.EnumSet
import kotlin.math.sqrt

class ARSceneView(context: Context) : ARSceneView(context) {

    var currentDestination: LatLng? = null
        set(value) {
            if (frame?.camera?.trackingState == TrackingState.TRACKING) {
                replaceMarker(
                    destLat = value?.latitude ?: 0.0,
                    destLng = value?.longitude ?: 0.0
                )
            } else {
                pendingDestination = value
            }

            field = value
        }

    var onDestinationChanged: ((LatLng) -> Unit)? = null
    var collectStats: ((
        anchor: AnchorNode,
        frame: Frame,
        cameraGeo: GeospatialPose,
        destLan: Double,
        destLng: Double,
        altitude: Double
    ) -> Unit)? = null

    var setHorizontalAccuracy: ((Double) -> Unit)? = null
    var onSourceLocationChanged: ((LatLng) -> Unit)? = null
    var afterSessionUpdated: (() -> Unit)? = null

    private var earth: Earth? = null
    private var markerAnchorNode: AnchorNode? = null
    private var groundAltitude = 0.0
    private var spherePlaced = false
    private var pendingDestination: LatLng? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var locationTrackerJob: Job? = null

    private val creationTime = System.currentTimeMillis()
    private val sphereNode = SphereNode(
        engine = engine,
        radius = 0.05f, // 5cm radius = 10cm diameter
        materialInstance = materialLoader.createColorInstance(Color.Blue)
    )

    private val viewNode = ViewNode(
        engine = engine,
        modelLoader = modelLoader,
        viewAttachmentManager = ViewAttachmentManager(
            context,
            this
        ).apply { onResume() }
    ).apply {
        loadView(context, R.layout.view_location)
    }

    init {
        sessionConfiguration = { session, config ->
            val filter = CameraConfigFilter(session).apply {
                setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
            }

            session.getSupportedCameraConfigs(filter)
                .firstOrNull()
                ?.let { session.cameraConfig = it }

            config.geospatialMode = Config.GeospatialMode.ENABLED
            config.streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.focusMode = Config.FocusMode.FIXED
            config.depthMode = Config.DepthMode.AUTOMATIC
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        }

        onSessionUpdated = { session, frame ->
            if (System.currentTimeMillis() - creationTime > 1000) {
                if (session.earth != earth)
                    earth = session.earth

                if (!spherePlaced && frame.camera.trackingState == TrackingState.TRACKING) {
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

                if (
                    currentDestination == null &&
                    earth != null &&
                    earth!!.trackingState == TrackingState.TRACKING
                ) {
                    val cameraGeo = earth!!.cameraGeospatialPose

                    if (pendingDestination != null) {
                        replaceMarker(
                            destLat = pendingDestination!!.latitude,
                            destLng = pendingDestination!!.longitude
                        )

                        pendingDestination = null
                    }

                    onDestinationChanged?.invoke(
                        LatLng(
                            cameraGeo.latitude,
                            cameraGeo.longitude
                        )
                    )
                }

                // Anchor successfully put so we can safely use the view node
                if (markerAnchorNode != null)
                    viewNode.lookAt(frame.camera.pose.position)

                markerAnchorNode?.let { anchor ->
                    val cameraGeo = earth?.cameraGeospatialPose ?: return@let
                    val destLat = currentDestination?.latitude ?: 0.0
                    val destLng = currentDestination?.longitude ?: 0.0

                    collectStats?.invoke(
                        anchor,
                        frame,
                        cameraGeo,
                        destLat,
                        destLng,
                        groundAltitude
                    )
                }

                setHorizontalAccuracy?.invoke(
                    earth?.cameraGeospatialPose
                        ?.horizontalAccuracy
                        ?: 0.0
                )

                afterSessionUpdated?.invoke()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        locationTrackerJob = coroutineScope.launch {
            earth?.cameraGeospatialPose?.let {
                val source = LatLng(it.latitude, it.longitude)
                onSourceLocationChanged?.invoke(source)
            }

            delay(30000)
        }
    }

    override fun onDetachedFromWindow() {
        locationTrackerJob?.cancel()
        locationTrackerJob = null

        super.onDetachedFromWindow()
    }

    fun processPath(path: List<LatLng>): List<PathPoint>? {
        val depthImage = try {
            frame?.acquireDepthImage16Bits()
        } catch (e: Exception) {
            return null
        }

        return path.mapNotNull {
            processPathPoint(
                latLng = it,
                depthImage = depthImage
            )
        }
    }

    private fun replaceMarker(
        destLat: Double,
        destLng: Double
    ) {
        markerAnchorNode?.let {
            removeChildNode(it)
            it.destroy()
        }

        earth!!.resolveAnchorOnTerrainAsync(
            destLat,
            destLng,
            0.5,
            0f, 0f, 0f, 1f
        ) { earthAnchor, state ->
            if (state == TerrainAnchorState.SUCCESS) {
                val anchorNode = AnchorNode(
                    engine,
                    earthAnchor
                ).apply {
                    addChildNode(viewNode)
                }

                addChildNode(anchorNode)
                markerAnchorNode = anchorNode
                val anchorGeoPose = earth!!.getGeospatialPose(earthAnchor.pose)
                groundAltitude = anchorGeoPose.altitude
                Log.d("ARDebug", "Geospatial Marker Placed at $destLat, $destLng")
            } else {
                Log.e("ARDebug", "Terrain Anchor failed: $state")
            }
        }
    }

    private fun processPathPoint(
        latLng: LatLng,
        depthImage: Image?
    ): PathPoint? {
        return try {
            // 1. Get Altitude (Snaps to terrain if on screen)
            val altitude = getTerrainAltitude(latLng)

            // 2. Project to Screen
            val pose = earth!!.getPose(
                latLng.latitude,
                latLng.longitude,
                altitude,
                0f, 0f, 0f, 1f
            )

            val screenOffset = pose.toScreenOffset(
                frame = frame!!,
                viewWidth = width.toFloat(),
                viewHeight = height.toFloat()
            )

            // 3. Occlusion Test
            val cameraRelativePose = frame!!.camera
                .pose
                .inverse()
                .compose(pose)

            val distance = cameraRelativePose
                .translation
                .let { t -> sqrt(t[0]*t[0] + t[1]*t[1] + t[2]*t[2]) }

            // Point is visible if it's in front of camera AND not behind a wall
            val isInFront = cameraRelativePose.tz() < 0
            val isNotObscured = depthImage == null ||
                    !isPointOccluded(
                        offset = screenOffset,
                        worldDistance = distance,
                        depthImage = depthImage
                    )

            PathPoint(screenOffset, isVisible = isInFront && isNotObscured)
        } catch (e: Exception) {
            null
        }
    }

    /** Finds the ground altitude by hit-testing the Streetscape mesh */
    private fun getTerrainAltitude(latLng: LatLng): Double {
        val tentativePose = earth!!.getPose(
            latLng.latitude,
            latLng.longitude,
            groundAltitude,
            0f, 0f, 0f, 1f
        )

        val screenPos = tentativePose.toScreenOffset(
            frame = frame!!,
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat()
        )

        if (screenPos.x !in 0f..width.toFloat() || screenPos.y !in 0f..height.toFloat())
            return groundAltitude

        return frame!!.hitTest(screenPos.x, screenPos.y)
            .firstOrNull {
                it.trackable is StreetscapeGeometry &&
                        (it.trackable as StreetscapeGeometry).type == StreetscapeGeometry.Type.TERRAIN
            }
            ?.let { earth!!.getGeospatialPose(it.hitPose).altitude }
            ?: groundAltitude
    }

    /** Checks the depth map to see if something is in front of the AR coordinate */
    private fun isPointOccluded(
        offset: Offset,
        worldDistance: Float,
        depthImage: Image
    ): Boolean {
        val realWorldDepth = depthImage.getDepthInMeters(
            offset.x,
            offset.y,
            width.toFloat(),
            height.toFloat()
        )

        // Return true if real world is closer than our path point (with 1m buffer)
        return realWorldDepth in 0.1f..<(worldDistance - 1.0f)
    }
}