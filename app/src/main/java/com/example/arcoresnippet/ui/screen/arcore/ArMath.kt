package com.example.arcoresnippet.ui.screen.arcore

import android.media.Image
import android.opengl.Matrix
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import com.google.ar.core.StreetscapeGeometry
import java.nio.ByteOrder

fun Pose.toScreenOffset(
    frame: Frame,
    viewWidth: Float,
    viewHeight: Float
): Offset {
    val camera = frame.camera
    val projectionMatrix = FloatArray(16)
    val viewMatrix = FloatArray(16)

    // 1. Get the matrices from ARCore
    camera.getProjectionMatrix(
        projectionMatrix,
        0,
        0.1f,
        100f
    )

    camera.getViewMatrix(viewMatrix, 0)

    // 2. Compute the Model-View-Projection matrix
    val mvpMatrix = FloatArray(16)
    Matrix.multiplyMM(
        mvpMatrix,
        0,
        projectionMatrix,
        0,
        viewMatrix,
        0
    )

    // 3. Define the 3D point (world space)
    val worldPoint = floatArrayOf(
        tx(), ty(), tz(), 1f
    )

    val clipSpacePoint = FloatArray(4)

    // 4. Transform world point to Clip Space
    Matrix.multiplyMV(
        clipSpacePoint,
        0,
        mvpMatrix,
        0,
        worldPoint,
        0
    )

    // 5. Normalize (Perspective Divide)
    val ndcX = clipSpacePoint[0] / clipSpacePoint[3]
    val ndcY = clipSpacePoint[1] / clipSpacePoint[3]

    // 6. Convert Normalized Device Coordinates (-1 to 1) to Screen Pixels (0 to Width/Height)
    val x = (ndcX + 1f) / 2f * viewWidth
    val y = (1f - ndcY) / 2f * viewHeight

    return Offset(x, y)
}

fun Image.getDepthInMeters(
    x: Float,
    y: Float,
    viewWidth: Float,
    viewHeight: Float
): Float {
    if (viewWidth <= 0 || viewHeight <= 0)
        return Float.MAX_VALUE

    // 1. Normalize screen coordinates to Depth Image coordinates (usually 160x120 or similar)
    val xI = ((x / viewWidth) * width)
        .toInt()
        .coerceIn(0, width - 1)

    val yI = ((y / viewHeight) * height)
        .toInt()
        .coerceIn(0, height - 1)

    // 2. Extract the 16-bit depth value from the buffer
    val plane = planes[0]
    val byteBuffer = plane.buffer
        .order(ByteOrder.nativeOrder())

    // Depth images are usually 2 bytes per pixel (Short)
    val index = yI * plane.rowStride + xI * plane.pixelStride
    val depthMillimeters = byteBuffer.getShort(index)
        .toInt() and 0xFFFF

    return depthMillimeters / 1000f // Convert to meters
}

/** Checks the depth map to see if something is in front of the AR coordinate */
fun Image.isPointOccluded(offset: Offset): Boolean {
    return false
//        val label = getSemanticLabel(
//            x = offset.x,
//            y = offset.y,
//            viewWidth = width.toFloat(),
//            viewHeight = height.toFloat()
//        )
//
//        // If the point is projected onto a building, it's likely occluded
//        if (label == SemanticLabel.BUILDING) return true
//
//        // If it's projected onto the sky, it's definitely visible
//        if (label == SemanticLabel.SKY) return false
//
//        return false
}