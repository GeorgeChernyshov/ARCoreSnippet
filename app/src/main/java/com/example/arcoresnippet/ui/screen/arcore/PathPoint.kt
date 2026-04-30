package com.example.arcoresnippet.ui.screen.arcore

import android.media.Image
import androidx.compose.ui.geometry.Offset
import com.google.android.gms.maps.model.LatLng
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.Pose

data class PathPoint(
    val pose: Pose,
    val screenOffset: Offset
) {
    var isVisible: Boolean = false

    constructor(
        pose: Pose,
        frame: Frame,
        viewWidth: Float,
        viewHeight: Float
    ) : this(
        pose = pose,
        screenOffset = pose.toScreenOffset(
            frame = frame,
            viewWidth = viewWidth,
            viewHeight = viewHeight
        )
    )

    constructor(
        latLng: LatLng,
        earth: Earth,
        frame: Frame,
        altitude: Double,
        viewWidth: Float,
        viewHeight: Float
    ) : this(
        pose = earth.getPose(
            latLng.latitude,
            latLng.longitude,
            altitude,
            0f, 0f, 0f, 1f
        ),
        frame = frame,
        viewWidth = viewWidth,
        viewHeight = viewHeight
    )

    // Checks if PathPoint is occluded
    fun checkOcclusion(
        semanticImage: Image,
        cameraPose: Pose
    ) {
        // Occlusion Test
        val cameraRelativePose = cameraPose
            .inverse()
            .compose(pose)

        // Point is visible if it's in front of camera AND not behind a wall
        val isInFront = cameraRelativePose.tz() < 0
        val isNotObscured = !semanticImage.isPointOccluded(screenOffset)

        isVisible = isInFront && isNotObscured
    }
}