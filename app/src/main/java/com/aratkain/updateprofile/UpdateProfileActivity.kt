package com.aratkain.updateprofile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.disable
import com.aratkain.core.utils.enable
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.core.utils.value
import com.aratkain.databinding.ActivityUpdateprofileBinding

class UpdateProfileActivity : AppCompatActivity(), UpdateProfileContract.View {

    private lateinit var binding:   ActivityUpdateprofileBinding
    private lateinit var presenter: UpdateProfileContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Edit Profile"
            setDisplayHomeAsUpEnabled(true)
        }

        presenter = UpdateProfilePresenter(this, SessionManager(this))
        presenter.onViewCreated()

        // Interaction 1: Save button
        binding.btnSave.setOnClickListener {
            presenter.onSaveClicked(
                username = binding.etUsername.value(),
                fullname = binding.etFullname.value()
            )
        }

        // Interaction 2: Clear username error on type
        binding.etUsername.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tilUsername.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Interaction 3: Clear fullname error on type
        binding.etFullname.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tilFullname.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun prefillData(username: String, fullname: String, email: String) {
        binding.etUsername.setText(username)
        binding.etFullname.setText(fullname)
        binding.etEmail.setText(email)
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

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}