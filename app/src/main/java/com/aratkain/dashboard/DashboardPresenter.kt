package com.aratkain.dashboard

import com.aratkain.AratKainApp
import com.aratkain.core.utils.SessionManager

class DashboardPresenter(
    private var view:    DashboardContract.View?,
    private val session: SessionManager,
    private val app:     AratKainApp
) : DashboardContract.Presenter {

    override fun onViewResumed() {
        view?.showLoading()

        // Load from session (already saved after login)
        val user = session.getCurrentUser()

        if (user == null) {
            view?.hideLoading()
            view?.showError("Session expired. Please log in again.")
            view?.navigateToLogin()
            return
        }

        // Sync app-level cache with session
        app.currentUser = user

        view?.hideLoading()
        view?.showUserInfo(user)
    }

    override fun onLogoutClicked() {
        view?.showLogoutConfirmation()
    }

    override fun onProfileClicked() {
        view?.navigateToProfile()
    }

    fun confirmLogout() {
        session.logout()
        app.currentUser = null
        view?.navigateToLogin()
    }

    override fun onDestroy() {
        view = null
    }
}