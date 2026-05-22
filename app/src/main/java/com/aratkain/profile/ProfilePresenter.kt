package com.aratkain.profile

import com.aratkain.core.utils.SessionManager

class ProfilePresenter(
    private var view:    ProfileContract.View?,
    private val session: SessionManager
) : ProfileContract.Presenter {

    override fun onViewResumed() {
        val user = session.getCurrentUser() ?: return
        view?.showUserInfo(user)
    }

    override fun onEditProfileClicked()      { view?.navigateToUpdateProfile()  }
    override fun onChangePasswordClicked()   { view?.navigateToChangePassword() }

    override fun onMapClicked() {
        view?.navigateToDashboard()
    }

    override fun onBookmarksClicked() {
        view?.navigateToFavorites()
    }

    override fun onLogoutClicked() {
        view?.showLogoutConfirmation()
    }

    override fun confirmLogout() {
        session.logout()
        view?.navigateToLogin()
    }

    override fun onBackClicked()             { view?.navigateBack()             }
    override fun onDestroy()                 { view = null                      }
}