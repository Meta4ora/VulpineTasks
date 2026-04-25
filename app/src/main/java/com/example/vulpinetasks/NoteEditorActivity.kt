package com.example.vulpinetasks

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityNoteEditorBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditorBinding
    private lateinit var tokenManager: TokenManager
    private var noteId: String? = null
    private var userId: String? = null
    private var originalContent: String = ""
    private var noteTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        noteId = intent.getStringExtra("note_id")
        noteTitle = intent.getStringExtra("note_title") ?: "Заметка"
        userId = tokenManager.getUserId()

        setupToolbar()
        loadContent()
        setupSaveButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = noteTitle
        binding.toolbar.setNavigationOnClickListener {
            saveAndClose()
        }
    }

    private fun loadContent() {
        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                showLoading(true)
                try {
                    val content = AppGraph.notesRepository.getNoteContent(noteId!!, userId!!)
                    originalContent = content
                    binding.noteContent.setText(content)
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast("Ошибка загрузки содержимого")
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

        if (newContent == originalContent) {
            finish()
            return
        }

        lifecycleScope.launch {
            if (noteId != null && userId != null) {
                showLoading(true)
                try {
                    AppGraph.notesRepository.updateNoteContent(noteId!!, userId!!, newContent)
                    originalContent = newContent
                    toast("Сохранено")
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast("Ошибка сохранения")
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
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.noteContent.isEnabled = !show
        binding.fabSave.isEnabled = !show
    }

    override fun onPause() {
        super.onPause()
        saveContent()
    }
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveAndClose()
    }
}