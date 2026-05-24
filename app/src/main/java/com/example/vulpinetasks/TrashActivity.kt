package com.example.vulpinetasks

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vulpinetasks.backend.NoteDto
import com.example.vulpinetasks.backend.TokenManager
import com.example.vulpinetasks.databinding.ActivityTrashBinding
import com.example.vulpinetasks.room.AppGraph
import kotlinx.coroutines.launch

class TrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrashBinding
    private lateinit var adapter: TrashAdapter

    private val repo get() = AppGraph.notesRepository
    private lateinit var userId: String
    private lateinit var tokenManager: TokenManager
    private var trashNotes = listOf<NoteDto>()

    companion object {
        private const val TAG = "VULPINE_TRASH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        userId = tokenManager.getUserId() ?: "guest"

        Log.d(TAG, "TrashActivity created userId=$userId")

        setupToolbar()
        setupRecycler()
        observeTrash()
        setupClearAllButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecycler() {
        adapter = TrashAdapter(
            notes = emptyList(),
            onRestore = { note ->
                Log.d(TAG, "RESTORE CLICK id=${note.id}")
                lifecycleScope.launch {
                    showLoading(true)
                    try {
                        repo.restoreFromTrash(note.id)
                        toast("Заметка восстановлена")
                    } catch (e: Exception) {
                        toast("Ошибка при восстановлении")
                        e.printStackTrace()
                    } finally {
                        showLoading(false)
                    }
                }
            },
            onDelete = { note ->
                Log.d(TAG, "DELETE CLICK id=${note.id}")
                showDeleteSingleDialog(note)
            }
        )

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
    }

    private fun observeTrash() {
        Log.d(TAG, "observeTrash() userId=$userId")
        lifecycleScope.launch {
            repo.observeTrash(userId).collect { list ->
                Log.d(TAG, "UI TRASH UPDATE size=${list.size}")
                trashNotes = list
                adapter.update(list)
                updateEmptyView(list.isEmpty())
                updateClearAllButtonVisibility(list.isNotEmpty())
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateClearAllButtonVisibility(hasItems: Boolean) {
        binding.clearAllButton.visibility = if (hasItems) View.VISIBLE else View.GONE
    }

    private fun setupClearAllButton() {
        binding.clearAllButton.setOnClickListener {
            if (trashNotes.isNotEmpty()) {
                showClearAllDialog()
            }
        }
    }

    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Очистить корзину")
            .setMessage("Вы уверены, что хотите навсегда удалить все заметки из корзины?")
            .setPositiveButton("Очистить всё") { _, _ ->
                clearAllTrash()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun clearAllTrash() {
        lifecycleScope.launch {
            showLoading(true)
            try {
                var deletedCount = 0
                for (note in trashNotes) {
                    try {
                        repo.deletePermanently(note.id)
                        deletedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete note: ${note.id}", e)
                    }
                }
                toast("Удалено $deletedCount заметок")

                if (deletedCount > 0) {
                    // Принудительно обновляем список
                    val updatedList = trashNotes.filter { it.id !in trashNotes.map { note -> note.id } }
                    adapter.update(updatedList)
                    updateEmptyView(updatedList.isEmpty())
                }
            } catch (e: Exception) {
                toast("Ошибка при очистке корзины")
                e.printStackTrace()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showDeleteSingleDialog(note: NoteDto) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage("Заметка \"${note.title}\" будет удалена навсегда")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    showLoading(true)
                    try {
                        repo.deletePermanently(note.id)
                        toast("Заметка удалена")
                    } catch (e: Exception) {
                        toast("Ошибка при удалении")
                        e.printStackTrace()
                    } finally {
                        showLoading(false)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.clearAllButton.isEnabled = !show
        binding.recycler.isEnabled = !show
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}