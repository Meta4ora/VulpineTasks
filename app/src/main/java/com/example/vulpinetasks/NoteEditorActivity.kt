package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.SubTaskDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityNoteEditorBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var tokenManager: TokenManager
    private var noteId: String? = null
    private var userId: String? = null
    private var originalContent: String = ""
    private var noteTitle: String = ""
    private var noteType: String = "note"
    private var childNotesAdapter: ChildNotesAdapter? = null
    private var subtasksAdapter: SubtasksAdapter? = null
    private var subtasks = mutableListOf<SubTaskDto>()
    private var isSaving = false
    private var saveJob: Job? = null

    companion object {
        private const val TAG = "NOTE_EDITOR"
        private const val SAVE_DELAY_MS = 1000L
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

        loadNoteType()

        setupToolbar()
        loadContent()
        setupChildNotesSection()
        loadChildNotes()
        setupSaveButton()
        setupAddChildNoteButton()
    }

    private fun loadNoteType() {
        lifecycleScope.launch {
            val note = AppGraph.notesRepository.getNoteByIdRaw(noteId ?: return@launch)
            noteType = note?.type ?: "note"

            Log.d(TAG, "Note type: $noteType")

            if (noteType == "task") {
                showTaskEditor()
            } else {
                showNoteEditor()
            }
        }
    }

    private fun showNoteEditor() {
        binding.noteEditorContainer.visibility = View.VISIBLE
        binding.taskEditorContainer.visibility = View.GONE
        setupFormatButtons()
    }

    private fun showTaskEditor() {
        binding.noteEditorContainer.visibility = View.GONE
        binding.taskEditorContainer.visibility = View.VISIBLE
        setupSubtasksRecyclerView()
        setupAddSubtaskButton()
    }

    private fun setupFormatButtons() {
        binding.btnBold.setOnClickListener { applyStyle(StyleSpan(android.graphics.Typeface.BOLD)) }
        binding.btnItalic.setOnClickListener { applyStyle(StyleSpan(android.graphics.Typeface.ITALIC)) }
        binding.btnUnderline.setOnClickListener { applyStyle(UnderlineSpan()) }
        binding.btnBulletList.setOnClickListener { insertBullet() }
        binding.btnNumberList.setOnClickListener { insertNumber() }
        binding.btnHeading.setOnClickListener { applyHeading() }
    }

    private fun applyStyle(span: Any) {
        val text = binding.noteContent.text
        val start = binding.noteContent.selectionStart
        val end = binding.noteContent.selectionEnd

        if (start >= 0 && end > start && start < end) {
            val spannable = text as Spannable
            spannable.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun insertBullet() {
        val position = binding.noteContent.selectionStart
        val text = binding.noteContent.text
        text?.insert(position, "• ")
    }

    private fun insertNumber() {
        val position = binding.noteContent.selectionStart
        val text = binding.noteContent.text
        text?.insert(position, "1. ")
    }

    private fun applyHeading() {
        val text = binding.noteContent.text
        val start = binding.noteContent.selectionStart
        val end = binding.noteContent.selectionEnd

        if (start >= 0 && end > start && start < end) {
            val spannable = text as Spannable
            spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun setupSubtasksRecyclerView() {
        subtasksAdapter = SubtasksAdapter(
            subtasks = subtasks,
            onToggleComplete = { position, isChecked ->
                subtasks[position] = subtasks[position].copy(isCompleted = isChecked)
                subtasksAdapter?.updateList(subtasks)
                scheduleAutoSave()
            },
            onDeleteSubtask = { position ->
                subtasks.removeAt(position)
                subtasksAdapter?.updateList(subtasks)
                scheduleAutoSave()
            }
        )

        binding.subtasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.subtasksRecyclerView.adapter = subtasksAdapter
    }

    private fun setupAddSubtaskButton() {
        binding.btnAddSubtask.setOnClickListener {
            showAddSubtaskDialog()
        }
    }

    private fun showAddSubtaskDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Название подзадачи"

        AlertDialog.Builder(this)
            .setTitle("Добавить подзадачу")
            .setView(input)
            .setPositiveButton("Добавить") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotEmpty()) {
                    val newSubtask = SubTaskDto(
                        id = System.currentTimeMillis().toString(),
                        title = title,
                        isCompleted = false
                    )
                    subtasks.add(newSubtask)
                    subtasksAdapter?.updateList(subtasks)
                    scheduleAutoSave()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
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

                childNotesAdapter?.submitList(childNotes)

                binding.tagsSection.visibility = View.VISIBLE

                if (childNotes.isEmpty()) {
                    Log.d(TAG, "No child notes, showing empty section")
                    binding.tagsTitle.text = "Вложенные заметки (0)"
                } else {
                    Log.d(TAG, "Showing child notes section (${childNotes.size} notes)")
                    binding.tagsTitle.text = "Вложенные заметки (${childNotes.size})"
                }
            }
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
            val existingChildIds = AppGraph.notesRepository.getChildNotesIds(currentNoteId)

            val availableNotes = allNotes.filter { note ->
                note.id != currentNoteId && note.id !in existingChildIds
            }

            if (availableNotes.isEmpty()) {
                Toast.makeText(
                    this@NoteEditorActivity,
                    "Нет доступных заметок для добавления",
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
                    val content = AppGraph.notesRepository.getNoteContent(noteId!!, userId!!)
                    originalContent = content

                    Log.d(TAG, "Loaded content length: ${content.length}")

                    if (noteType == "task") {
                        parseSubtasksFromJson(content)
                        subtasksAdapter?.updateList(subtasks)
                        Log.d(TAG, "Parsed ${subtasks.size} subtasks")
                    } else {
                        // Конвертируем HTML в Spanned для отображения форматирования
                        val spanned = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        binding.noteContent.setText(spanned)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading content", e)
                    Toast.makeText(this@NoteEditorActivity, "Ошибка загрузки содержимого", Toast.LENGTH_SHORT).show()
                    if (noteType == "task") {
                        subtasks.clear()
                        subtasksAdapter?.updateList(subtasks)
                    } else {
                        binding.noteContent.setText("# $noteTitle\n\n")
                    }
                } finally {
                    showLoading(false)
                }
            }
        }
    }

    private fun parseSubtasksFromJson(json: String) {
        try {
            val jsonArray = JSONArray(json)
            subtasks.clear()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                subtasks.add(
                    SubTaskDto(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        isCompleted = obj.getBoolean("isCompleted")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subtasks JSON", e)
            subtasks.clear()
        }
    }

    private fun saveSubtasksToJson(): String {
        val jsonArray = JSONArray()
        subtasks.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("isCompleted", it.isCompleted)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    /**
     * Конвертирует форматированный текст в HTML для сохранения
     */
    private fun getFormattedTextAsHtml(): String {
        val text = binding.noteContent.text
        if (text is Spanned) {
            return Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        }
        return text.toString()
    }

    private fun scheduleAutoSave() {
        if (noteType != "task") return

        saveJob?.cancel()
        saveJob = lifecycleScope.launch {
            delay(SAVE_DELAY_MS)
            autoSaveContent()
        }
    }

    private suspend fun autoSaveContent() {
        if (isSaving) return

        val newContent = if (noteType == "task") {
            saveSubtasksToJson()
        } else {
            getFormattedTextAsHtml()
        }

        if (newContent == originalContent) {
            Log.d(TAG, "Auto-save: content unchanged")
            return
        }

        Log.d(TAG, "Auto-saving...")

        isSaving = true
        try {
            if (noteId != null && userId != null) {
                AppGraph.notesRepository.updateNoteContent(noteId!!, userId!!, newContent)
                originalContent = newContent
                Log.d(TAG, "Auto-save completed successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-save failed", e)
        } finally {
            isSaving = false
        }
    }

    private fun setupSaveButton() {
        binding.fabSave.setOnClickListener {
            saveContent()
        }
    }

    private fun saveContent() {
        val newContent = if (noteType == "task") {
            saveSubtasksToJson()
        } else {
            getFormattedTextAsHtml()
        }

        Log.d(TAG, "saveContent: original length=${originalContent.length}, new length=${newContent.length}")

        if (newContent == originalContent) {
            finish()
            return
        }

        saveJob?.cancel()

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
        saveJob?.cancel()
        saveContent()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabSave.isEnabled = !show

        if (noteType == "task") {
            binding.btnAddSubtask.isEnabled = !show
        } else {
            binding.noteContent.isEnabled = !show
        }
    }

    override fun onPause() {
        super.onPause()
        if (noteType == "task") {
            saveJob?.cancel()
            lifecycleScope.launch {
                autoSaveContent()
            }
        } else {
            // Для заметок тоже сохраняем при паузе
            saveJob?.cancel()
            lifecycleScope.launch {
                if (!isSaving && noteId != null && userId != null) {
                    val newContent = getFormattedTextAsHtml()
                    if (newContent != originalContent) {
                        AppGraph.notesRepository.updateNoteContent(noteId!!, userId!!, newContent)
                        originalContent = newContent
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveAndClose()
    }
}