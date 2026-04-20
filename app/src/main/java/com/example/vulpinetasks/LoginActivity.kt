package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vulpinetasks.backend.*
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: com.example.vulpinetasks.databinding.ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = com.example.vulpinetasks.databinding.ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            login()
        }

        binding.registerButton.setOnClickListener {
            register()
        }
    }

    private fun login() {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.login(
                    AuthRequest(
                        binding.email.text.toString(),
                        binding.password.text.toString()
                    )
                )

                val tokenManager = TokenManager(this@LoginActivity)

                tokenManager.saveToken(res.token)
                tokenManager.saveUserId(res.userId)

                AppGraph.notesRepository.fetchNotesFromServer(res.userId)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun register() {
        lifecycleScope.launch {
            try {
                RetrofitClient.api.register(
                    AuthRequest(
                        binding.email.text.toString(),
                        binding.password.text.toString()
                    )
                )

                Toast.makeText(this@LoginActivity, "Registered", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Register error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}