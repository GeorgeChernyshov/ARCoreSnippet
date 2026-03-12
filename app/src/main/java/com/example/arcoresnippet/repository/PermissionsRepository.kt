package com.example.arcoresnippet.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PermissionsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun checkCameraPermission() = checkPermission(
        Manifest.permission.CAMERA
    )

    fun checkCoarseLocationPermission() = checkPermission(
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun checkFineLocationPermission() = checkPermission(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun checkPermission(
        permission: String
    ): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}