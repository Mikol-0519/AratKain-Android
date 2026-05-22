package com.aratkain.profile

import com.aratkain.core.model.UserData

// ════════════════════════════════════════════════════════════
// ProfileContract
// ════════════════════════════════════════════════════════════
interface ProfileContract {

    interface View {
        fun showUserInfo(user: UserData)
        fun navigateToUpdateProfile()
        fun navigateToChangePassword()
        fun navigateBack()
        fun navigateToDashboard()
        fun navigateToFavorites()
        fun showLogoutConfirmation()
        fun navigateToLogin()
        fun showError(message: String)
    }

    interface Presenter {
        fun onViewResumed()
        fun onEditProfileClicked()
        fun onChangePasswordClicked()
        fun onMapClicked()
        fun onBookmarksClicked()
        fun onLogoutClicked()
        fun confirmLogout()
        fun onBackClicked()
        fun onDestroy()
    }
}