package com.example.vulpinetasks.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vulpinetasks.backend.TokenManager

class SettingsManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DEFAULT_NOTE_TITLE = "default_title"
        const val KEY_WORD_WRAP = "word_wrap"
        const val KEY_SHOW_WORD_COUNT = "show_word_count"
        const val KEY_SHOW_PREVIEW = "show_preview"
        const val KEY_COMPACT_VIEW = "compact_view"
        const val KEY_ANIMATION_SPEED = "animation_speed"
        const val KEY_CONFIRM_DELETE = "confirm_delete"
        const val KEY_VIBRATION = "vibration_on_long_press"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_SCROLL_TO_CURSOR = "scroll_to_cursor"
        const val KEY_AUTO_CAPITALIZE = "auto_capitalize"
        const val KEY_AUTO_CORRECT = "auto_correct"

        // Сохраненные методы из старой версии (для совместимости)
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_LINE_NUMBERS = "line_numbers"
        const val KEY_AUTO_SAVE = "auto_save"
        const val KEY_AUTO_SYNC = "auto_sync"

        // Событие изменения настроек
        const val ACTION_SETTINGS_CHANGED = "SETTINGS_CHANGED"
    }

    fun getDefaultNoteTitle(): String {
        return prefs.getString(KEY_DEFAULT_NOTE_TITLE, "Новая заметка") ?: "Новая заметка"
    }

    fun setDefaultNoteTitle(title: String) {
        prefs.edit().putString(KEY_DEFAULT_NOTE_TITLE, title).apply()
        broadcastChange()
    }

    fun isWordWrapEnabled(): Boolean {
        return prefs.getBoolean(KEY_WORD_WRAP, true)
    }

    fun setWordWrapEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WORD_WRAP, enabled).apply()
        broadcastChange()
    }

    fun isWordCountEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_WORD_COUNT, true)
    }

    fun setWordCountEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_WORD_COUNT, enabled).apply()
        broadcastChange()
    }

    fun isPreviewEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHOW_PREVIEW, true)
    }

    fun setPreviewEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_PREVIEW, enabled).apply()
        broadcastChange()
    }

    fun isCompactViewEnabled(): Boolean {
        return prefs.getBoolean(KEY_COMPACT_VIEW, false)
    }

    fun setCompactViewEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_COMPACT_VIEW, enabled).apply()
        broadcastChange()
    }

    fun getAnimationSpeed(): String {
        return prefs.getString(KEY_ANIMATION_SPEED, "normal") ?: "normal"
    }

    fun getAnimationDuration(): Long {
        return when (getAnimationSpeed()) {
            "fast" -> 150L
            "normal" -> 300L
            "slow" -> 500L
            "off" -> 0L
            else -> 300L
        }
    }

    fun setAnimationSpeed(speed: String) {
        prefs.edit().putString(KEY_ANIMATION_SPEED, speed).apply()
        broadcastChange()
    }

    fun isConfirmDeleteEnabled(): Boolean {
        return prefs.getBoolean(KEY_CONFIRM_DELETE, true)
    }

    fun setConfirmDeleteEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean {
        return prefs.getBoolean(KEY_VIBRATION, true)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun isKeepScreenOn(): Boolean {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun isScrollToCursorEnabled(): Boolean {
        return prefs.getBoolean(KEY_SCROLL_TO_CURSOR, true)
    }

    fun setScrollToCursorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCROLL_TO_CURSOR, enabled).apply()
    }

    fun isAutoCapitalizeEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CAPITALIZE, true)
    }

    fun setAutoCapitalizeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CAPITALIZE, enabled).apply()
    }

    fun isAutoCorrectEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CORRECT, false)
    }

    fun setAutoCorrectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CORRECT, enabled).apply()
    }

    // ========== МЕТОДЫ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ==========

    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        broadcastChange()
    }

    fun isLineNumbersEnabled(): Boolean {
        return prefs.getBoolean(KEY_LINE_NUMBERS, false)
    }

    fun setLineNumbersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LINE_NUMBERS, enabled).apply()
        broadcastChange()
    }

    fun isAutoSaveEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SAVE, true)
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SAVE, enabled).apply()
    }

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun getUserId(): String? {
        val tokenManager = TokenManager(context)
        return tokenManager.getUserId()
    }

    private fun broadcastChange() {
        val intent = Intent(ACTION_SETTINGS_CHANGED)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}