package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityNoteEditorBinding
import com.example.vulpinetasks.mappers.toDto
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var tokenManager: TokenManager
    private var noteId: String? = null
    private var userId: String? = null
    private var originalContent: String = ""
    private var noteTitle: String = ""
    private var childNotesAdapter: ChildNotesAdapter? = null

    companion object {
        private const val TAG = "NOTE_EDITOR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        noteId = intent.getStringExtra("note_id")
        noteTitle = intent.getStringExtra("note_title") ?: "Заметка"
        userId = tokenManager.getUserId()

        Log.d(TAG, "=== onCreate ===")
        Log.d(TAG, "noteId: $noteId")
        Log.d(TAG, "noteTitle: $noteTitle")

        setupToolbar()
        loadContent()
        setupChildNotesSection()
        loadChildNotes()
        setupSaveButton()
        setupAddChildNoteButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = noteTitle
        binding.toolbar.setNavigationOnClickListener {
            saveAndClose()
        }
    }

    private fun setupChildNotesSection() {
        childNotesAdapter = ChildNotesAdapter(
            onNoteClick = { childNote ->
                Log.d(TAG, "Child note clicked: ${childNote.title}")
                val intent = Intent(this, NoteEditorActivity::class.java)
                intent.putExtra("note_id", childNote.id)
                intent.putExtra("note_title", childNote.title)
                startActivity(intent)
            },
            onUnlinkClick = { childNote ->
                lifecycleScope.launch {
                    if (noteId != null) {
                        Log.d(TAG, "Unlinking child note: ${childNote.id} from parent: $noteId")
                        AppGraph.notesRepository.removeParentRelation(childNote.id, noteId!!)
                        loadChildNotes()
                        Toast.makeText(
                            this@NoteEditorActivity,
                            "Заметка отвязана",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        binding.childNotesRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@NoteEditorActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = childNotesAdapter
            // Устанавливаем пустой список, если адаптер еще не получил данные
            childNotesAdapter?.submitList(emptyList())
        }
    }

    private fun loadChildNotes() {
        Log.d(TAG, "=== loadChildNotes ===")
        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                Log.d(TAG, "Loading child notes for parentId: $noteId")

                val childNotes = AppGraph.notesRepository.getChildNotes(noteId!!, userId!!)
                Log.d(TAG, "Child notes found: ${childNotes.size}")
                childNotes.forEach { note ->
                    Log.d(TAG, "  Child: ${note.title} (${note.id})")
                }

                childNotesAdapter?.submitList(childNotes)

                // ВСЕГДА показываем секцию, даже если нет заметок
                // Изменяем заголовок в зависимости от наличия заметок
                binding.tagsSection.visibility = View.VISIBLE

                if (childNotes.isEmpty()) {
                    Log.d(TAG, "No child notes, showing empty section")
                    binding.tagsTitle.text = "Вложенные заметки (0)"

                    // Опционально: показать подсказку, что нет вложенных заметок
                    // Можно добавить временный элемент "Нет вложенных заметок"
                    // Для этого нужно модифицировать адаптер
                    showEmptyState()
                } else {
                    Log.d(TAG, "Showing child notes section (${childNotes.size} notes)")
                    binding.tagsTitle.text = "Вложенные заметки (${childNotes.size})"
                }
            }
        }
    }

    private fun showEmptyState() {
        // Создаем пустой список (уже сделано через adapter.submitList(emptyList()))
        // Можно добавить плейсхолдер, если нужно
        if (childNotesAdapter?.currentList.isNullOrEmpty()) {
            // Показываем сообщение, что нет вложенных заметок
            // Для этого нужно добавить TextView в layout или использовать другой подход
        }
    }

    private fun setupAddChildNoteButton() {
        binding.addTagButton.setOnClickListener {
            Log.d(TAG, "Add child note button clicked")
            showAddChildNoteDialog()
        }
    }

    private fun showAddChildNoteDialog() {
        Log.d(TAG, "=== showAddChildNoteDialog ===")
        lifecycleScope.launch {
            val currentNoteId = noteId ?: run {
                Log.e(TAG, "noteId is null")
                return@launch
            }
            val currentUserId = userId ?: run {
                Log.e(TAG, "userId is null")
                return@launch
            }

            val allNotes = AppGraph.notesRepository.getAllNotes(currentUserId)
            Log.d(TAG, "All notes count: ${allNotes.size}")

            val existingChildIds = AppGraph.notesRepository.getChildNotesIds(currentNoteId)
            Log.d(TAG, "Existing child IDs: $existingChildIds")

            // Фильтруем заметки, которые можно добавить:
            // 1. Не текущая заметка
            // 2. Не уже дочерняя
            val availableNotes = allNotes.filter { note ->
                note.id != currentNoteId && note.id !in existingChildIds
            }

            Log.d(TAG, "Available notes to add as children: ${availableNotes.size}")

            if (availableNotes.isEmpty()) {
                Toast.makeText(
                    this@NoteEditorActivity,
                    "Нет доступных заметок для добавления. Сначала создайте другие заметки.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val titles = availableNotes.map { it.title }.toTypedArray()

            AlertDialog.Builder(this@NoteEditorActivity)
                .setTitle("Выберите заметку")
                .setItems(titles) { _, which ->
                    val selectedNote = availableNotes[which]
                    lifecycleScope.launch {
                        Log.d(TAG, "Adding child note: ${selectedNote.id} to parent: $currentNoteId")
                        AppGraph.notesRepository.addParentRelation(selectedNote.id, currentNoteId)
                        loadChildNotes()
                        Toast.makeText(
                            this@NoteEditorActivity,
                            "Заметка добавлена: ${selectedNote.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun loadContent() {
        Log.d(TAG, "=== loadContent ===")
        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                showLoading(true)
                try {
                    Log.d(TAG, "Loading content for noteId: $noteId")
                    val content = AppGraph.notesRepository.getNoteContent(noteId!!, userId!!)
                    originalContent = content
                    binding.noteContent.setText(content)
                    Log.d(TAG, "Content loaded, length: ${content.length}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading content", e)
                    Toast.makeText(this@NoteEditorActivity, "Ошибка загрузки содержимого", Toast.LENGTH_SHORT).show()
                    binding.noteContent.setText("# $noteTitle\n\nОшибка загрузки содержимого заметки")
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.fabSave.setOnClickListener {
            saveContent()
        }
    }

    private fun saveContent() {
        val newContent = binding.noteContent.text.toString()
        Log.d(TAG, "saveContent: original length=${originalContent.length}, new length=${newContent.length}")

        if (newContent == originalContent) {
            finish()
            return
        }

        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                showLoading(true)
                try {
                    Log.d(TAG, "Updating note content")
                    AppGraph.notesRepository.updateNoteContent(noteId!!, userId!!, newContent)
                    originalContent = newContent
                    Toast.makeText(this@NoteEditorActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving content", e)
                    Toast.makeText(this@NoteEditorActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun saveAndClose() {
        val newContent = binding.noteContent.text.toString()
        if (newContent != originalContent) {
            saveContent()
        } else {
            finish()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.noteContent.isEnabled = !show
        binding.fabSave.isEnabled = !show
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveAndClose()
    }
}