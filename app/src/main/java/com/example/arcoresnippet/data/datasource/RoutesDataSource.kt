package com.example.arcoresnippet.data.datasource

import com.example.arcoresnippet.data.entity.RoutesRequest
import com.example.arcoresnippet.data.entity.RoutesResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RoutesDataSource {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoutes(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = "routes.polyline",
        @Body request: RoutesRequest
    ): RoutesResponse
}