package com.aratkain.updateprofile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.disable
import com.aratkain.core.utils.enable
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.core.utils.value
import com.aratkain.databinding.ActivityUpdateprofileBinding
import com.bumptech.glide.Glide

class UpdateProfileActivity : AppCompatActivity(), UpdateProfileContract.View {

    private lateinit var binding:   ActivityUpdateprofileBinding
    private lateinit var presenter: UpdateProfileContract.Presenter

    // ── Image picker ──────────────────────────────────────────────────────────

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { presenter.onPhotoSelected(it) }
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchImagePicker()
            else Toast.makeText(this, "Storage permission is required to change your photo.", Toast.LENGTH_SHORT).show()
        }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Edit Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        presenter = UpdateProfilePresenter(this, SessionManager(this), this)
        presenter.onViewCreated()

        // Save profile fields
        binding.btnSave.setOnClickListener {
            presenter.onSaveClicked(
                username = binding.etUsername.value(),
                fullname = binding.etFullname.value()
            )
        }

        // Change photo
        binding.ibChangePhoto.setOnClickListener {
            presenter.onChangePhotoClicked()
        }

        // Clear field errors on type
        binding.etUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { binding.tilUsername.error = null }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etFullname.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { binding.tilFullname.error = null }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    // ── UpdateProfileContract.View ────────────────────────────────────────────

    override fun prefillData(username: String, fullname: String, email: String, photoUrl: String?) {
        binding.etUsername.setText(username)
        binding.etFullname.setText(fullname)
        binding.etEmail.setText(email)

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(binding.ivAvatar)
            binding.ivAvatar.show()
            binding.tvInitials.hide()
        } else {
            // Show initials fallback
            val initials = if (username.isNotEmpty()) username.take(2) else "?"
            binding.tvInitials.text = initials.uppercase()
            binding.ivAvatar.hide()
            binding.tvInitials.show()
        }
    }

    override fun showLoading() {
        binding.progressBar.show()
        binding.btnSave.disable()
        binding.btnSave.text = "Saving…"
    }

    override fun hideLoading() {
        binding.progressBar.hide()
        binding.btnSave.enable()
        binding.btnSave.text = "Save Changes"
    }

    override fun showFieldError(field: String, message: String) {
        when (field) {
            "username" -> binding.tilUsername.error = message
            "fullname" -> binding.tilFullname.error = message
        }
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.layoutError.show()
        binding.layoutSuccess.hide()
    }

    override fun showSuccess(message: String) {
        binding.tvSuccess.text = message
        binding.layoutSuccess.show()
        binding.layoutError.hide()
    }

    override fun clearErrors() {
        binding.tilUsername.error = null
        binding.tilFullname.error = null
        binding.layoutError.hide()
        binding.layoutSuccess.hide()
    }

    override fun openImagePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                launchImagePicker()
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Please allow storage access to change your profile photo.", Toast.LENGTH_LONG).show()
                permissionLauncher.launch(permission)
            }
            else -> permissionLauncher.launch(permission)
        }
    }

    override fun showPhotoUploading() {
        binding.pbPhotoUpload.show()
        binding.ibChangePhoto.isEnabled = false
    }

    override fun hidePhotoUploading() {
        binding.pbPhotoUpload.hide()
        binding.ibChangePhoto.isEnabled = true
    }

    override fun showUpdatedPhoto(photoUrl: String) {
        Glide.with(this).load(photoUrl).circleCrop().into(binding.ivAvatar)
        binding.ivAvatar.show()
        binding.tvInitials.hide()
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun launchImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
}