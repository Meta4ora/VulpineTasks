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

        setupRecycler()
        observeTrash()
    }

    private fun setupRecycler() {
        adapter = TrashAdapter(
            notes = emptyList(),
            onRestore = { note ->
                Log.d(TAG, "RESTORE CLICK id=${note.id}")
                lifecycleScope.launch {
                    repo.restoreFromTrash(note.id)
                    toast("Заметка восстановлена")
                }
            },
            onDelete = { note ->
                Log.d(TAG, "DELETE CLICK id=${note.id}")
                showDeleteDialog(note)
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
                list.forEach { note ->
                    Log.d(TAG, "TRASH ITEM id=${note.id} title=${note.title}")
                }
                adapter.update(list)
                binding.emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showDeleteDialog(note: NoteDto) {
        AlertDialog.Builder(this)
            .setTitle("Удалить заметку?")
            .setMessage("Заметка будет удалена навсегда")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    repo.deletePermanently(note.id)
                    toast("Заметка удалена")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}