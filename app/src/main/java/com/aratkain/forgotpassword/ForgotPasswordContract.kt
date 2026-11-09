package com.aratkain.forgotpassword

interface ForgotPasswordContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showEmailError(message: String)
        fun showError(message: String)
        fun clearErrors()
        fun showSuccess()
        fun navigateToLogin()
    }

    interface Presenter {
        fun onSendResetEmailClicked(email: String)
        fun onBackToLoginClicked()
        fun onDestroy()
    }
}
