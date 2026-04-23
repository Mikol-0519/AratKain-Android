package com.aratkain.updateprofile

// ════════════════════════════════════════════════════════════
// Contract
// ════════════════════════════════════════════════════════════
interface UpdateProfileContract {

    interface View {
        fun prefillData(username: String, fullname: String, email: String)
        fun showLoading()
        fun hideLoading()
        fun showFieldError(field: String, message: String)
        fun showError(message: String)
        fun showSuccess(message: String)
        fun clearErrors()
    }

    interface Presenter {
        fun onViewCreated()
        fun onSaveClicked(username: String, fullname: String)
        fun onDestroy()
    }
}