package com.aratkain.forgotpassword

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.utils.disable
import com.aratkain.core.utils.enable
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.core.utils.value
import com.aratkain.databinding.ActivityForgotpasswordBinding
import com.aratkain.login.LoginActivity

class ForgotPasswordActivity : AppCompatActivity(), ForgotPasswordContract.View {

    private lateinit var binding:       ActivityForgotpasswordBinding
    private lateinit var presenter:     ForgotPasswordContract.Presenter
    private var          verifiedEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotpasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Forgot Password"
            setDisplayHomeAsUpEnabled(true)
        }

        presenter = ForgotPasswordPresenter(this)

        // Interaction 1: Find account button (Step 1)
        binding.btnNext.setOnClickListener {
            if (binding.layoutStep1.visibility == android.view.View.VISIBLE) {
                presenter.onFindAccountClicked(binding.etEmail.value())
            } else {
                presenter.onResetPasswordClicked(
                    email           = verifiedEmail,
                    newPassword     = binding.etNewPassword.value(),
                    confirmPassword = binding.etConfirmPassword.value()
                )
            }
        }

        // Interaction 2: Back to login
        binding.tvBack.setOnClickListener {
            presenter.onBackToLoginClicked()
        }

        // Interaction 3: Clear email error on type
        binding.etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.tilEmail.error = null
                binding.layoutError.hide()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun showLoading() {
        binding.progressBar.show()
        binding.btnNext.disable()
    }

    override fun hideLoading() {
        binding.progressBar.hide()
        binding.btnNext.enable()
    }

    override fun showEmailError(message: String) {
        binding.tilEmail.error = message
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
    }

    override fun clearErrors() {
        binding.tilEmail.error            = null
        binding.tilNewPassword.error      = null
        binding.tilConfirmPassword.error  = null
        binding.layoutError.hide()
    }

    override fun goToStep2(email: String) {
        verifiedEmail = email
        // Update UI to Step 2
        binding.layoutStep1.hide()
        binding.layoutStep2.show()
        binding.layoutSuccess.hide()
        binding.tvStepIndicator.text = "Step 2 of 2 — Set new password for $email"
        binding.btnNext.text = "Update Password"
        binding.step1Bar.setBackgroundColor(getColor(com.aratkain.R.color.accent))
        binding.step2Bar.setBackgroundColor(getColor(com.aratkain.R.color.accent))
    }

    override fun showSuccess() {
        binding.layoutStep1.hide()
        binding.layoutStep2.hide()
        binding.layoutSuccess.show()
        binding.btnNext.hide()
        binding.btnGoToLogin.setOnClickListener {
            presenter.onBackToLoginClicked()
        }
    }

    override fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}