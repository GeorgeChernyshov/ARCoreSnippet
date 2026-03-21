package com.example.arcoresnippet.ui.screen.source

import com.example.arcoresnippet.domain.model.Recording

data class SourceScreenState(
    val recordings: List<Recording> = emptyList(),
    val recordingListShown: Boolean = false,
    val confirmDeleteDialogShown: Boolean = false,
)