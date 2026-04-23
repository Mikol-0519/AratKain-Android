package com.aratkain.forgotpassword

import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.api.SupabaseConfig
import com.aratkain.core.model.UserProfile
import com.aratkain.core.utils.isValidEmail
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class ForgotPasswordPresenter(
    private var view: ForgotPasswordContract.View?
) : ForgotPasswordContract.Presenter {

    override fun onFindAccountClicked(email: String) {
        view?.clearErrors()

        // Validation in Presenter
        if (email.isEmpty()) { view?.showEmailError("Email is required"); return }
        if (!email.isValidEmail()) { view?.showEmailError("Enter a valid email address"); return }

        view?.showLoading()

        // Check if email exists in users table
        SupabaseClient.db.getUserByEmail(
            token = "Bearer ${SupabaseConfig.ANON_KEY}",
            email = "eq.$email"
        ).enqueue(object : Callback<List<UserProfile>> {

            override fun onResponse(
                call: Call<List<UserProfile>>,
                response: Response<List<UserProfile>>
            ) {
                view?.hideLoading()
                val found = !response.body().isNullOrEmpty()
                if (found) {
                    view?.goToStep2(email)
                } else {
                    view?.showEmailError("No account found with this email address")
                }
            }

            override fun onFailure(call: Call<List<UserProfile>>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    override fun onResetPasswordClicked(
        email:           String,
        newPassword:     String,
        confirmPassword: String
    ) {
        view?.clearErrors()

        // Validation in Presenter
        var valid = true
        if (newPassword.isEmpty()) {
            view?.showFieldError("newPassword", "Password is required"); valid = false
        } else if (newPassword.length < 6) {
            view?.showFieldError("newPassword", "At least 6 characters"); valid = false
        }
        if (newPassword != confirmPassword) {
            view?.showFieldError("confirmPassword", "Passwords do not match"); valid = false
        }
        if (!valid) return

        view?.showLoading()

        // Call Supabase RPC function reset_user_password
        val json = """{"user_email":"$email","new_password":"$newPassword"}"""
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("${SupabaseConfig.PROJECT_URL}/rest/v1/rpc/reset_user_password")
            .post(body)
            .addHeader("apikey",        SupabaseConfig.ANON_KEY)
            .addHeader("Authorization", "Bearer ${SupabaseConfig.ANON_KEY}")
            .addHeader("Content-Type",  "application/json")
            .build()

        val client = OkHttpClient()

        Thread {
            try {
                val response = client.newCall(request).execute()
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    view?.hideLoading()
                    if (response.isSuccessful) {
                        view?.showSuccess()
                    } else {
                        view?.showError("Failed to reset password. Please try again.")
                    }
                }
            } catch (e: IOException) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    view?.hideLoading()
                    view?.showError(e.toNetworkMessage())
                }
            }
        }.start()
    }

    private fun Throwable.toNetworkMessage() = when {
        message?.contains("Unable to resolve host") == true -> "No internet connection."
        message?.contains("timeout") == true                -> "Connection timed out."
        else                                                 -> "Network error. Please try again."
    }

    override fun onBackToLoginClicked() { view?.navigateToLogin() }
    override fun onDestroy()            { view = null }
}