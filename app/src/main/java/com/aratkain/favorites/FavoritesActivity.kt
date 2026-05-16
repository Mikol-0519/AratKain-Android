package com.aratkain.favorites

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aratkain.core.model.EstablishmentResponse
import com.aratkain.core.utils.BookmarkManager
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.getApp
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.dashboard.DashboardActivity
import com.aratkain.dashboard.NearbyPlaceAdapter
import com.aratkain.dashboard.PlaceDetailBottomSheet
import com.aratkain.databinding.ActivityFavoritesBinding
import com.aratkain.login.LoginActivity
import com.aratkain.profile.ProfileActivity

class FavoritesActivity : AppCompatActivity(), FavoritesContract.View {

    private lateinit var binding:          ActivityFavoritesBinding
    private lateinit var presenter:        FavoritesPresenter
    private lateinit var favoritesAdapter: NearbyPlaceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = FavoritesPresenter(
            view            = this,
            session         = SessionManager(this),
            app             = getApp(),
            bookmarkManager = BookmarkManager(this)
        )

        setupRecyclerView()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()   // reloads list every time we enter the screen
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // ── Setup ──────────────────────────────────────────────────

    private fun setupRecyclerView() {
        favoritesAdapter = NearbyPlaceAdapter(
            bookmarkManager   = BookmarkManager(this),
            onPlaceClick      = { place -> presenter.onPlaceClicked(place) },
            onBookmarkChanged = { place, isSaved -> presenter.onBookmarkChanged(place, isSaved) }
        )
        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter        = favoritesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnNavMap.setOnClickListener     { presenter.onMapClicked() }
        binding.btnNavProfile.setOnClickListener { presenter.onProfileClicked() }
        binding.btnLogout.setOnClickListener     { presenter.onLogoutClicked() }
    }

    // ── FavoritesContract.View ─────────────────────────────────

    override fun showLoading()  { binding.progressBar.show() }
    override fun hideLoading()  { binding.progressBar.hide() }

    override fun showFavorites(places: List<EstablishmentResponse>) {
        favoritesAdapter.submitList(places)
    }

    override fun updateSavedCount(count: Int) {
        binding.tvSavedCount.text = if (count == 0) "No saved places" else "$count saved"
    }

    override fun showEmptyState() {
        binding.rvFavorites.hide()
        binding.layoutEmpty.show()
    }

    override fun hideEmptyState() {
        binding.layoutEmpty.hide()
        binding.rvFavorites.show()
    }

    override fun openPlaceDetail(place: EstablishmentResponse) {
        val tag = "fav_detail"
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        PlaceDetailBottomSheet.newInstance(place).show(supportFragmentManager, tag)
    }

    override fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ -> presenter.confirmLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    override fun navigateToProfile() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    override fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }
}