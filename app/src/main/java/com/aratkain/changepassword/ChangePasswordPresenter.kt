package com.aratkain.changepassword

import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.model.SupabaseAuthResponse
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.parseErrorMessage
import com.aratkain.core.utils.toNetworkMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordPresenter(
    private var view:    ChangePasswordContract.View?,
    private val session: SessionManager
) : ChangePasswordContract.Presenter {

    override fun onUpdateClicked(newPassword: String, confirmPassword: String) {
        view?.clearErrors()

        // Validation in Presenter
        var valid = true
        if (newPassword.isEmpty()) {
            view?.showFieldError("newPassword", "New password is required"); valid = false
        } else if (newPassword.length < 6) {
            view?.showFieldError("newPassword", "Password must be at least 6 characters"); valid = false
        }
        if (newPassword != confirmPassword) {
            view?.showFieldError("confirmPassword", "Passwords do not match"); valid = false
        }
        if (!valid) return

        view?.showLoading()

        val body = mapOf("password" to newPassword)

        SupabaseClient.auth.updatePassword(
            token = session.getBearerToken(),
            body  = body
        ).enqueue(object : Callback<SupabaseAuthResponse> {

            override fun onResponse(
                call: Call<SupabaseAuthResponse>,
                response: Response<SupabaseAuthResponse>
            ) {
                view?.hideLoading()
                if (response.isSuccessful) {
                    view?.clearFields()
                    view?.showSuccess("Password updated successfully!")
                } else {
                    view?.showError(response.parseErrorMessage())
                }
            }

            override fun onFailure(call: Call<SupabaseAuthResponse>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    override fun onDestroy() { view = null }
}