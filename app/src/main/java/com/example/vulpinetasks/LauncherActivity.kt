package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.vulpinetasks.backend.TokenManager

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = TokenManager(this).getToken()

        when {
            !token.isNullOrEmpty() -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        finish()
    }
}