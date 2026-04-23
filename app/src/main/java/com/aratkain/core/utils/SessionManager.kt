package com.aratkain.core.utils

import android.content.Context
import android.content.SharedPreferences
import com.aratkain.core.model.UserData

// ── Manages JWT token + user data in SharedPreferences ────────
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("AratKainSession", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN    = "access_token"
        private const val KEY_USER_ID  = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULLNAME = "fullname"
        private const val KEY_EMAIL    = "email"
        private const val KEY_PHOTO    = "photo_url"
    }

    fun saveSession(user: UserData) {
        prefs.edit().apply {
            putString(KEY_TOKEN,    user.token)
            putString(KEY_USER_ID,  user.userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULLNAME, user.fullname)
            putString(KEY_EMAIL,    user.email)
            user.photoUrl?.let { putString(KEY_PHOTO, it) }
            apply()
        }
    }

    fun getToken():    String? = prefs.getString(KEY_TOKEN,    null)
    fun getUserId():   String? = prefs.getString(KEY_USER_ID,  null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getFullname(): String? = prefs.getString(KEY_FULLNAME, null)
    fun getEmail():    String? = prefs.getString(KEY_EMAIL,    null)
    fun getPhotoUrl(): String? = prefs.getString(KEY_PHOTO,    null)

    fun getBearerToken(): String = "Bearer ${getToken()}"

    fun isLoggedIn(): Boolean = getToken() != null

    fun updateProfile(username: String, fullname: String) {
        prefs.edit().apply {
            putString(KEY_USERNAME, username)
            putString(KEY_FULLNAME, fullname)
            apply()
        }
    }

    fun getCurrentUser(): UserData? {
        val token = getToken() ?: return null
        val userId = getUserId() ?: return null
        return UserData(
            userId   = userId,
            username = getUsername() ?: "",
            fullname = getFullname() ?: "",
            email    = getEmail()    ?: "",
            token    = token,
            photoUrl = getPhotoUrl()
        )
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}