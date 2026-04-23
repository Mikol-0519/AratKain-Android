package com.aratkain.changepassword

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.disable
import com.aratkain.core.utils.enable
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.core.utils.value
import com.aratkain.databinding.ActivityChangepasswordBinding

class ChangePasswordActivity : AppCompatActivity(), ChangePasswordContract.View {

    private lateinit var binding:   ActivityChangepasswordBinding
    private lateinit var presenter: ChangePasswordContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangepasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Change Password"
            setDisplayHomeAsUpEnabled(true)
        }

        presenter = ChangePasswordPresenter(this, SessionManager(this))

        // Interaction 1: Update button
        binding.btnUpdate.setOnClickListener {
            presenter.onUpdateClicked(
                newPassword     = binding.etNewPassword.value(),
                confirmPassword = binding.etConfirmPassword.value()
            )
        }

        // Interaction 2: Clear error on new password type
        binding.etNewPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tilNewPassword.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Interaction 3: Clear error on confirm type
        binding.etConfirmPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tilConfirmPassword.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun showLoading() {
        binding.progressBar.show()
        binding.btnUpdate.disable()
        binding.btnUpdate.text = "Updating…"
    }

    override fun hideLoading() {
        binding.progressBar.hide()
        binding.btnUpdate.enable()
        binding.btnUpdate.text = "Update Password"
    }

    override fun showFieldError(field: String, message: String) {
        when (field) {
            "newPassword"     -> binding.tilNewPassword.error     = message
            "confirmPassword" -> binding.tilConfirmPassword.error = message
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

    override fun clearFields() {
        binding.etNewPassword.setText("")
        binding.etConfirmPassword.setText("")
    }

    override fun clearErrors() {
        binding.tilNewPassword.error     = null
        binding.tilConfirmPassword.error = null
        binding.layoutError.hide()
        binding.layoutSuccess.hide()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}