package com.aratkain.dashboard

import com.aratkain.core.model.UserData
import com.aratkain.core.model.EstablishmentResponse

interface DashboardContract {

    interface View {
        // ── Existing ──────────────────────────────────────────
        fun showLoading()
        fun hideLoading()
        fun showUserInfo(user: UserData)
        fun showError(message: String)
        fun showLogoutConfirmation()
        fun navigateToLogin()
        fun navigateToProfile()

        // ── Map & nearby ──────────────────────────────────────
        /** Animate the map camera to the user's GPS position. */
        fun centerMapOn(lat: Double, lng: Double)

        /** Drop a pin on the map for each establishment. */
        fun addMapMarkers(places: List<EstablishmentResponse>)

        /** Push the list to the bottom-sheet RecyclerView. */
        fun showNearbyPlaces(places: List<EstablishmentResponse>)

        /** Update the "X spots" counter in the bottom-sheet header. */
        fun updateNearbyCount(count: Int)

        /** Show a non-blocking error message overlaid on the map. */
        fun showMapError(message: String)
    }

    interface Presenter {
        // ── Existing ──────────────────────────────────────────
        fun onViewResumed()
        fun onLogoutClicked()
        fun onProfileClicked()
        fun onDestroy()

        // ── Map & nearby ──────────────────────────────────────
        /** Called once the Activity has obtained a GPS fix. */
        fun onLocationReady(lat: Double, lng: Double)
    }
}