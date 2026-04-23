package com.aratkain.login

import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.model.SupabaseAuthResponse
import com.aratkain.core.model.UserData
import com.aratkain.core.model.UserProfile
import com.aratkain.core.utils.isValidEmail
import com.aratkain.core.utils.parseErrorMessage
import com.aratkain.core.utils.toNetworkMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginPresenter(
    private var view: LoginContract.View?
) : LoginContract.Presenter {

    // ── Login ─────────────────────────────────────────────────
    override fun onLoginClicked(email: String, password: String) {

        // Validation lives in Presenter (not View)
        if (email.isEmpty()) {
            view?.showEmailError("Email is required")
            return
        }
        if (!email.isValidEmail()) {
            view?.showEmailError("Enter a valid email address")
            return
        }
        if (password.isEmpty()) {
            view?.showPasswordError("Password is required")
            return
        }
        if (password.length < 6) {
            view?.showPasswordError("Password must be at least 6 characters")
            return
        }

        view?.showLoading()
        view?.clearErrors()

        // Step 1: Sign in via Supabase Auth
        // Some Supabase configurations expect a flat map of credentials
        val body = mapOf(
            "email" to email,
            "password" to password
        )

        SupabaseClient.auth.signIn(body).enqueue(object : Callback<SupabaseAuthResponse> {

            override fun onResponse(
                call: Call<SupabaseAuthResponse>,
                response: Response<SupabaseAuthResponse>
            ) {
                if (!response.isSuccessful) {
                    view?.hideLoading()
                    view?.showError(response.parseErrorMessage())
                    return
                }

                val token  = response.body()?.access_token
                val userId = response.body()?.user?.id

                if (token == null || userId == null) {
                    view?.hideLoading()
                    view?.showError("Login failed. Please try again.")
                    return
                }

                // Step 2: Fetch profile from users table
                fetchProfile(token, userId, email)
            }

            override fun onFailure(call: Call<SupabaseAuthResponse>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    // ── Fetch user profile from Supabase DB ───────────────────
    private fun fetchProfile(token: String, userId: String, email: String) {
        SupabaseClient.db.getUser(
            token  = "Bearer $token",
            userId = "eq.$userId"
        ).enqueue(object : Callback<List<UserProfile>> {

            override fun onResponse(
                call: Call<List<UserProfile>>,
                response: Response<List<UserProfile>>
            ) {
                view?.hideLoading()

                val profile = response.body()?.firstOrNull()

                val user = UserData(
                    userId   = userId,
                    username = profile?.username ?: email.substringBefore("@"),
                    fullname = profile?.fullname ?: "",
                    email    = email,
                    token    = token,
                    photoUrl = profile?.photo_url
                )

                view?.navigateToDashboard(user)
            }

            override fun onFailure(call: Call<List<UserProfile>>, t: Throwable) {
                view?.hideLoading()
                // Still navigate even if profile fetch fails
                val user = UserData(
                    userId   = userId,
                    username = email.substringBefore("@"),
                    fullname = "",
                    email    = email,
                    token    = token
                )
                view?.navigateToDashboard(user)
            }
        })
    }

    override fun onClearClicked() {
        view?.clearInputs()
        view?.clearErrors()
    }

    override fun onDestroy() {
        view = null // Prevent memory leaks
    }
}