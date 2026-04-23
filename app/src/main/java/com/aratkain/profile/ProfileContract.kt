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
    }

    interface Presenter {
        fun onViewResumed()
        fun onEditProfileClicked()
        fun onChangePasswordClicked()
        fun onBackClicked()
        fun onDestroy()
    }
}