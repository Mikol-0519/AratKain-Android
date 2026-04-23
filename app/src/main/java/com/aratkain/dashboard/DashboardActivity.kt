package com.aratkain.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.getApp
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.databinding.ActivityDashboardBinding
import com.aratkain.login.LoginActivity
import com.aratkain.profile.ProfileActivity
import com.bumptech.glide.Glide

class DashboardActivity : AppCompatActivity(), DashboardContract.View {

    private lateinit var binding:   ActivityDashboardBinding
    private lateinit var presenter: DashboardPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        presenter = DashboardPresenter(this, SessionManager(this), getApp())

        // Interaction 1: Profile click
        binding.btnViewProfile.setOnClickListener {
            presenter.onProfileClicked()
        }
        binding.cardWelcome.setOnClickListener {
            presenter.onProfileClicked()
        }

        // Interaction 3: Logout
        binding.btnLogout.setOnClickListener {
            presenter.onLogoutClicked()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    // ── View interface ────────────────────────────────────────

    override fun showLoading() {
        binding.progressBar.show()
    }

    override fun hideLoading() {
        binding.progressBar.hide()
    }

    override fun showUserInfo(user: UserData) {
        binding.tvWelcome.text  = "Welcome back, ${user.username}! 👋"
        binding.tvFullname.text = user.fullname.ifEmpty { user.username }
        binding.tvUsername.text = "@${user.username}"
        binding.tvEmail.text    = user.email

        val initials = if (user.fullname.isNotEmpty())
            user.fullname.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        else user.username.take(2)

        binding.tvInitials.text = initials.uppercase()

        if (!user.photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .into(binding.ivAvatar)
            binding.ivAvatar.show()
            binding.tvInitials.hide()
        } else {
            binding.ivAvatar.hide()
            binding.tvInitials.show()
        }
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.layoutError.show()
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

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
