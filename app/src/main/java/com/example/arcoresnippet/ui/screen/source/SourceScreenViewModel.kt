package com.example.arcoresnippet.ui.screen.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcoresnippet.domain.repository.RecordingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SourceScreenViewModel @Inject constructor(
    private val recordingsRepository: RecordingsRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(SourceScreenState())
    val uiState = _uiState.asStateFlow()

    fun showRecordingsList() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            recordingListShown = true
        )
    }

    fun showDeleteConfirmationDialog() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            confirmDeleteDialogShown = true
        )
    }

    fun hideDeleteConfirmationDialog() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            confirmDeleteDialogShown = false
        )
    }

    fun refreshRecordings() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            recordings = recordingsRepository.listRecordings()
        )
    }

    fun deleteRecordings() = viewModelScope.launch {
        recordingsRepository.deleteAllRecordings()
    }
}