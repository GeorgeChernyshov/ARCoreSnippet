package com.example.arcoresnippet.ui.screen.arcore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import io.github.sceneview.ar.node.AnchorNode

@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    currentDestination: LatLng?,
    currentPath: List<LatLng>?,
    setDestination: (LatLng) -> Unit,
    onSourceLocationChanged: (LatLng) -> Unit,
    collectStats: (
        anchor: AnchorNode,
        frame: Frame,
        cameraGeo: GeospatialPose,
        destLan: Double,
        destLng: Double,
        altitude: Double
    ) -> Unit,
    setHorizontalAccuracy: (Double) -> Unit
) {
    var sceneView by remember { mutableStateOf<ARSceneView?>(null) }
    val currentPathState by rememberUpdatedState(currentPath)
    var visibleSegments by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                sceneView = ARSceneView(context).apply {
                    this.onDestinationChanged = setDestination
                    this.collectStats = collectStats
                    this.setHorizontalAccuracy = setHorizontalAccuracy
                    this.onSourceLocationChanged = onSourceLocationChanged
                    this.afterSessionUpdated = {
                        currentPathState?.let { path ->
                            processPath(path = path)?.let {
                                visibleSegments = assembleSegments(
                                    startPoint = Offset(
                                        width.toFloat() / 2f,
                                        height.toFloat()
                                    ),
                                    points = it
                                )
                            }
                        }
                    }
                }

                sceneView!!
            },
            update = { view ->
                view.currentDestination = currentDestination
            }
        )

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
            currentSegment.add(pt.offset)
        } else if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment)
            currentSegment = mutableListOf()
        }
    }

    if (currentSegment.isNotEmpty())
        segments.add(currentSegment)

    return segments
}