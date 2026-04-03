package com.example.vulpinetasks

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.navigation.NavigationView
import com.example.vulpinetasks.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: DemoAdapter
    private val notesList = mutableListOf<DemoNote>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Загружаем начальные заметки
        notesList.addAll(getDemoNotes())

        setupRecyclerView()
        setupDrawer()

        // Кнопка добавления с выбором типа
        binding.addNoteButton.setOnClickListener {
            showCreateNoteDialog()
        }

        // Кнопка меню в шапке
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Очистка поиска
        binding.clearButton.setOnClickListener {
            binding.searchEditText.text.clear()
        }
    }

    private fun showCreateNoteDialog() {
        val options = arrayOf(
            "📝 Обычная заметка",
            "✅ Список задач",
            "💡 Идея",
            "📅 Напоминание",
            "🔗 Ссылка"
        )

        AlertDialog.Builder(this)
            .setTitle("Создать новую запись")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> createRegularNote()
                    1 -> createTaskList()
                    2 -> createIdea()
                    3 -> createReminder()
                    4 -> createLink()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createRegularNote() {
        val title = "Новая заметка ${notesList.size + 1}"
        val content = "Текст вашей заметки..."
        val newNote = DemoNote(
            title = title,
            preview = content,
            date = getCurrentDate(),
            wordCount = "${content.length / 5} слов",
            type = "📝"
        )
        notesList.add(0, newNote)
        noteAdapter.updateNotes(notesList)
        Toast.makeText(this, "Создана новая заметка", Toast.LENGTH_SHORT).show()
    }

    private fun createTaskList() {
        val title = "Новый список задач ${notesList.size + 1}"
        val content = "□ Задача 1\n□ Задача 2\n□ Задача 3"
        val newNote = DemoNote(
            title = title,
            preview = "□ Задача 1\n□ Задача 2\n□ Задача 3",
            date = getCurrentDate(),
            wordCount = "3 задачи",
            type = "✅"
        )
        notesList.add(0, newNote)
        noteAdapter.updateNotes(notesList)
        Toast.makeText(this, "Создан новый список задач", Toast.LENGTH_SHORT).show()
    }

    private fun createIdea() {
        val title = "Новая идея ${notesList.size + 1}"
        val content = "Запишите вашу идею здесь..."
        val newNote = DemoNote(
            title = title,
            preview = content,
            date = getCurrentDate(),
            wordCount = "${content.length / 5} слов",
            type = "💡"
        )
        notesList.add(0, newNote)
        noteAdapter.updateNotes(notesList)
        Toast.makeText(this, "Добавлена новая идея", Toast.LENGTH_SHORT).show()
    }

    private fun createReminder() {
        val title = "Напоминание ${notesList.size + 1}"
        val content = "Установите дату и время напоминания"
        val newNote = DemoNote(
            title = title,
            preview = content,
            date = getCurrentDate(),
            wordCount = "Напоминание",
            type = "📅"
        )
        notesList.add(0, newNote)
        noteAdapter.updateNotes(notesList)
        Toast.makeText(this, "Создано новое напоминание", Toast.LENGTH_SHORT).show()
    }

    private fun createLink() {
        val title = "Ссылка ${notesList.size + 1}"
        val content = "https://example.com"
        val newNote = DemoNote(
            title = title,
            preview = content,
            date = getCurrentDate(),
            wordCount = "Ссылка",
            type = "🔗"
        )
        notesList.add(0, newNote)
        noteAdapter.updateNotes(notesList)
        Toast.makeText(this, "Добавлена новая ссылка", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }

    private fun setupRecyclerView() {
        noteAdapter = DemoAdapter(notesList)
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = noteAdapter
    }

    private fun getDemoNotes(): List<DemoNote> {
        return listOf(
            DemoNote("Важная заметка", "Содержание заметки с текстом...", "10.04.2026", "120 слов", "📝"),
            DemoNote("Идеи для проекта", "Нужно реализовать синхронизацию с облаком...", "09.04.2026", "85 слов", "💡"),
            DemoNote("Список задач", "1. Сделать дизайн\n2. Написать код\n3. Задеплоить", "08.04.2026", "3 задачи", "✅"),
            DemoNote("Вложенная заметка", "Это пример заметки с отступом", "07.04.2026", "30 слов", "📝"),
            DemoNote("Полезная ссылка", "https://github.com/", "06.04.2026", "Ссылка", "🔗")
        )
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_all_notes -> {
                    Toast.makeText(this, "Все заметки", Toast.LENGTH_SHORT).show()
                    noteAdapter.updateNotes(notesList)
                }
                R.id.nav_favorites -> Toast.makeText(this, "Избранное", Toast.LENGTH_SHORT).show()
                R.id.nav_trash -> Toast.makeText(this, "Корзина", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show()
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }
}

// Адаптер для заметок
class DemoAdapter(private var notes: List<DemoNote>) :
    RecyclerView.Adapter<DemoAdapter.NoteViewHolder>() {

    fun updateNotes(newNotes: List<DemoNote>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount() = notes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeIcon = itemView.findViewById<TextView>(R.id.type_icon)
        private val title = itemView.findViewById<TextView>(R.id.note_title)
        private val preview = itemView.findViewById<TextView>(R.id.note_preview)
        private val dateText = itemView.findViewById<TextView>(R.id.date_text)
        private val wordCount = itemView.findViewById<TextView>(R.id.word_count)

        fun bind(note: DemoNote) {
            typeIcon.text = note.type
            title.text = note.title
            preview.text = note.preview
            dateText.text = note.date
            wordCount.text = note.wordCount
        }
    }
}

// Обновлённый класс заметки с типом
data class DemoNote(
    val title: String,
    val preview: String,
    val date: String,
    val wordCount: String,
    val type: String = "📝"  // Тип заметки (эмодзи)
)