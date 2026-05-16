package com.example.vulpinetasks.backend

import android.content.Context
import android.content.SharedPreferences

class TokenManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_GUEST_USER_ID = "guest_user_id"
        private const val KEY_LAST_LOGGED_USER_ID = "last_logged_user_id"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        // Сохраняем как последнего залогиненного пользователя
        if (!isGuest()) {
            prefs.edit().putString(KEY_LAST_LOGGED_USER_ID, userId).apply()
        }
    }

    fun getUserId(): String? {
        if (isGuest()) {
            return getGuestUserId()
        }
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getLastLoggedUserId(): String? {
        return prefs.getString(KEY_LAST_LOGGED_USER_ID, null)
    }

    fun saveEmail(email: String) {
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun setGuestMode(isGuest: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply()
    }

    fun isGuest(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, true)
    }

    fun getGuestUserId(): String {
        var guestId = prefs.getString(KEY_GUEST_USER_ID, null)
        if (guestId == null) {
            guestId = "guest_${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_GUEST_USER_ID, guestId).apply()
        }
        return guestId
    }

    fun logout() {
        val guestId = getGuestUserId()
        prefs.edit().clear().apply()
        prefs.edit().putString(KEY_GUEST_USER_ID, guestId).apply()
        setGuestMode(true)
    }
}