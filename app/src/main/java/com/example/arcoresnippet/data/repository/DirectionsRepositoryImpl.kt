package com.example.arcoresnippet.data.repository

import com.example.arcoresnippet.data.datasource.RoutesDataSource
import com.example.arcoresnippet.data.entity.LatLngLiteral
import com.example.arcoresnippet.data.entity.Location
import com.example.arcoresnippet.data.entity.RoutesRequest
import com.example.arcoresnippet.data.entity.Waypoint
import com.example.arcoresnippet.domain.repository.DirectionsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class DirectionsRepositoryImpl @Inject constructor(
    private val routesDataSource: RoutesDataSource,
    @Named("arcore_api_key") private val apiKey: String
) : DirectionsRepository {

    override suspend fun getRoute(
        from: LatLng,
        to: LatLng
    ): List<LatLng> = withContext(Dispatchers.IO) {
        try {
            val sourceLocation = Location(
                LatLngLiteral(from.latitude, from.longitude)
            )

            val destinationLocation = Location(
                LatLngLiteral(to.latitude, to.longitude)
            )

            val request = RoutesRequest(
                origin = Waypoint(sourceLocation),
                destination = Waypoint(destinationLocation),
                travelMode = "WALK",
                polylineEncoding = "ENCODED_POLYLINE"
            )

            val response = routesDataSource.computeRoutes(
                apiKey = apiKey,
                request = request
            )

            val encodedString = response.routes.firstOrNull()?.polyline?.encodedPolyline

            return@withContext if (encodedString != null) {
                decodePolyline(encodedString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Standard Google Polyline Algorithm Decoder
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
        var lastLat = 0
        var lastLng = 0

        return sequence {
            val chars = encoded.iterator()
            while (chars.hasNext()) {
                // 1. Extract a single VLQ integer from the character stream
                var shift = 0
                var result = 0
                while (true) {
                    val b = chars.next().code - 63
                    result = result or (b and 0x1f shl shift)
                    shift += 5
                    if (b < 0x20) break // 0x20 is the "continuation bit"
                }

                // 2. De-ZigZag the value (convert from unsigned-style bits back to signed Int)
                // The formula is: (n >> 1) ^ -(n & 1)
                val decodedValue = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
                yield(decodedValue)
            }
        }
        .chunked(2) // Group into [latitudeDelta, longitudeDelta] pairs
        .map { (dLat, dLng) ->
            // 3. Add deltas to previous coordinates (Cumulative decoding)
            lastLat += dLat
            lastLng += dLng
            LatLng(lastLat / 1e5, lastLng / 1e5)
        }
        .toList()
    }
}