package com.aratkain.dashboard

import com.aratkain.core.model.UserData

// ── Contract ──────────────────────────────────────────────────
interface DashboardContract {

    interface View {
        fun showLoading()
        fun hideLoading()
        fun showUserInfo(user: UserData)
        fun showError(message: String)
        fun showLogoutConfirmation()
        fun navigateToLogin()
        fun navigateToProfile()
    }

    interface Presenter {
        fun onViewResumed()
        fun onLogoutClicked()
        fun onProfileClicked()
        fun onDestroy()
    }
}