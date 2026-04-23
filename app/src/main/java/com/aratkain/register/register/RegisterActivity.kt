package com.aratkain.register.register

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.disable
import com.aratkain.core.utils.enable
import com.aratkain.core.utils.getApp
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.core.utils.value
import com.aratkain.dashboard.DashboardActivity
import com.aratkain.databinding.ActivityRegisterBinding
import com.aratkain.register.RegisterContract

class RegisterActivity : AppCompatActivity(), RegisterContract.View {

    private lateinit var binding:   ActivityRegisterBinding
    private lateinit var presenter: RegisterContract.Presenter
    private lateinit var session:   SessionManager

    private var registeredUser: UserData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session   = SessionManager(this)
        presenter = RegisterPresenter(this)

        // Interaction 1: Register button
        binding.btnRegister.setOnClickListener {
            presenter.onRegisterClicked(
                username        = binding.etUsername.value(),
                fullname        = binding.etFullname.value(),
                email           = binding.etEmail.value(),
                password        = binding.etPassword.value(),
                confirmPassword = binding.etConfirmPassword.value()
            )
        }

        // Interaction 2: Go to Dashboard after success
        binding.btnGoToDashboard.setOnClickListener {
            registeredUser?.let { presenter.onGoToDashboardClicked(it) }
        }

        // Interaction 3: Back to login
        binding.tvLogin.setOnClickListener { finish() }
    }

    // ── View interface ────────────────────────────────────────

    override fun showLoading() {
        binding.progressBar.show()
        binding.btnRegister.disable()
        binding.btnRegister.text = "Creating account…"
    }

    override fun hideLoading() {
        binding.progressBar.hide()
        binding.btnRegister.enable()
        binding.btnRegister.text = "Create Account"
    }

    override fun showFieldError(field: String, message: String) {
        when (field) {
            "username"        -> binding.tilUsername.error        = message
            "fullname"        -> binding.tilFullname.error        = message
            "email"           -> binding.tilEmail.error           = message
            "password"        -> binding.tilPassword.error        = message
            "confirmPassword" -> binding.tilConfirmPassword.error = message
        }
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.layoutError.show()
    }

    override fun clearErrors() {
        binding.tilUsername.error        = null
        binding.tilFullname.error        = null
        binding.tilEmail.error           = null
        binding.tilPassword.error        = null
        binding.tilConfirmPassword.error = null
        binding.layoutError.hide()
    }

    override fun showSuccess(user: UserData) {
        registeredUser = user
        binding.tvSuccessUsername.text = user.username
        binding.layoutForm.hide()
        binding.layoutSuccess.show()
    }

    override fun navigateToDashboard(user: UserData) {
        session.saveSession(user)
        getApp().currentUser = user
        startActivity(Intent(this, DashboardActivity::class.java))
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}