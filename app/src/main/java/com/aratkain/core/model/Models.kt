package com.aratkain.core.model

// ── Domain model shared across features ──────────────────────
data class UserData(
    val userId:   String,
    val username: String,
    val fullname: String,
    val email:    String,
    val token:    String,
    val photoUrl: String? = null
)

// ── Supabase Auth API response ────────────────────────────────
data class SupabaseAuthResponse(
    val access_token:      String?,
    val token_type:        String?,
    val user:              SupabaseUser?,
    val error:             String?,
    val error_description: String?,
    val msg:               String?
)

data class SupabaseUser(
    val id:    String?,
    val email: String?
)

// ── Supabase DB user row ──────────────────────────────────────
data class UserProfile(
    val user_id:   String?,
    val username:  String?,
    val fullname:  String?,
    val email:     String?,
    val role:      String?,
    val photo_url: String?
)