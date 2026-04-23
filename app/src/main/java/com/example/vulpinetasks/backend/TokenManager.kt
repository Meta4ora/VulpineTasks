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

    fun saveEmail(email: String) {
        prefs.edit().putString("email", email).apply()
    }

    fun getEmail(): String? {
        return prefs.getString("email", null)
    }

    fun setGuestMode(isGuest: Boolean) {
        prefs.edit().putBoolean("guest", isGuest).apply()
    }

    fun isGuest(): Boolean {
        return prefs.getBoolean("guest", false)
    }

    fun logout() {
        val userId = getUserId()
        val email = getEmail()

        prefs.edit().clear().apply()

        if (userId != null) saveUserId(userId)
        if (email != null) saveEmail(email)

        setGuestMode(true)
    }
}