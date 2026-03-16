package com.example.arcoresnippet.screen.arcore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcoresnippet.repository.RecordingsRepository
import com.example.arcoresnippet.toFileUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ARCoreViewModel @Inject constructor(
    private val recordingsRepository: RecordingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ARCoreScreenState())
    val uiState = _uiState.asStateFlow()

    fun createNewRecording() = viewModelScope.launch {
        val uri = recordingsRepository.createNewRecording()
            .fileName
            .toFileUri()

        _uiState.value = _uiState.value.copy(
            recorderState = ARCoreRecorderState.RECORDING,
            fileUri = uri
        )
    }

    fun openRecording(uriString: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            recorderState = ARCoreRecorderState.PLAYBACK,
            fileUri = uriString.toFileUri()
        )
    }
}