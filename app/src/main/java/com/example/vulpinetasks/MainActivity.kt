package com.example.vulpinetasks

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityMainBinding
import com.example.vulpinetasks.room.AppGraph
import com.example.vulpinetasks.util.NetworkUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotesAdapter
    private lateinit var tokenManager: TokenManager

    private val repo get() = AppGraph.notesRepository
    private lateinit var userId: String

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
                adapter.submitList(notes)
            }
        }
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_login -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                R.id.nav_trash -> {
                    startActivity(Intent(this, TrashActivity::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /**
     * Диалог выбора типа создаваемой заметки
     */
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

    /**
     * Диалог создания заметки/задачи с вводом заголовка
     */
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
                        // Создаем заметку/задачу с соответствующим типом
                        val defaultContent = if (type == "note") {
                            "# $text\n\n"
                        } else {
                            "[]"  // Пустой JSON массив для подзадач
                        }

                        repo.createNote(
                            title = text,
                            type = type,
                            userId = userId,
                            isOnline = NetworkUtil.isOnline(this@MainActivity)
                        )

                        // Ждем небольшую задержку для завершения операции
                        kotlinx.coroutines.delay(100)

                        // Находим созданную заметку по заголовку и обновляем содержимое
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

    /**
     * Найти заметку по заголовку
     */
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