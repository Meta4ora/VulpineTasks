package com.example.vulpinetasks

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityMainBinding
import com.example.vulpinetasks.room.AppGraph
import com.example.vulpinetasks.util.NetworkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotesAdapter
    private lateinit var tokenManager: TokenManager

    private val repo get() = AppGraph.notesRepository
    private lateinit var userId: String

    // Состояния для фильтрации и поиска
    private val searchQuery = MutableStateFlow("")
    private val sortType = MutableStateFlow(SortType.DATE_DESC)
    private val allNotes = mutableListOf<NoteDto>()

    enum class SortType {
        DATE_DESC,      // Сначала новые
        DATE_ASC,       // Сначала старые
        ALPHABETICAL_ASC,   // А-Я
        ALPHABETICAL_DESC   // Я-А
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        userId = tokenManager.getUserId() ?: "guest_${System.currentTimeMillis()}"
        tokenManager.saveUserId(userId)

        setupRecycler()
        observeNotes()
        setupDrawer()
        setupSearchAndFilter()

        if (!tokenManager.isGuest()) {
            lifecycleScope.launch {
                repo.clearDeletedRelations()
                repo.fetchFromServer(userId)
            }
        }

        binding.addNoteButton.setOnClickListener {
            showTypeSelectionDialog()
        }

        binding.syncButton.setOnClickListener {
            lifecycleScope.launch {
                if (tokenManager.isGuest()) {
                    toast("Синхронизация недоступна в гостевом режиме")
                } else {
                    repo.syncUnsyncedNotes(userId)
                    repo.fetchFromServer(userId)
                    toast("Синхронизация завершена")
                }
            }
        }
    }

    private fun setupRecycler() {
        adapter = NotesAdapter(
            onOpen = { note ->
                val intent = Intent(this, NoteEditorActivity::class.java)
                intent.putExtra("note_id", note.id)
                intent.putExtra("note_title", note.title)
                startActivity(intent)
            },
            onTrash = { note ->
                lifecycleScope.launch {
                    repo.moveToTrash(note.id)
                    toast("Перемещено в корзину")
                }
            },
            onInfo = { note ->
                showNoteInfoDialog(note)
            },
            onRename = { note ->
                showRenameDialog(note)
            }
        )

        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = adapter
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repo.observeNotes(userId).collect { notes ->
                allNotes.clear()
                allNotes.addAll(notes)
                applyFiltersAndSort()
            }
        }
    }

    private fun applyFiltersAndSort() {
        lifecycleScope.launch {
            combine(searchQuery, sortType) { query, sort ->
                filterAndSortNotes(query, sort)
            }.collect { filteredNotes ->
                adapter.submitList(filteredNotes)
                updateEmptyView(filteredNotes.isEmpty())
            }
        }
    }

    private fun filterAndSortNotes(query: String, sort: SortType): List<NoteDto> {
        var filtered = allNotes.toList()

        // Фильтрация по поисковому запросу
        if (query.isNotEmpty()) {
            filtered = filtered.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }
        }

        // Сортировка
        filtered = when (sort) {
            SortType.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
            SortType.DATE_ASC -> filtered.sortedBy { it.updatedAt }
            SortType.ALPHABETICAL_ASC -> filtered.sortedBy { it.title.lowercase() }
            SortType.ALPHABETICAL_DESC -> filtered.sortedByDescending { it.title.lowercase() }
        }

        return filtered
    }

    private fun setupSearchAndFilter() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Настройка SearchView
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Поиск по заголовку или содержанию"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchQuery.value = query
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                searchQuery.value = newText
                return true
            }
        })

        // Обработчик кнопки "Очистить поиск"
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchQuery.value = ""
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val sortOptions = arrayOf(
            "📅 Сначала новые",
            "📅 Сначала старые",
            "🔤 По алфавиту (А-Я)",
            "🔤 По алфавиту (Я-А)"
        )

        val currentSelection = when (sortType.value) {
            SortType.DATE_DESC -> 0
            SortType.DATE_ASC -> 1
            SortType.ALPHABETICAL_ASC -> 2
            SortType.ALPHABETICAL_DESC -> 3
        }

        AlertDialog.Builder(this)
            .setTitle("Сортировка заметок")
            .setSingleChoiceItems(sortOptions, currentSelection) { _, which ->
                sortType.value = when (which) {
                    0 -> SortType.DATE_DESC
                    1 -> SortType.DATE_ASC
                    2 -> SortType.ALPHABETICAL_ASC
                    3 -> SortType.ALPHABETICAL_DESC
                    else -> SortType.DATE_DESC
                }
            }
            .setPositiveButton("Применить") { dialog, _ ->
                dialog.dismiss()
                toast("Сортировка применена")
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = android.view.View.VISIBLE
            binding.emptyText.text = if (searchQuery.value.isNotEmpty()) {
                "Ничего не найдено"
            } else {
                "Нет заметок"
            }
        } else {
            binding.emptyView.visibility = android.view.View.GONE
        }
    }

    private fun setupDrawer() {
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_login -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                R.id.nav_trash -> {
                    startActivity(Intent(this, TrashActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showTypeSelectionDialog() {
        val options = arrayOf("📝 Заметка", "✅ Задача")

        AlertDialog.Builder(this)
            .setTitle("Создать")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateNoteDialog("note")
                    1 -> showCreateNoteDialog("task")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCreateNoteDialog(type: String) {
        val input = EditText(this)
        input.hint = if (type == "note") "Введите заголовок заметки" else "Введите название задачи"

        AlertDialog.Builder(this)
            .setTitle(if (type == "note") "Новая заметка" else "Новая задача")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isEmpty()) {
                    toast("Заголовок не может быть пустым")
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val defaultContent = if (type == "note") {
                            "# $text\n\n"
                        } else {
                            "[]"
                        }

                        repo.createNote(
                            title = text,
                            type = type,
                            userId = userId,
                            isOnline = NetworkUtil.isOnline(this@MainActivity)
                        )

                        kotlinx.coroutines.delay(100)

                        val createdNote = getNoteByTitle(text)
                        if (createdNote != null) {
                            repo.updateNoteContent(createdNote.id, userId, defaultContent)
                            toast(if (type == "note") "Заметка создана" else "Задача создана")
                        } else {
                            toast(if (type == "note") "Заметка создана" else "Задача создана")
                        }
                    } catch (e: Exception) {
                        toast("Ошибка при создании")
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private suspend fun getNoteByTitle(title: String): NoteDto? {
        val notes = repo.getAllNotes(userId)
        return notes.find { it.title == title }
    }

    private fun showRenameDialog(note: NoteDto) {
        val input = EditText(this)
        input.hint = "Введите новый заголовок"
        input.setText(note.title)
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle("Переименовать заметку")
            .setMessage("Введите новое название")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val newTitle = input.text.toString().trim()
                if (newTitle.isEmpty()) {
                    toast("Название не может быть пустым")
                    return@setPositiveButton
                }

                if (newTitle == note.title) {
                    toast("Название не изменено")
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        repo.updateNoteTitle(note.id, newTitle)
                        toast("Название изменено на: $newTitle")
                    } catch (e: Exception) {
                        toast("Ошибка при переименовании")
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNoteInfoDialog(note: NoteDto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note_info, null)

        val titleText = dialogView.findViewById<TextView>(R.id.info_title)
        val createdAtText = dialogView.findViewById<TextView>(R.id.info_created_at)
        val updatedAtText = dialogView.findViewById<TextView>(R.id.info_updated_at)
        val childCountText = dialogView.findViewById<TextView>(R.id.info_child_count)
        val parentCountText = dialogView.findViewById<TextView>(R.id.info_parent_count)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

        titleText.text = note.title
        createdAtText.text = dateFormat.format(Date(note.createdAt))
        updatedAtText.text = dateFormat.format(Date(note.updatedAt))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Информация о заметке")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .create()

        dialog.show()

        lifecycleScope.launch {
            try {
                val childCount = repo.getChildNotesIds(note.id).size
                val parentCount = repo.getParentIdsForNoteInfo(note.id).size

                childCountText.text = childCount.toString()
                parentCountText.text = parentCount.toString()
            } catch (e: Exception) {
                childCountText.text = "0"
                parentCountText.text = "0"
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}