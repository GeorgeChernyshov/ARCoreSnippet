package com.example.arcoresnippet.screen.arcore

import android.net.Uri
import com.google.android.gms.maps.model.LatLng

data class ARCoreScreenState(
    val recorderState: ARCoreRecorderState = ARCoreRecorderState.INITIAL,
    val fileUri: Uri? = null,
    val destination: LatLng? = null,
    val mapsBottomSheetShown: Boolean = false
)