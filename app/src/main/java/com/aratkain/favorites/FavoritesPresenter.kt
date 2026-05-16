package com.aratkain.favorites

import android.util.Log
import com.aratkain.AratKainApp
import com.aratkain.core.model.EstablishmentResponse
import com.aratkain.core.utils.BookmarkManager
import com.aratkain.core.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesPresenter(
    private var view:            FavoritesContract.View?,
    private val session:         SessionManager,
    private val app:             AratKainApp,
    private val bookmarkManager: BookmarkManager
) : FavoritesContract.Presenter {

    private val job   = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    // ── Lifecycle ──────────────────────────────────────────────

    override fun onViewResumed() {
        loadBookmarks()
    }

    override fun onDestroy() {
        job.cancel()
        view = null
    }

    // ── User actions ───────────────────────────────────────────

    override fun onPlaceClicked(place: EstablishmentResponse) {
        view?.openPlaceDetail(place)
    }

    override fun onBookmarkChanged(place: EstablishmentResponse, isSaved: Boolean) {
        // If the user un-bookmarked from the list row, remove it and refresh.
        // If somehow re-bookmarked (shouldn't happen here, but guard anyway), just refresh.
        Log.d("FAVORITES", "Bookmark changed: ${place.name} → isSaved=$isSaved")
        loadBookmarks()
    }

    // ── Navigation ─────────────────────────────────────────────

    override fun onLogoutClicked() {
        view?.showLogoutConfirmation()
    }

    override fun confirmLogout() {
        session.logout()
        app.currentUser = null
        view?.navigateToLogin()
    }

    override fun onProfileClicked() {
        view?.navigateToProfile()
    }

    override fun onMapClicked() {
        view?.navigateToDashboard()
    }

    // ── Private helpers ────────────────────────────────────────

    /**
     * Reads bookmarks off the IO dispatcher (SharedPreferences read is fast but
     * keeping it off Main is consistent with the rest of the app pattern).
     */
    private fun loadBookmarks() {
        scope.launch {
            view?.showLoading()
            try {
                val places: List<EstablishmentResponse> = withContext(Dispatchers.IO) {
                    bookmarkManager.getAll()
                }
                view?.hideLoading()
                view?.showFavorites(places)
                view?.updateSavedCount(places.size)

                if (places.isEmpty()) {
                    view?.showEmptyState()
                } else {
                    view?.hideEmptyState()
                }

                Log.d("FAVORITES", "Loaded ${places.size} bookmarks")
            } catch (e: Exception) {
                view?.hideLoading()
                Log.e("FAVORITES", "loadBookmarks FAILED: ${e.message}", e)
                view?.showError("Could not load saved places: ${e.localizedMessage}")
            }
        }
    }
}