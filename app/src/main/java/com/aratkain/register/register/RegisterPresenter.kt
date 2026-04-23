package com.aratkain.register.register

import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.model.SupabaseAuthResponse
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.isValidEmail
import com.aratkain.core.utils.isValidUsername
import com.aratkain.core.utils.parseErrorMessage
import com.aratkain.core.utils.toNetworkMessage
import com.aratkain.register.RegisterContract
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterPresenter(
    private var view: RegisterContract.View?
) : RegisterContract.Presenter {

    override fun onRegisterClicked(
        username:        String,
        fullname:        String,
        email:           String,
        password:        String,
        confirmPassword: String
    ) {
        view?.clearErrors()

        // ── Validation (in Presenter, not View) ───────────────
        var valid = true

        if (username.isEmpty()) {
            view?.showFieldError("username", "Username is required"); valid = false
        } else if (!username.isValidUsername()) {
            view?.showFieldError("username", "3-50 chars: letters, numbers, underscores only"); valid = false
        }

        if (fullname.isEmpty()) {
            view?.showFieldError("fullname", "Full name is required"); valid = false
        }

        if (email.isEmpty()) {
            view?.showFieldError("email", "Email is required"); valid = false
        } else if (!email.isValidEmail()) {
            view?.showFieldError("email", "Enter a valid email address"); valid = false
        }

        if (password.isEmpty()) {
            view?.showFieldError("password", "Password is required"); valid = false
        } else if (password.length < 6) {
            view?.showFieldError("password", "Password must be at least 6 characters"); valid = false
        }

        if (password != confirmPassword) {
            view?.showFieldError("confirmPassword", "Passwords do not match"); valid = false
        }

        if (!valid) return

        view?.showLoading()

        // Step 1: Sign up via Supabase Auth
        val authBody = mapOf("email" to email, "password" to password)

        SupabaseClient.auth.signUp(authBody).enqueue(object : Callback<SupabaseAuthResponse> {

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
                    view?.showError("Registration failed. Please try again.")
                    return
                }

                // Step 2: Insert user profile into users table
                insertProfile(token, userId, username, fullname, email)
            }

            override fun onFailure(call: Call<SupabaseAuthResponse>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    // ── Insert into Supabase users table ──────────────────────
    private fun insertProfile(
        token:    String,
        userId:   String,
        username: String,
        fullname: String,
        email:    String
    ) {
        val userMap = mapOf(
            "user_id"  to userId,
            "username" to username,
            "fullname" to fullname,
            "email"    to email,
            "role"     to "user"
        )

        SupabaseClient.db.insertUser(
            token = "Bearer $token",
            user  = userMap
        ).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                view?.hideLoading()

                val user = UserData(
                    userId   = userId,
                    username = username,
                    fullname = fullname,
                    email    = email,
                    token    = token
                )
                view?.showSuccess(user)
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                view?.hideLoading()
                // Still show success even if profile insert fails
                val user = UserData(
                    userId   = userId,
                    username = username,
                    fullname = fullname,
                    email    = email,
                    token    = token
                )
                view?.showSuccess(user)
            }
        })
    }

    override fun onGoToDashboardClicked(user: UserData) {
        view?.navigateToDashboard(user)
    }

    override fun onDestroy() {
        view = null
    }
}