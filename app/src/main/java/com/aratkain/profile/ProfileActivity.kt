package com.aratkain.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.databinding.ActivityProfileBinding
import com.aratkain.updateprofile.UpdateProfileActivity
import com.aratkain.changepassword.ChangePasswordActivity
import com.aratkain.favorites.FavoritesActivity
import com.aratkain.dashboard.DashboardActivity
import com.aratkain.login.LoginActivity
import com.bumptech.glide.Glide

class ProfileActivity : AppCompatActivity(), ProfileContract.View {

    private lateinit var binding:   ActivityProfileBinding
    private lateinit var presenter: ProfileContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "My Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        presenter = ProfilePresenter(this, SessionManager(this))

        binding.btnUpdateProfile.setOnClickListener  { presenter.onEditProfileClicked()    }
        binding.btnChangePassword.setOnClickListener { presenter.onChangePasswordClicked() }

        // ── Sidebar Navigation ──────────────────────────────────
        setupSidebarNavigation()
    }

    private fun setupSidebarNavigation() {
        binding.btnNavMap.setOnClickListener     { presenter.onMapClicked() }
        binding.btnNavBookmarks.setOnClickListener { presenter.onBookmarksClicked() }
        binding.btnLogout.setOnClickListener     { presenter.onLogoutClicked() }
    }

    override fun onResume() {
        super.onResume()
        // Re-load on resume so photo/name changes from UpdateProfileActivity are reflected
        presenter.onViewResumed()
    }

    override fun showUserInfo(user: UserData) {
        binding.tvFullname.text = user.fullname.ifEmpty { user.username }
        binding.tvUsername.text = "@${user.username}"
        binding.tvEmail.text    = user.email

        val initials = if (user.fullname.isNotEmpty())
            user.fullname.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        else user.username.take(2)
        binding.tvInitials.text = initials.uppercase()

        if (!user.photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(user.photoUrl).circleCrop().into(binding.ivAvatar)
            binding.ivAvatar.show()
            binding.tvInitials.hide()
        } else {
            binding.ivAvatar.hide()
            binding.tvInitials.show()
        }
    }

    override fun navigateToUpdateProfile()  { startActivity(Intent(this, UpdateProfileActivity::class.java)) }
    override fun navigateToChangePassword() { startActivity(Intent(this, ChangePasswordActivity::class.java)) }
    override fun navigateBack()             { finish() }

    override fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    override fun navigateToFavorites() {
        startActivity(Intent(this, FavoritesActivity::class.java))
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

    override fun showError(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean { presenter.onBackClicked(); return true }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}