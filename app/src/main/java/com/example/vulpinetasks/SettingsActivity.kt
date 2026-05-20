package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.vulpinetasks.databinding.ActivitySettingsBinding
import com.example.vulpinetasks.utils.SettingsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
        if (settingsManager.isDarkMode()) {
            setTheme(R.style.Theme_VulpineTasks_Dark)
        } else {
            setTheme(R.style.Theme_VulpineTasks)
        }

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.preference_container, SettingsFragment())
            .commit()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Настройки"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var settingsManager: SettingsManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            settingsManager = SettingsManager(requireContext())

            setupPreferences()
            updateSummaries()
        }

        private fun updateSummaries() {
            val fontSizePref = findPreference<ListPreference>(SettingsManager.KEY_FONT_SIZE)
            fontSizePref?.summary = fontSizePref?.entry

            val defaultTitlePref = findPreference<EditTextPreference>(SettingsManager.KEY_DEFAULT_NOTE_TITLE)
            defaultTitlePref?.summary = settingsManager.getDefaultNoteTitle()
        }

        private fun setupPreferences() {
            // Тёмная тема
            val darkModePref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_DARK_MODE)
            darkModePref?.isChecked = settingsManager.isDarkMode()
            darkModePref?.setOnPreferenceChangeListener { _, newValue ->
                val isDark = newValue as Boolean
                settingsManager.setDarkMode(isDark)
                requireActivity().recreate()
                true
            }

            // Размер шрифта
            val fontSizePref = findPreference<ListPreference>(SettingsManager.KEY_FONT_SIZE)
            fontSizePref?.setValue(settingsManager.getFontSize())
            fontSizePref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setFontSize(newValue as String)
                fontSizePref?.summary = fontSizePref?.entry
                requireContext().sendBroadcast(Intent("FONT_SIZE_CHANGED"))
                true
            }

            // Нумерация строк
            val lineNumbersPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_LINE_NUMBERS)
            lineNumbersPref?.isChecked = settingsManager.isLineNumbersEnabled()
            lineNumbersPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setLineNumbersEnabled(newValue as Boolean)
                requireContext().sendBroadcast(Intent("LINE_NUMBERS_CHANGED"))
                true
            }

            // Перенос слов
            val wordWrapPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_WORD_WRAP)
            wordWrapPref?.isChecked = settingsManager.isWordWrapEnabled()
            wordWrapPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setWordWrapEnabled(newValue as Boolean)
                true
            }

            // Автосохранение
            val autoSavePref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_AUTO_SAVE)
            autoSavePref?.isChecked = settingsManager.isAutoSaveEnabled()
            autoSavePref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setAutoSaveEnabled(newValue as Boolean)
                true
            }

            // Автосинхронизация
            val autoSyncPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_AUTO_SYNC)
            autoSyncPref?.isChecked = settingsManager.isAutoSyncEnabled()
            autoSyncPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setAutoSyncEnabled(newValue as Boolean)
                true
            }

            // Заголовок по умолчанию
            val defaultTitlePref = findPreference<EditTextPreference>(SettingsManager.KEY_DEFAULT_NOTE_TITLE)
            defaultTitlePref?.text = settingsManager.getDefaultNoteTitle()
            defaultTitlePref?.setOnPreferenceChangeListener { _, newValue ->
                val newTitle = newValue as String
                settingsManager.setDefaultNoteTitle(newTitle)
                defaultTitlePref?.summary = newTitle
                true
            }

            // Синхронизация сейчас
            val syncNowPref = findPreference<Preference>("sync_now")
            syncNowPref?.setOnPreferenceClickListener {
                performManualSync()
                true
            }

            // О приложении
            val aboutPref = findPreference<Preference>("about")
            aboutPref?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }
        }

        private fun performManualSync() {
            Toast.makeText(requireContext(), "Синхронизация запущена...", Toast.LENGTH_SHORT).show()
        }

        private fun showAboutDialog() {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("О приложении")
                .setMessage("""
                    VulpineNotes
                    Версия 1.0.0
                    
                    Приложение для ведения заметок с поддержкой Markdown,
                    синхронизацией с облаком и богатым форматированием.
                    
                    © 2026 Vulpine Team
                """.trimIndent())
                .setPositiveButton("Закрыть", null)
                .show()
        }
    }
}