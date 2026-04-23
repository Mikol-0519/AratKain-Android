package com.aratkain.login

import com.aratkain.core.model.UserData

interface LoginContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showEmailError(message: String)
        fun showPasswordError(message: String)
        fun showError(message: String)
        fun clearErrors()
        fun clearInputs()
        fun navigateToDashboard(user: UserData)
    }

    interface Presenter {
        fun onLoginClicked(email: String, password: String)
        fun onClearClicked()
        fun onDestroy()
    }
}