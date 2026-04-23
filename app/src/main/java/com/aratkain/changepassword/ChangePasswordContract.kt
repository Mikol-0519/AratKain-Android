package com.aratkain.changepassword

interface ChangePasswordContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showFieldError(field: String, message: String)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun clearFields()
        fun clearErrors()
    }

    interface Presenter {
        fun onUpdateClicked(newPassword: String, confirmPassword: String)
        fun onDestroy()
    }
}