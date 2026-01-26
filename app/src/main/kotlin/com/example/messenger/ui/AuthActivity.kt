package com.example.messenger.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.messenger.R
import com.example.messenger.databinding.ActivityAuthBinding
import com.example.messenger.firebase.FirebaseManager
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val firebaseManager = FirebaseManager()
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if already logged in
        if (firebaseManager.isLoggedIn()) {
            navigateToChat()
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.apply {
            // Toggle between login and register
            toggleButton.setOnClickListener {
                toggleMode()
            }

            // Action button (login/register)
            actionButton.setOnClickListener {
                if (isLoginMode) {
                    handleLogin()
                } else {
                    handleRegister()
                }
            }
        }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        binding.apply {
            if (isLoginMode) {
                titleText.text = getString(R.string.login)
                actionButton.text = getString(R.string.button_login)
                toggleText.text = getString(R.string.no_account)
                toggleButton.text = getString(R.string.button_register)
                usernameLayout.visibility = View.GONE
                passwordConfirmLayout.visibility = View.GONE
            } else {
                titleText.text = getString(R.string.register)
                actionButton.text = getString(R.string.button_register)
                toggleText.text = getString(R.string.have_account)
                toggleButton.text = getString(R.string.button_login)
                usernameLayout.visibility = View.VISIBLE
                passwordConfirmLayout.visibility = View.VISIBLE
            }
            errorText.visibility = View.GONE
        }
    }

    private fun handleLogin() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (!validateLoginInput(email, password)) return

        binding.actionButton.isEnabled = false
        lifecycleScope.launch {
            val result = firebaseManager.loginUser(email, password)
            result.onSuccess {
                navigateToChat()
            }
            result.onFailure { error ->
                showError(error.message ?: "Login failed")
                binding.actionButton.isEnabled = true
            }
        }
    }

    private fun handleRegister() {
        val email = binding.emailInput.text.toString().trim()
        val username = binding.usernameInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val passwordConfirm = binding.passwordConfirmInput.text.toString().trim()

        if (!validateRegisterInput(email, username, password, passwordConfirm)) return

        binding.actionButton.isEnabled = false
        lifecycleScope.launch {
            val result = firebaseManager.registerUser(email, password, username)
            result.onSuccess {
                showSuccess("Registration successful!")
                toggleMode()
                binding.actionButton.isEnabled = true
            }
            result.onFailure { error ->
                showError(error.message ?: "Registration failed")
                binding.actionButton.isEnabled = true
            }
        }
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                showError("Email is required")
                false
            }
            !email.contains("@") -> {
                showError(getString(R.string.invalid_email))
                false
            }
            password.isEmpty() -> {
                showError("Password is required")
                false
            }
            password.length < 6 -> {
                showError(getString(R.string.password_too_short))
                false
            }
            else -> true
        }
    }

    private fun validateRegisterInput(
        email: String,
        username: String,
        password: String,
        passwordConfirm: String
    ): Boolean {
        return when {
            email.isEmpty() -> {
                showError("Email is required")
                false
            }
            !email.contains("@") -> {
                showError(getString(R.string.invalid_email))
                false
            }
            username.isEmpty() -> {
                showError("Username is required")
                false
            }
            username.length < 3 -> {
                showError(getString(R.string.username_too_short))
                false
            }
            password.isEmpty() -> {
                showError("Password is required")
                false
            }
            password.length < 6 -> {
                showError(getString(R.string.password_too_short))
                false
            }
            password != passwordConfirm -> {
                showError(getString(R.string.passwords_not_match))
                false
            }
            else -> true
        }
    }

    private fun showError(message: String) {
        binding.apply {
            errorText.text = message
            errorText.visibility = View.VISIBLE
        }
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToChat() {
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }
}
