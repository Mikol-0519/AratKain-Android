package com.aratkain.dashboard

import com.aratkain.AratKainApp
import com.aratkain.core.model.NearbyRequest
import com.aratkain.core.network.EstablishmentApiClient
import com.aratkain.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardPresenter(
    private var view:    DashboardContract.View?,
    private val session: SessionManager,
    private val app:     AratKainApp
) : DashboardContract.Presenter {

    private val job   = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    // ── Existing methods ─────────────────────────────────────

    override fun onViewResumed() {
        view?.showLoading()
        val user = session.getCurrentUser()
        if (user == null) {
            view?.hideLoading()
            view?.showError("Session expired. Please log in again.")
            view?.navigateToLogin()
            return
        }
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

    // ── Map & nearby ─────────────────────────────────────────

    override fun onLocationReady(lat: Double, lng: Double) {
        android.util.Log.d("DASHBOARD", "Location dispatched: lat=$lat lng=$lng")
        view?.centerMapOn(lat, lng)
        fetchNearby(lat, lng)
    }

    private fun fetchNearby(lat: Double, lng: Double, radiusKm: Double = 10.0) {
        scope.launch {
            view?.showLoading()
            try {
                val places = withContext(Dispatchers.IO) {
                    EstablishmentApiClient.service.findNearby(
                        NearbyRequest(
                            latitude  = lat,
                            longitude = lng,
                            radiusKm  = radiusKm
                        )
                    )
                }
                view?.hideLoading()
                view?.updateNearbyCount(places.size)
                view?.addMapMarkers(places)
                view?.showNearbyPlaces(places)
                android.util.Log.d("DASHBOARD", "fetchNearby: ${places.size} places within ${radiusKm}km")
            } catch (e: Exception) {
                view?.hideLoading()
                android.util.Log.e("DASHBOARD", "fetchNearby FAILED: ${e::class.simpleName}: ${e.message}", e)
                view?.showMapError("Could not load nearby places: ${e.localizedMessage}")
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────

    override fun onDestroy() {
        job.cancel()
        view = null
    }
}