package com.aratkain.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.databinding.ActivityProfileBinding
import com.aratkain.updateprofile.UpdateProfileActivity
import com.aratkain.changepassword.ChangePasswordActivity
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

        // Interaction 1: Edit Profile
        binding.btnUpdateProfile.setOnClickListener {
            presenter.onEditProfileClicked()
        }

        // Interaction 2: Change Password
        binding.btnChangePassword.setOnClickListener {
            presenter.onChangePasswordClicked()
        }

        // Interaction 3: Back (toolbar)
        // handled by onSupportNavigateUp
    }

    override fun onResume() {
        super.onResume()
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

    override fun navigateToUpdateProfile()  {
        startActivity(Intent(this, UpdateProfileActivity::class.java))
    }

    override fun navigateToChangePassword() {
        startActivity(Intent(this, ChangePasswordActivity::class.java))
    }

    override fun navigateBack() { finish() }

    override fun onSupportNavigateUp(): Boolean {
        presenter.onBackClicked(); return true
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}