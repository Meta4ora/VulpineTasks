package com.example.vulpinetasks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import com.example.vulpinetasks.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteAdapter: DemoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupDrawer()

        // Кнопка добавления (просто тост для демо)
        binding.addNoteButton.setOnClickListener {
            android.widget.Toast.makeText(this, "Добавить заметку", android.widget.Toast.LENGTH_SHORT).show()
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

    private fun setupRecyclerView() {
        noteAdapter = DemoAdapter(getDemoNotes())
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = noteAdapter
    }

    private fun getDemoNotes(): List<DemoNote> {
        return listOf(
            DemoNote("Важная заметка", "Содержание заметки с текстом...", "10.04.2026", "120 слов"),
            DemoNote("Идеи для проекта", "Нужно реализовать синхронизацию с облаком...", "09.04.2026", "85 слов"),
            DemoNote("Список задач", "1. Сделать дизайн\n2. Написать код\n3. Задеплоить", "08.04.2026", "45 слов"),
            DemoNote("Вложенная заметка", "Это пример заметки с отступом", "07.04.2026", "30 слов"),
            DemoNote("Ещё одна заметка", "Демонстрация дизайна карточек", "06.04.2026", "67 слов")
        )
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                //R.id.nav_all_notes -> android.widget.Toast.makeText(this, "Все заметки", android.widget.Toast.LENGTH_SHORT).show()
                //R.id.nav_favorites -> android.widget.Toast.makeText(this, "Избранное", android.widget.Toast.LENGTH_SHORT).show()
                //R.id.nav_trash -> android.widget.Toast.makeText(this, "Корзина", android.widget.Toast.LENGTH_SHORT).show()
                //R.id.nav_settings -> android.widget.Toast.makeText(this, "Настройки", android.widget.Toast.LENGTH_SHORT).show()
            }
            binding.drawerLayout.closeDrawers()
            true
        }
    }
}

// Адаптер для демо-заметок
class DemoAdapter(private val notes: List<DemoNote>) :
    RecyclerView.Adapter<DemoAdapter.NoteViewHolder>() {

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
        private val title = itemView.findViewById<TextView>(R.id.note_title)
        private val preview = itemView.findViewById<TextView>(R.id.note_preview)
        private val dateText = itemView.findViewById<TextView>(R.id.date_text)
        private val wordCount = itemView.findViewById<TextView>(R.id.word_count)

        fun bind(note: DemoNote) {
            title.text = note.title
            preview.text = note.preview
            dateText.text = note.date
            wordCount.text = note.wordCount
        }
    }
}

// Простой класс для демо
data class DemoNote(
    val title: String,
    val preview: String,
    val date: String,
    val wordCount: String
)