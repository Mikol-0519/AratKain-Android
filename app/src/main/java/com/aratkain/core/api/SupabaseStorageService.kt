package com.aratkain.core.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

// ════════════════════════════════════════════════════════════
// SupabaseStorageService
// Matches Supabase Storage REST API:
//   POST /storage/v1/object/{bucket}/{path}
//
// Add an instance of this to your SupabaseClient, e.g.:
//   val storage: SupabaseStorageService = retrofit.create(SupabaseStorageService::class.java)
// ════════════════════════════════════════════════════════════
interface SupabaseStorageService {

    /**
     * Upload (or replace) a file in Supabase Storage.
     *
     * @param token  Bearer token — use session.getBearerToken()
     * @param bucket Your storage bucket name, e.g. "avatars"
     * @param path   File path inside the bucket, e.g. "{userId}.jpg"
     */
    @Multipart
    @POST("storage/v1/object/{bucket}/{path}")
    fun uploadFile(
        @Header("Authorization") token:  String,
        @Path("bucket")          bucket: String,
        @Path("path")            path:   String,
        @Part                    file:   MultipartBody.Part
    ): Call<ResponseBody>
}