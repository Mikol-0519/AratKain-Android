package com.aratkain.core.api

import com.aratkain.core.model.SupabaseAuthResponse
import com.aratkain.core.model.UserProfile
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ── Supabase credentials ──────────────────────────────────────
object SupabaseConfig {
    const val PROJECT_URL = "https://oyohrydkfhsmgwrejvga.supabase.co"
    const val ANON_KEY    = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im95b2hyeWRrZmhzbWd3cmVqdmdhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI4MTgxMjYsImV4cCI6MjA4ODM5NDEyNn0." +
            "oBD-bp0GJNCJI-OMOy8rUtK1eD_2iFKmzjcDF8_ICWA"
}

// ── Auth API (Supabase /auth/v1) ──────────────────────────────
interface SupabaseAuthApi {

    @POST("auth/v1/token?grant_type=password")
    fun signIn(
        @Body body: Map<String, String>
    ): Call<SupabaseAuthResponse>

    @POST("auth/v1/signup")
    fun signUp(
        @Body body: Map<String, String>
    ): Call<SupabaseAuthResponse>

    @PUT("auth/v1/user")
    fun updatePassword(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Call<SupabaseAuthResponse>
}

// ── Database API (Supabase /rest/v1) ─────────────────────────
interface SupabaseDbApi {

    @GET("rest/v1/users")
    fun getUser(
        @Header("Authorization") token: String,
        @Query("user_id") userId: String,
        @Query("select") select: String = "user_id,username,fullname,email,photo_url,role"
    ): Call<List<UserProfile>>

    @GET("rest/v1/users")
    fun getUserByEmail(
        @Header("Authorization") token: String,
        @Query("email") email: String,
        @Query("select") select: String = "email"
    ): Call<List<UserProfile>>

    @POST("rest/v1/users")
    fun insertUser(
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Body user: Map<String, String?>
    ): Call<Void>

    @PATCH("rest/v1/users")
    fun updateUser(
        @Header("Authorization") token: String,
        @Header("Prefer") prefer: String = "return=minimal",
        @Query("user_id") userId: String,
        @Body updates: Map<String, String?>
    ): Call<Void>
}

// ── Centralized Retrofit builder ──────────────────────────────
object SupabaseClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("apikey",       SupabaseConfig.ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30,    TimeUnit.SECONDS)
        .writeTimeout(30,   TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("${SupabaseConfig.PROJECT_URL}/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val auth: SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)
    val db:   SupabaseDbApi   = retrofit.create(SupabaseDbApi::class.java)
}