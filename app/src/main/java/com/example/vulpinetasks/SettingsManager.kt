package com.example.vulpinetasks.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.vulpinetasks.R

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_LINE_NUMBERS = "line_numbers"
        const val KEY_WORD_WRAP = "word_wrap"
        const val KEY_AUTO_SAVE = "auto_save"
        const val KEY_AUTO_SYNC = "auto_sync"
        const val KEY_DEFAULT_NOTE_TITLE = "default_note_title"

        const val FONT_SIZE_SMALL = "small"
        const val FONT_SIZE_MEDIUM = "medium"
        const val FONT_SIZE_LARGE = "large"
        const val FONT_SIZE_XLARGE = "xlarge"
    }

    // Тёмная тема
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
        applyTheme(enabled)
    }

    fun applySavedTheme() {
        applyTheme(isDarkMode())
    }

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    // Размер шрифта
    fun getFontSize(): String = prefs.getString(KEY_FONT_SIZE, FONT_SIZE_MEDIUM) ?: FONT_SIZE_MEDIUM
    fun setFontSize(size: String) {
        prefs.edit { putString(KEY_FONT_SIZE, size) }
    }

    fun getFontSizeSp(): Float {
        return when (getFontSize()) {
            FONT_SIZE_SMALL -> 14f
            FONT_SIZE_MEDIUM -> 16f
            FONT_SIZE_LARGE -> 20f
            FONT_SIZE_XLARGE -> 24f
            else -> 16f
        }
    }

    // Нумерация строк
    fun isLineNumbersEnabled(): Boolean = prefs.getBoolean(KEY_LINE_NUMBERS, false)
    fun setLineNumbersEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LINE_NUMBERS, enabled) }
    }

    // Перенос слов
    fun isWordWrapEnabled(): Boolean = prefs.getBoolean(KEY_WORD_WRAP, true)
    fun setWordWrapEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_WORD_WRAP, enabled) }
    }

    // Автосохранение
    fun isAutoSaveEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SAVE, true)
    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SAVE, enabled) }
    }

    // Автосинхронизация
    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SYNC, true)
    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SYNC, enabled) }
    }

    // Заголовок по умолчанию
    fun getDefaultNoteTitle(): String = prefs.getString(KEY_DEFAULT_NOTE_TITLE, "Новая заметка") ?: "Новая заметка"
    fun setDefaultNoteTitle(title: String) {
        prefs.edit { putString(KEY_DEFAULT_NOTE_TITLE, title) }
    }

    // Сброс всех настроек
    fun resetAllSettings() {
        prefs.edit().clear().apply()
        applyTheme(false)
    }
}