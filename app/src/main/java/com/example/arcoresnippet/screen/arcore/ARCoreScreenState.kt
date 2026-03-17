package com.example.arcoresnippet.screen.arcore

import android.net.Uri

data class ARCoreScreenState(
    val recorderState: ARCoreRecorderState = ARCoreRecorderState.INITIAL,
    val fileUri: Uri? = null,
    val mapsBottomSheetShown: Boolean = false
)