package com.example.arcoresnippet.ui.screen.arcore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcoresnippet.domain.repository.DirectionsRepository
import com.example.arcoresnippet.domain.repository.RecordingsRepository
import com.example.arcoresnippet.util.toFileUri
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ARCoreViewModel @Inject constructor(
    private val recordingsRepository: RecordingsRepository,
    private val directionsRepository: DirectionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ARCoreScreenState())
    val uiState = _uiState.asStateFlow()

    fun setDestination(latLng: LatLng) = viewModelScope.launch{
        _uiState.value = uiState.value.copy(
            destination = latLng,
            path = emptyList()
        )
    }

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

    fun showMapBottomSheet() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            mapsBottomSheetShown = true
        )
    }

    fun hideMapBottomSheet() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(
            mapsBottomSheetShown = false
        )
    }

    fun fetchRoadPath(origin: LatLng, dest: LatLng) {
        viewModelScope.launch {
            val path = directionsRepository.getRoute(
                from = origin,
                to = dest,
            )

            _uiState.value = _uiState.value.copy(
                path = path
            )
        }
    }
}