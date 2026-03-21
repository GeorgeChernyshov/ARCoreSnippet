package com.example.arcoresnippet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcoresnippet.domain.repository.PermissionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val permissionsRepository: PermissionsRepository
) : ViewModel() {

    private val _appState = MutableStateFlow(AppState())
    val appState = _appState.asStateFlow()

    suspend fun checkPermissions() {
        _appState.value = _appState.value.copy(
            cameraPermissionGranted = permissionsRepository.checkCameraPermission(),
            coarseLocationPermissionGranted = permissionsRepository.checkCoarseLocationPermission(),
            fineLocationPermissionGranted = permissionsRepository.checkFineLocationPermission()
        )
    }

    fun setARSupported(isSupported: Boolean) = viewModelScope.launch {
        _appState.value = _appState.value.copy(arSupported = isSupported)
    }

    fun setCameraPermissionGranted(isGranted: Boolean) = viewModelScope.launch {
        _appState.value = _appState.value.copy(cameraPermissionGranted = isGranted)
    }

    fun setCoarseLocationPermissionGranted(isGranted: Boolean) = viewModelScope.launch {
        _appState.value = _appState.value.copy(
            coarseLocationPermissionGranted = isGranted
        )
    }

    fun setFineLocationPermissionGranted(isGranted: Boolean) = viewModelScope.launch {
        _appState.value = _appState.value.copy(
            fineLocationPermissionGranted = isGranted
        )
    }
}