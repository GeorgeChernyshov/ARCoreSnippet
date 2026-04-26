//package com.example.arcoresnippet.ui.screen.arcore
//
//import android.content.Context
//import android.media.Image
//import android.util.Log
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Paint
//import androidx.lifecycle.DefaultLifecycleObserver
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleOwner
//import com.example.arcoresnippet.R
//import com.google.android.gms.maps.model.LatLng
//import com.google.ar.core.Anchor.TerrainAnchorState
//import com.google.ar.core.CameraConfig
//import com.google.ar.core.CameraConfigFilter
//import com.google.ar.core.Config
//import com.google.ar.core.Earth
//import com.google.ar.core.Frame
//import com.google.ar.core.GeospatialPose
//import com.google.ar.core.Pose
//import com.google.ar.core.SemanticLabel
//import com.google.ar.core.StreetscapeGeometry
//import com.google.ar.core.TrackingState
//import io.github.sceneview.ar.arcore.position
//import io.github.sceneview.ar.node.AnchorNode
//import io.github.sceneview.node.SphereNode
//import io.github.sceneview.node.ViewNode
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import java.util.EnumSet
//import kotlin.math.sqrt
//
//class ARSceneView2(context: Context) : ARSceneView(context) {
//
////    var currentDestination: LatLng? = null
////        set(value) {
////            if (earth?.trackingState == TrackingState.TRACKING) {
//////                replaceMarker(
//////                    destLat = value?.latitude ?: 0.0,
//////                    destLng = value?.longitude ?: 0.0
//////                )
////            } else {
////                pendingDestination = value
////            }
////
////            field = value
////
////        }
////
////    var onDestinationChanged: ((LatLng) -> Unit)? = null
////    var collectStats: ((
////        anchor: AnchorNode,
////        frame: Frame,
////        cameraGeo: GeospatialPose,
////        destLan: Double,
////        destLng: Double,
////        altitude: Double
////    ) -> Unit)? = null
////
////    var setHorizontalAccuracy: ((Double) -> Unit)? = null
////    var onSourceLocationChanged: ((LatLng) -> Unit)? = null
////    var afterSessionUpdated: (() -> Unit)? = null
////
////    private var earth: Earth? = null
////    private var markerAnchorNode: AnchorNode? = null
////    private var groundAltitude = 0.0
////    private var spherePlaced = false
////    private var pendingDestination: LatLng? = null
////
////
////    private val creationTime = System.currentTimeMillis()

////
//    private val viewNode = ViewNode(
//        engine = engine,
//        modelLoader = modelLoader,
//        viewAttachmentManager = ViewAttachmentManager(
//            context,
//            this
//        ).apply { onResume() }
//    ).apply {
//        loadView(context, R.layout.view_location)
//    }
////
////    init {
////
//////        onSessionUpdated = { session, frame ->
//////            if (System.currentTimeMillis() - creationTime > 3000) {
//////
//////
//////                if (
//////                    earth != null &&
//////                    earth!!.trackingState == TrackingState.TRACKING
//////                ) {
//////
//////                    if (currentDestination == null &&
//////                        pendingDestination == null
//////                    ) {
//////                        val cameraGeo = earth!!.cameraGeospatialPose
//////
//////                        onDestinationChanged?.invoke(
//////                            LatLng(
//////                                cameraGeo.latitude,
//////                                cameraGeo.longitude
//////                            )
//////                        )
//////                    }
//////
//////                    if (pendingDestination != null) {
//////                        replaceMarker(
//////                            destLat = pendingDestination!!.latitude,
//////                            destLng = pendingDestination!!.longitude
//////                        )
//////
//////                        pendingDestination = null
//////                    }
//////                }
//////            }
//////        }
////    }
////
////
////    fun processPath(path: List<LatLng>): List<PathPoint>? {
//////        val semanticImage = try {
//////            frame?.acquireSemanticImage()
//////        } catch (e: Exception) {
//////            return null
//////        }
////
////        val semanticImage = null
////
//////        return path.mapNotNull {
//////            processPathPoint(
//////                latLng = it,
//////                semanticImage = semanticImage
//////            )
//////        }
////
////        return emptyList()
////    }
////
//////
////
////    private fun processPathPoint(
////        latLng: LatLng,
////        semanticImage: Image?
////    ): PathPoint? {
////        return PathPoint(Offset(0f, 0f), true)
////    }
//////        return try {
//////            // 1. Get Altitude (Snaps to terrain if on screen)
//////            val altitude = getTerrainAltitude(latLng)
//////
//////            // 2. Project to Screen
//////            val pose = earth!!.getPose(
//////                latLng.latitude,
//////                latLng.longitude,
//////                altitude,
//////                0f, 0f, 0f, 1f
//////            )
//////
//////            val screenOffset = pose.toScreenOffset(
//////                frame = frame!!,
//////                viewWidth = width.toFloat(),
//////                viewHeight = height.toFloat()
//////            )
//////
//////            // 3. Occlusion Test
//////            val cameraRelativePose = frame!!.camera
//////                .pose
//////                .inverse()
//////                .compose(pose)
//////
//////            // Point is visible if it's in front of camera AND not behind a wall
//////            val isInFront = cameraRelativePose.tz() < 0
//////            val isNotObscured = semanticImage == null ||
//////                    !isPointOccluded(
//////                        offset = screenOffset,
//////                        semanticImage = semanticImage
//////                    )
//////
//////            PathPoint(screenOffset, isVisible = isInFront && isNotObscured)
//////        } catch (e: Exception) {
//////            null
//////        }
//////    }
////
////    /** Finds the ground altitude by hit-testing the Streetscape mesh */
//////    private fun getTerrainAltitude(latLng: LatLng): Double {
//////        val tentativePose = earth!!.getPose(
//////            latLng.latitude,
//////            latLng.longitude,
//////            groundAltitude,
//////            0f, 0f, 0f, 1f
//////        )
//////
//////        val screenPos = tentativePose.toScreenOffset(
//////            frame = frame!!,
//////            viewWidth = width.toFloat(),
//////            viewHeight = height.toFloat()
//////        )
//////
//////        if (screenPos.x !in 0f..width.toFloat() || screenPos.y !in 0f..height.toFloat())
//////            return groundAltitude
//////
//////        return frame!!.hitTest(screenPos.x, screenPos.y)
//////            .firstOrNull {
//////                it.trackable is StreetscapeGeometry &&
//////                        (it.trackable as StreetscapeGeometry).type == StreetscapeGeometry.Type.TERRAIN
//////            }
//////            ?.let { earth!!.getGeospatialPose(it.hitPose).altitude }
//////            ?: groundAltitude
//////    }
////
////    /** Checks the depth map to see if something is in front of the AR coordinate */
////    private fun isPointOccluded(
////        offset: Offset,
////        semanticImage: Image
////    ): Boolean {
////        return false
//////        val label = semanticImage.getSemanticLabel(
//////            x = offset.x,
//////            y =offset.y,
//////            viewWidth = width.toFloat(),
//////            viewHeight = height.toFloat()
//////        )
//////
//////        // If the point is projected onto a building, it's likely occluded
//////        if (label == SemanticLabel.BUILDING) return true
//////
//////        // If it's projected onto the sky, it's definitely visible
//////        if (label == SemanticLabel.SKY) return false
//////
//////        return false
////    }
////
//
//}