package com.example.vulpinetasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.vulpinetasks.databinding.ActivitySettingsBinding
import com.example.vulpinetasks.room.AppGraph
import com.example.vulpinetasks.utils.SettingsManager
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = SettingsManager(this)
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
        private lateinit var vibrator: Vibrator

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            settingsManager = SettingsManager(requireContext())

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(VibratorManager::class.java)
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Vibrator::class.java)
            }

            setupPreferences()
            updateSummaries()
        }

        private fun updateSummaries() {
            val defaultTitlePref = findPreference<EditTextPreference>(SettingsManager.KEY_DEFAULT_NOTE_TITLE)
            defaultTitlePref?.summary = settingsManager.getDefaultNoteTitle()

            val animationSpeedPref = findPreference<ListPreference>(SettingsManager.KEY_ANIMATION_SPEED)
            animationSpeedPref?.summary = animationSpeedPref?.entry
        }

        private fun vibrate() {
            if (settingsManager.isVibrationEnabled()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                } catch (e: Exception) {
                    // Игнорируем ошибки вибрации
                }
            }
        }

        private fun setupPreferences() {
            // Заголовок по умолчанию
            val defaultTitlePref = findPreference<EditTextPreference>(SettingsManager.KEY_DEFAULT_NOTE_TITLE)
            defaultTitlePref?.text = settingsManager.getDefaultNoteTitle()
            defaultTitlePref?.setOnPreferenceChangeListener { _, newValue ->
                val newTitle = newValue as String
                settingsManager.setDefaultNoteTitle(newTitle)
                defaultTitlePref?.summary = newTitle
                vibrate()
                true
            }

            // Перенос слов
            val wordWrapPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_WORD_WRAP)
            wordWrapPref?.isChecked = settingsManager.isWordWrapEnabled()
            wordWrapPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setWordWrapEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Счётчик слов
            val wordCountPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_SHOW_WORD_COUNT)
            wordCountPref?.isChecked = settingsManager.isWordCountEnabled()
            wordCountPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setWordCountEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Предпросмотр
            val previewPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_SHOW_PREVIEW)
            previewPref?.isChecked = settingsManager.isPreviewEnabled()
            previewPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setPreviewEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Компактный вид
            val compactViewPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_COMPACT_VIEW)
            compactViewPref?.isChecked = settingsManager.isCompactViewEnabled()
            compactViewPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setCompactViewEnabled(newValue as Boolean)
                vibrate()
                Toast.makeText(requireContext(), "Изменения применятся при перезапуске", Toast.LENGTH_SHORT).show()
                true
            }

            // Скорость анимации
            val animationSpeedPref = findPreference<ListPreference>(SettingsManager.KEY_ANIMATION_SPEED)
            animationSpeedPref?.value = settingsManager.getAnimationSpeed()
            animationSpeedPref?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as String
                settingsManager.setAnimationSpeed(value)
                animationSpeedPref.summary = animationSpeedPref.entry
                vibrate()
                true
            }

            // Подтверждение удаления
            val confirmDeletePref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_CONFIRM_DELETE)
            confirmDeletePref?.isChecked = settingsManager.isConfirmDeleteEnabled()
            confirmDeletePref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setConfirmDeleteEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Вибрация
            val vibrationPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_VIBRATION)
            vibrationPref?.isChecked = settingsManager.isVibrationEnabled()
            vibrationPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setVibrationEnabled(newValue as Boolean)
                true
            }

            // Не выключать экран
            val keepScreenOnPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_KEEP_SCREEN_ON)
            keepScreenOnPref?.isChecked = settingsManager.isKeepScreenOn()
            keepScreenOnPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setKeepScreenOn(newValue as Boolean)
                vibrate()
                true
            }

            // Прокрутка к курсору
            val scrollToCursorPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_SCROLL_TO_CURSOR)
            scrollToCursorPref?.isChecked = settingsManager.isScrollToCursorEnabled()
            scrollToCursorPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setScrollToCursorEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Автозаглавная буква
            val autoCapitalizePref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_AUTO_CAPITALIZE)
            autoCapitalizePref?.isChecked = settingsManager.isAutoCapitalizeEnabled()
            autoCapitalizePref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setAutoCapitalizeEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Автоисправление
            val autoCorrectPref = findPreference<SwitchPreferenceCompat>(SettingsManager.KEY_AUTO_CORRECT)
            autoCorrectPref?.isChecked = settingsManager.isAutoCorrectEnabled()
            autoCorrectPref?.setOnPreferenceChangeListener { _, newValue ->
                settingsManager.setAutoCorrectEnabled(newValue as Boolean)
                vibrate()
                true
            }

            // Статистика
            val statsPref = findPreference<Preference>("stats_info")
            statsPref?.setOnPreferenceClickListener {
                showStatsDialog()
                true
            }

            // О приложении
            val aboutPref = findPreference<Preference>("about")
            aboutPref?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }
        }

        private fun showStatsDialog() {
            lifecycleScope.launch {
                try {
                    val userId = settingsManager.getUserId() ?: return@launch
                    val notes = AppGraph.notesRepository.getAllNotes(userId)
                    var totalWords = 0
                    var totalChars = 0
                    var taskCount = 0
                    var noteCount = 0

                    notes.forEach { note ->
                        if (note.type == "task") {
                            taskCount++
                        } else {
                            noteCount++
                            totalWords += note.getWordCount()
                            totalChars += note.getCharacterCount()
                        }
                    }

                    AlertDialog.Builder(requireContext())
                        .setTitle("Статистика")
                        .setMessage("""
                            📊 Всего заметок: ${notes.size}
                            📝 Обычных заметок: $noteCount
                            ✅ Задач: $taskCount
                            
                            📖 Всего слов: $totalWords
                            🔤 Всего символов: $totalChars
                            
                            💾 В среднем: ${if (noteCount > 0) totalWords / noteCount else 0} слов на заметку
                        """.trimIndent())
                        .setPositiveButton("Закрыть", null)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun showAboutDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle("О приложении")
                .setMessage("""
                    VulpineTasks
                    Версия 1.0.0
                    
                    📝 Умное приложение для ведения заметок
                    ✨ Поддержка Markdown форматирования
                    🔄 Синхронизация с облаком
                    🎨 Удобный и интуитивный интерфейс
                    
                    Особенности:
                    • Создание заметок и задач
                    • Вложенные заметки
                    • Редактор с форматированием
                    • Корзина для удалённых заметок
                    
                    © 2026 Vulpine Team
                """.trimIndent())
                .setPositiveButton("Закрыть", null)
                .show()
        }
    }
}