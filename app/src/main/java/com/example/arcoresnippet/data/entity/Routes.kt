package com.example.arcoresnippet.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoutesRequest(
    val origin: Waypoint,
    val destination: Waypoint,
    val travelMode: String = "WALK", // "DRIVE", "BICYCLE", "WALK"
    val polylineEncoding: String = "ENCODED_POLYLINE"
)

@JsonClass(generateAdapter = true)
data class Waypoint(
    val location: Location
)

@JsonClass(generateAdapter = true)
data class Location(
    val latLng: LatLngLiteral
)

@JsonClass(generateAdapter = true)
data class LatLngLiteral(
    val latitude: Double,
    val longitude: Double
)

@JsonClass(generateAdapter = true)
data class RoutesResponse(
    val routes: List<RouteResponse>
)

@JsonClass(generateAdapter = true)
data class RouteResponse(
    val polyline: PolylineData
)

@JsonClass(generateAdapter = true)
data class PolylineData(
    val encodedPolyline: String
)