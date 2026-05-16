package com.aratkain.core.model

import com.google.gson.annotations.SerializedName

// ── POST /api/establishments/nearby ──────────────────────────
data class NearbyRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 10.0
)

// ── Response shape ────────────────────────────────────────────
data class EstablishmentResponse(
    @SerializedName("establishmentId") val id: Long,
    val name: String,
    @SerializedName("estabType") val type: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val city: String?,
    @SerializedName("averageRating") val rating: Double?,
    val reviewCount: Int?,
    val isOpen: Boolean?,
    val distanceKm: Double?
)