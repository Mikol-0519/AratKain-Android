package com.aratkain.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.R
import com.aratkain.core.model.UserData
import com.aratkain.core.utils.SessionManager
import com.aratkain.core.utils.disable
import com.aratkain.core.utils.enable
import com.aratkain.core.utils.getApp
import com.aratkain.core.utils.hide
import com.aratkain.core.utils.show
import com.aratkain.core.utils.value
import com.aratkain.dashboard.DashboardActivity
import com.aratkain.databinding.ActivityLoginBinding
import com.aratkain.forgotpassword.ForgotPasswordActivity
import com.aratkain.register.register.RegisterActivity

class LoginActivity : AppCompatActivity(), LoginContract.View {

    private lateinit var binding:   ActivityLoginBinding
    private lateinit var presenter: LoginContract.Presenter
    private lateinit var session:   SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session   = SessionManager(this)
        presenter = LoginPresenter(this)

        // Interaction 1: Login button click
        binding.btnLogin.setOnClickListener {
            presenter.onLoginClicked(
                binding.etEmail.value(),
                binding.etPassword.value()
            )
        }

        // Interaction 2: Clear button
        binding.btnClear.setOnClickListener {
            presenter.onClearClicked()
        }

        // Interaction 3: Text change clears field errors
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tilEmail.error = null
                binding.layoutError.hide()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.tilPassword.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Navigate to Register
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Navigate to Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    // ── View interface ────────────────────────────────────────

    override fun showLoading() {
        binding.progressBar.show()
        binding.btnLogin.disable()
        binding.btnLogin.text = "Logging in…"
    }

    override fun hideLoading() {
        binding.progressBar.hide()
        binding.btnLogin.enable()
        binding.btnLogin.text = "Log In"
    }

    override fun showEmailError(message: String) {
        binding.tilEmail.error = message
    }

    override fun showPasswordError(message: String) {
        binding.tilPassword.error = message
    }

    override fun showError(message: String) {
        binding.tvError.text = message
        binding.layoutError.show()
    }

    override fun clearErrors() {
        binding.tilEmail.error    = null
        binding.tilPassword.error = null
        binding.layoutError.hide()
    }

    override fun clearInputs() {
        binding.etEmail.setText("")
        binding.etPassword.setText("")
        clearErrors()
    }

    override fun navigateToDashboard(user: UserData) {
        // Save session
        session.saveSession(user)
        // Save to app-level cache
        getApp().currentUser = user

        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}