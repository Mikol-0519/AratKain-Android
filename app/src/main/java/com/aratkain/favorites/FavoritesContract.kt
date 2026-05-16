package com.aratkain.favorites

import com.aratkain.core.model.EstablishmentResponse

interface FavoritesContract {

    interface View {
        // ── Loading ────────────────────────────────────────────
        fun showLoading()
        fun hideLoading()

        // ── List state ─────────────────────────────────────────
        /** Populate the RecyclerView with the current saved places. */
        fun showFavorites(places: List<EstablishmentResponse>)

        /** Update the "X saved" counter in the header. */
        fun updateSavedCount(count: Int)

        /** Show the empty-state illustration when there are no bookmarks. */
        fun showEmptyState()

        /** Hide the empty-state illustration when there is at least one bookmark. */
        fun hideEmptyState()

        // ── Detail sheet ───────────────────────────────────────
        /** Open the place-detail bottom sheet for the given establishment. */
        fun openPlaceDetail(place: EstablishmentResponse)

        // ── Errors ─────────────────────────────────────────────
        fun showError(message: String)

        // ── Navigation ─────────────────────────────────────────
        fun showLogoutConfirmation()
        fun navigateToLogin()
        fun navigateToProfile()
        fun navigateToDashboard()
    }

    interface Presenter {
        // ── Lifecycle ──────────────────────────────────────────
        /** Call from Activity.onResume — reloads the bookmark list. */
        fun onViewResumed()
        fun onDestroy()

        // ── User actions ───────────────────────────────────────
        /** User tapped a place row → open its detail sheet. */
        fun onPlaceClicked(place: EstablishmentResponse)

        /**
         * Bookmark icon was toggled on a list row.
         * The presenter removes the place if [isSaved] is now false,
         * then refreshes the list.
         */
        fun onBookmarkChanged(place: EstablishmentResponse, isSaved: Boolean)

        // ── Nav actions ────────────────────────────────────────
        fun onLogoutClicked()
        fun confirmLogout()
        fun onProfileClicked()
        fun onMapClicked()
    }
}