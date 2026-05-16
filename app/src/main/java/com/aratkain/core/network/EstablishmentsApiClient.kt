package com.aratkain.core.network

import com.aratkain.core.model.NearbyRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.aratkain.core.model.EstablishmentResponse


// ── Retrofit interface ────────────────────────────────────────
interface EstablishmentApiService {

    // POST /api/establishments/nearby
    @POST("establishments/nearby")
    suspend fun findNearby(@Body request: NearbyRequest): List<EstablishmentResponse>

    // GET /api/establishments
    @GET("establishments")
    suspend fun getAll(): List<EstablishmentResponse>

    // GET /api/establishments/search?q=...
    @GET("establishments/search")
    suspend fun search(@Query("q") query: String): List<EstablishmentResponse>

    // GET /api/establishments/type/{type}
    @GET("establishments/type/{type}")
    suspend fun filterByType(@Path("type") type: String): List<EstablishmentResponse>
}

// ── Singleton client ──────────────────────────────────────────
object EstablishmentApiClient {

    private const val BASE_URL = "https://aratkain-backend.onrender.com/api/"


    val service: EstablishmentApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EstablishmentApiService::class.java)
    }
}