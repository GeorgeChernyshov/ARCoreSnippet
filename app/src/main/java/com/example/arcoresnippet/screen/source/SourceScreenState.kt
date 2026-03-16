package com.example.arcoresnippet.screen.source

import com.example.arcoresnippet.model.Recording

data class SourceScreenState(
    val recordings: List<Recording> = emptyList(),
    val recordingListShown: Boolean = false,
    val confirmDeleteDialogShown: Boolean = false,
)