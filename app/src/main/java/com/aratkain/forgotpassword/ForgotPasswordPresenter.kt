package com.aratkain.forgotpassword

import com.aratkain.core.api.SupabaseClient
import com.aratkain.core.utils.isValidEmail
import com.aratkain.core.utils.toNetworkMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Single-step forgot password: send the recovery email via Supabase Auth.
 *
 * The previous implementation looked up the email in the `users` table with
 * the anon key (enumeration) and then set a new password through the
 * `reset_user_password` RPC, which let anyone take over any account. Now the
 * email is sent through POST /auth/v1/recover — 200 {} regardless of whether
 * the account exists — and the actual reset happens on the web app's
 * reset-password page that the email link opens.
 */
class ForgotPasswordPresenter(
    private var view: ForgotPasswordContract.View?
) : ForgotPasswordContract.Presenter {

    override fun onSendResetEmailClicked(email: String) {
        view?.clearErrors()

        // Validation in Presenter
        if (email.isEmpty()) { view?.showEmailError("Email is required"); return }
        if (!email.isValidEmail()) { view?.showEmailError("Enter a valid email address"); return }

        view?.showLoading()

        SupabaseClient.auth.recoverPassword(
            body = mapOf("email" to email)
        ).enqueue(object : Callback<Void> {

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                view?.hideLoading()
                if (response.isSuccessful) {
                    view?.showSuccess()
                } else {
                    view?.showError("Unable to send the reset email. Please try again.")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                view?.hideLoading()
                view?.showError(t.toNetworkMessage())
            }
        })
    }

    override fun onBackToLoginClicked() { view?.navigateToLogin() }
    override fun onDestroy()            { view = null }
}
