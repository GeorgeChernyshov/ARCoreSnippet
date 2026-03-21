package com.example.arcoresnippet.ui

data class AppState(
    val cameraPermissionGranted: Boolean = false,
    val coarseLocationPermissionGranted: Boolean = false,
    val fineLocationPermissionGranted: Boolean = false,
    val arSupported: Boolean = false
) {
    val allPermissionsGranted = cameraPermissionGranted &&
            coarseLocationPermissionGranted &&
            fineLocationPermissionGranted &&
            arSupported
}