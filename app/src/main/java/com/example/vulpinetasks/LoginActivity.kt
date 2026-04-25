package com.example.vulpinetasks

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vulpinetasks.backend.*
import com.example.vulpinetasks.databinding.ActivityLoginBinding
import com.example.vulpinetasks.databinding.DialogRegisterBinding
import com.example.vulpinetasks.room.AppGraph
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenManager: TokenManager
    private var registerDialog: Dialog? = null

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
        binding.registerButton.setOnClickListener { showRegisterDialog() }

        binding.logoutButton.setOnClickListener {
            lifecycleScope.launch {
                val userUserId = tokenManager.getUserId()
                val guestUserId = tokenManager.getGuestUserId()

                // Копируем заметки пользователя гостю (старые гостевые удалятся)
                if (userUserId != null && guestUserId != null) {
                    AppGraph.notesRepository.copyUserNotesToGuest(userUserId, guestUserId)
                }

                tokenManager.logout()
                updateUI()

                toast("Вы вышли из аккаунта. Заметки сохранены локально.")
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.guestButton.setOnClickListener {
            lifecycleScope.launch {
                val userId = tokenManager.getUserId()
                val guestUserId = tokenManager.getGuestUserId()

                // Если был залогинен - копируем заметки гостю
                if (!tokenManager.isGuest() && userId != null) {
                    AppGraph.notesRepository.copyUserNotesToGuest(userId, guestUserId)
                }

                tokenManager.setGuestMode(true)
                tokenManager.saveUserId(guestUserId)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun showRegisterDialog() {
        val dialogBinding = DialogRegisterBinding.inflate(layoutInflater)

        registerDialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            setCancelable(true)
            window?.setBackgroundDrawableResource(android.R.color.transparent)

            // Настройка ширины диалога
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Обработчики кнопок
        dialogBinding.dialogCancelButton.setOnClickListener {
            registerDialog?.dismiss()
        }

        dialogBinding.dialogRegisterButton.setOnClickListener {
            val email = dialogBinding.dialogEmail.text.toString().trim()
            val password = dialogBinding.dialogPassword.text.toString().trim()
            val confirmPassword = dialogBinding.dialogConfirmPassword.text.toString().trim()

            // Валидация
            when {
                email.isBlank() -> {
                    toast("Введите email")
                    return@setOnClickListener
                }
                password.isBlank() -> {
                    toast("Введите пароль")
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    toast("Пароль должен содержать минимум 6 символов")
                    return@setOnClickListener
                }
                confirmPassword.isBlank() -> {
                    toast("Подтвердите пароль")
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    toast("Пароли не совпадают")
                    // Визуальная индикация ошибки
                    dialogBinding.dialogPasswordLayout.error = "Пароли не совпадают"
                    dialogBinding.dialogConfirmPasswordLayout.error = "Пароли не совпадают"
                    return@setOnClickListener
                }
            }

            // Сброс ошибок
            dialogBinding.dialogPasswordLayout.error = null
            dialogBinding.dialogConfirmPasswordLayout.error = null

            performRegistration(email, password, dialogBinding)
        }

        // Очистка ошибок при вводе
        dialogBinding.dialogPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) dialogBinding.dialogPasswordLayout.error = null
        }

        dialogBinding.dialogConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) dialogBinding.dialogConfirmPasswordLayout.error = null
        }

        registerDialog?.show()
    }

    private fun performRegistration(
        email: String,
        password: String,
        binding: DialogRegisterBinding
    ) {
        lifecycleScope.launch {
            try {
                // Показываем индикатор загрузки
                binding.dialogRegisterButton.isEnabled = false
                binding.dialogRegisterButton.text = "Регистрация..."

                val response = RetrofitClient.api.register(AuthRequest(email, password))

                toast("Регистрация успешна! Теперь войдите.")
                registerDialog?.dismiss()

                // Автоматически заполняем поля входа
                this@LoginActivity.binding.email.setText(email)
                this@LoginActivity.binding.password.setText(password)

            } catch (e: Exception) {
                e.printStackTrace()

                // Обработка ошибок
                val errorMessage = when {
                    e.message?.contains("already exists") == true ->
                        "Пользователь с таким email уже существует"
                    e.message?.contains("network") == true ->
                        "Ошибка сети. Проверьте подключение"
                    else -> "Ошибка регистрации: ${e.message}"
                }

                toast(errorMessage)

                // Возвращаем кнопку в обычное состояние
                binding.dialogRegisterButton.isEnabled = true
                binding.dialogRegisterButton.text = "Зарегистрироваться"
            }
        }
    }

    private fun updateUI() {
        val token = tokenManager.getToken()

        if (token != null) {
            binding.loginCard.visibility = View.GONE
            binding.profileCard.visibility = View.VISIBLE
            binding.userEmail.text = tokenManager.getEmail() ?: "Unknown user"
        } else {
            binding.loginCard.visibility = View.VISIBLE
            binding.profileCard.visibility = View.GONE
        }
    }

    private fun login() {
        val email = binding.email.text.toString().trim()
        val password = binding.password.text.toString().trim()

        if (email.isBlank() || password.isBlank()) {
            toast("Введите email и пароль")
            return
        }

        lifecycleScope.launch {
            try {
                val guestUserId = tokenManager.getGuestUserId()

                val res = RetrofitClient.api.login(
                    AuthRequest(email, password)
                )

                tokenManager.saveToken(res.token)
                tokenManager.saveUserId(res.userId)
                tokenManager.saveEmail(email)
                tokenManager.setGuestMode(false)

                // Мигрируем гостевые заметки (после миграции гостевые удалятся)
                AppGraph.notesRepository.migrateGuestNotesToUser(guestUserId, res.userId)

                // Загружаем с сервера
                AppGraph.notesRepository.fetchFromServer(res.userId)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                toast("Ошибка входа: ${e.message}")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        registerDialog?.dismiss()
    }
}