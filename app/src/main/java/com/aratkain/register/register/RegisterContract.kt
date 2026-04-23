package com.aratkain.register

import com.aratkain.core.model.UserData

interface RegisterContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showFieldError(field: String, message: String)
        fun showError(message: String)
        fun clearErrors()
        fun showSuccess(user: UserData)
        fun navigateToDashboard(user: UserData)
    }

    interface Presenter {
        fun onRegisterClicked(
            username:        String,
            fullname:        String,
            email:           String,
            password:        String,
            confirmPassword: String
        )
        fun onGoToDashboardClicked(user: UserData)
        fun onDestroy()
    }
}