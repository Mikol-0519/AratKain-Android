package com.aratkain.updateprofile

import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.isValidUsername
import com.aratkain.core.utils.parseErrorMessage
import com.aratkain.core.utils.toNetworkMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UpdateProfilePresenter(
    private var view:    UpdateProfileContract.View?,
    private val session: SessionManager
) : UpdateProfileContract.Presenter {

    override fun onViewCreated() {
        // Pre-fill fields with current session data
        val username = session.getUsername() ?: ""
        val fullname = session.getFullname() ?: ""
        val email    = session.getEmail()    ?: ""
        view?.prefillData(username, fullname, email)
    }

    override fun onSaveClicked(username: String, fullname: String) {
        view?.clearErrors()

        // Validation in Presenter
        var valid = true
        if (username.isEmpty()) {
            view?.showFieldError("username", "Username is required"); valid = false
        } else if (!username.isValidUsername()) {
            view?.showFieldError("username", "3-50 chars: letters, numbers, underscores only"); valid = false
        }
        if (fullname.isEmpty()) {
            view?.showFieldError("fullname", "Full name is required"); valid = false
        }
        if (!valid) return

        val userId = session.getUserId() ?: run {
            view?.showError("Session expired. Please log in again.")
            return
        }

        view?.showLoading()

        val updates = mapOf(
            "username" to username,
            "fullname" to fullname
        )

        SupabaseClient.db.updateUser(
            token   = session.getBearerToken(),
            userId  = "eq.$userId",
            updates = updates
        ).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                view?.hideLoading()
                if (response.isSuccessful || response.code() == 204) {
                    session.updateProfile(username, fullname)
                    view?.showSuccess("Profile updated successfully!")
                } else {
                    view?.showError(response.parseErrorMessage())
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    override fun onDestroy() { view = null }
}