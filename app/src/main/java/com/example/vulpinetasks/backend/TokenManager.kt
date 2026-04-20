package com.example.vulpinetasks.backend

import android.content.Context

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString("userId", userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("userId", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}