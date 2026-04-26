package com.example.arcoresnippet.ui.screen.arcore

import android.media.Image
import com.google.ar.core.SemanticLabel

fun Image.getSemanticLabel(
    x: Float,
    y: Float,
    viewWidth: Float,
    viewHeight: Float
): SemanticLabel {
    val xI = ((x / viewWidth) * width).toInt()
        .coerceIn(0, width - 1)

    val yI = ((y / viewHeight) * height).toInt()
        .coerceIn(0, height - 1)

    val plane = planes[0]
    val buffer = plane.buffer
    val pixelIndex = yI * plane.rowStride + xI * plane.pixelStride

    // Semantic labels are stored as single bytes
    val labelValue = buffer.get(pixelIndex)
        .toInt()

    return SemanticLabel.entries
        .firstOrNull { it.ordinal == labelValue }
        ?: SemanticLabel.UNLABELED
}