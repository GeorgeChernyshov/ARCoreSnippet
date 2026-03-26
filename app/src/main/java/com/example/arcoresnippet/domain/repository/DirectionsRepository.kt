package com.example.arcoresnippet.domain.repository

import com.google.android.gms.maps.model.LatLng

interface DirectionsRepository {

    suspend fun getRoute(
        from: LatLng,
        to: LatLng
    ): List<LatLng>
}