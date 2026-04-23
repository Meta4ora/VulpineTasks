package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vulpinetasks.backend.*
import com.example.vulpinetasks.databinding.ActivityLoginBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        updateUI()
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener { login() }
        binding.registerButton.setOnClickListener { register() }

        binding.logoutButton.setOnClickListener {
            val userId = tokenManager.getUserId()

            tokenManager.logout()
            updateUI()

            toast("Выход выполнен")
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.guestButton.setOnClickListener {
            tokenManager.setGuestMode(true)

            if (tokenManager.getUserId() == null) {
                tokenManager.saveUserId("guest_${System.currentTimeMillis()}")
            }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun updateUI() {
        val token = tokenManager.getToken()

        if (token != null) {
            binding.loginCard.visibility = View.GONE
            binding.profileCard.visibility = View.VISIBLE

            binding.userEmail.text =
                tokenManager.getEmail() ?: "Unknown user"

        } else {
            binding.loginCard.visibility = View.VISIBLE
            binding.profileCard.visibility = View.GONE
        }
    }

    private fun login() {
        val email = binding.email.text.toString()
        val password = binding.password.text.toString()

        if (email.isBlank() || password.isBlank()) {
            toast("Введите email и пароль")
            return
        }

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.login(
                    AuthRequest(email, password)
                )

                tokenManager.saveToken(res.token)
                tokenManager.saveUserId(res.userId)
                tokenManager.saveEmail(email)
                tokenManager.setGuestMode(false)

                AppGraph.notesRepository.fetchFromServer(res.userId)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                toast("Ошибка входа")
            }
        }
    }

    private fun register() {
        val email = binding.email.text.toString()
        val password = binding.password.text.toString()

        lifecycleScope.launch {
            try {
                RetrofitClient.api.register(
                    AuthRequest(email, password)
                )

                toast("Регистрация успешна")

            } catch (e: Exception) {
                toast("Ошибка регистрации")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}