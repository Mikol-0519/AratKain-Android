package com.aratkain.forgotpassword

interface ForgotPasswordContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showEmailError(message: String)
        fun showFieldError(field: String, message: String)
        fun showError(message: String)
        fun clearErrors()
        fun goToStep2(email: String)
        fun showSuccess()
        fun navigateToLogin()
    }

    interface Presenter {
        fun onFindAccountClicked(email: String)
        fun onResetPasswordClicked(email: String, newPassword: String, confirmPassword: String)
        fun onBackToLoginClicked()
        fun onDestroy()
    }
}