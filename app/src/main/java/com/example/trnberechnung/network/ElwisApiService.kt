package com.example.trnberechnung.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API service for fetching ELWIS (Elektronischer Wasserstraßen-Informationsservice)
 * nautical warnings and BfS (Bekanntmachungen für Seefahrer) messages.
 *
 * Uses the BSH GDI WFS endpoint for nautical warnings in the North Sea / German Bight area.
 */
interface ElwisApiService {

    /**
     * Fetch active nautical warnings from BSH (NtM / BfS).
     * The BSH publishes these as GeoJSON features.
     */
    @GET("ldproxy/rest/services/NauticalWarnings/collections/NauticalWarnings/items")
    suspend fun getNauticalWarnings(
        @Query("limit") limit: Int = 50,
        @Query("f") format: String = "json"
    ): Response<NauticalWarningsResponse>
}

/**
 * Response wrapper for BSH Nautical Warnings GeoJSON.
 */
data class NauticalWarningsResponse(
    val type: String? = null,
    val features: List<NauticalWarningFeature>? = null,
    val numberReturned: Int? = null
)

data class NauticalWarningFeature(
    val type: String? = null,
    val id: String? = null,
    val properties: NauticalWarningProperties? = null,
    val geometry: NauticalWarningGeometry? = null
)

data class NauticalWarningProperties(
    val warningNumber: String? = null,
    val warningType: String? = null,       // e.g. "NfS", "T", "P"
    val areaDescription: String? = null,
    val subject: String? = null,
    val text: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val authority: String? = null,
    val status: String? = null
)

data class NauticalWarningGeometry(
    val type: String? = null,
    val coordinates: List<Any>? = null    // Can be Point [lon, lat] or other geometry types
)
